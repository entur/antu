package no.entur.antu.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import no.entur.antu.config.EmbeddedRedisTestBase;
import no.entur.antu.pipeline.ValidationBarrier.Stage;
import org.junit.jupiter.api.Test;

/**
 * The barrier is the one place where two pods racing would either run a stage twice or never run it.
 */
class ValidationBarrierTest extends EmbeddedRedisTestBase {

  private static ValidationBarrier barrier() {
    return new ValidationBarrier(redissonClient);
  }

  private static String newReportId() {
    return "report-" + UUID.randomUUID();
  }

  /**
   * Records what the barrier handed over, and how many times.
   */
  private static final class Opened {

    private final List<List<String>> handovers = new ArrayList<>();

    void accept(List<String> fileNames) {
      handovers.add(fileNames);
    }

    int count() {
      return handovers.size();
    }

    List<String> only() {
      assertEquals(1, handovers.size(), "expected exactly one hand-off");
      return handovers.getFirst();
    }
  }

  @Test
  void theBarrierStaysClosedUntilEveryFileHasArrived() {
    ValidationBarrier barrier = barrier();
    String reportId = newReportId();
    Opened opened = new Opened();

    barrier.arrive(Stage.REPORTS_WRITTEN, reportId, "a.xml", 3, opened::accept);
    barrier.arrive(Stage.REPORTS_WRITTEN, reportId, "b.xml", 3, opened::accept);
    assertEquals(0, opened.count());

    barrier.arrive(Stage.REPORTS_WRITTEN, reportId, "c.xml", 3, opened::accept);

    assertEquals(List.of("a.xml", "b.xml", "c.xml"), opened.only());
  }

  /**
   * PubSub redelivers, so the same file arrives more than once. Counting it twice would open the barrier
   * before the other files are done.
   */
  @Test
  void aFileArrivingTwiceIsCountedOnce() {
    ValidationBarrier barrier = barrier();
    String reportId = newReportId();
    Opened opened = new Opened();

    barrier.arrive(Stage.REPORTS_WRITTEN, reportId, "a.xml", 2, opened::accept);
    barrier.arrive(Stage.REPORTS_WRITTEN, reportId, "a.xml", 2, opened::accept);
    assertEquals(0, opened.count());

    barrier.arrive(Stage.REPORTS_WRITTEN, reportId, "b.xml", 2, opened::accept);
    assertEquals(1, opened.count());
  }

  @Test
  void theBarrierOpensOnlyOnceEvenWhenTheLastFileIsRedelivered() {
    ValidationBarrier barrier = barrier();
    String reportId = newReportId();
    Opened opened = new Opened();

    barrier.arrive(Stage.REPORTS_WRITTEN, reportId, "a.xml", 1, opened::accept);
    barrier.arrive(Stage.REPORTS_WRITTEN, reportId, "a.xml", 1, opened::accept);

    assertEquals(
      1,
      opened.count(),
      "a second delivery must not start the next stage again"
    );
  }

  /**
   * The hand-off publishes the next job. If it throws, the message is nacked and redelivered, and the
   * redelivery has to be able to pass the barrier again or the dataset waits forever with nothing left to
   * release it.
   */
  @Test
  void aFailedHandOffReleasesTheBarrierForTheRedelivery() {
    ValidationBarrier barrier = barrier();
    String reportId = newReportId();
    AtomicInteger attempts = new AtomicInteger();

    assertThrows(
      IllegalStateException.class,
      () ->
        barrier.arrive(
          Stage.REPORTS_WRITTEN,
          reportId,
          "a.xml",
          1,
          fileNames -> {
            attempts.incrementAndGet();
            throw new IllegalStateException("publish failed");
          }
        )
    );

    Opened opened = new Opened();
    barrier.arrive(Stage.REPORTS_WRITTEN, reportId, "a.xml", 1, opened::accept);

    assertEquals(1, attempts.get());
    assertEquals(List.of("a.xml"), opened.only());
  }

  @Test
  void theTwoStagesAreCountedSeparately() {
    ValidationBarrier barrier = barrier();
    String reportId = newReportId();
    Opened commonFiles = new Opened();
    Opened reports = new Opened();

    barrier.arrive(
      Stage.COMMON_FILES_VALIDATED,
      reportId,
      "_common.xml",
      1,
      commonFiles::accept
    );
    barrier.arrive(
      Stage.REPORTS_WRITTEN,
      reportId,
      "_common.xml",
      2,
      reports::accept
    );

    assertEquals(1, commonFiles.count());
    assertEquals(
      0,
      reports.count(),
      "the report aggregation still waits for the line files"
    );
  }

  @Test
  void validationsDoNotShareABarrier() {
    ValidationBarrier barrier = barrier();
    Opened opened = new Opened();

    barrier.arrive(
      Stage.REPORTS_WRITTEN,
      newReportId(),
      "a.xml",
      2,
      opened::accept
    );
    barrier.arrive(
      Stage.REPORTS_WRITTEN,
      newReportId(),
      "b.xml",
      2,
      opened::accept
    );

    assertEquals(0, opened.count());
  }

  /**
   * Many pods finishing their file at the same instant, which is the case a heap-based aggregator could
   * not handle without a single leader.
   */
  @Test
  void concurrentArrivalsOpenTheBarrierExactlyOnce() throws Exception {
    String reportId = newReportId();
    int fileCount = 16;
    AtomicInteger handovers = new AtomicInteger();

    try (ExecutorService executor = Executors.newFixedThreadPool(8)) {
      List<Callable<Void>> arrivals = IntStream
        .range(0, fileCount)
        .mapToObj(i ->
          (Callable<Void>) () -> {
            barrier()
              .arrive(
                Stage.REPORTS_WRITTEN,
                reportId,
                "file-" + i + ".xml",
                fileCount,
                fileNames -> handovers.incrementAndGet()
              );
            return null;
          }
        )
        .toList();

      for (Future<Void> result : executor.invokeAll(arrivals)) {
        result.get();
      }
    }

    assertEquals(1, handovers.get());
  }

  @Test
  void cleanUpLetsAReportIdBeReused() {
    ValidationBarrier barrier = barrier();
    String reportId = newReportId();
    Opened opened = new Opened();

    barrier.arrive(Stage.REPORTS_WRITTEN, reportId, "a.xml", 1, opened::accept);
    barrier.cleanUp(reportId);
    barrier.arrive(Stage.REPORTS_WRITTEN, reportId, "a.xml", 1, opened::accept);

    assertEquals(2, opened.count());
  }

  @Test
  void arrivalsExpireSoAnAbandonedValidationDoesNotLeak() {
    ValidationBarrier barrier = barrier();
    String reportId = newReportId();

    barrier.arrive(
      Stage.REPORTS_WRITTEN,
      reportId,
      "a.xml",
      2,
      fileNames -> {}
    );

    long ttl = redissonClient
      .getSet("BARRIER_" + Stage.REPORTS_WRITTEN.name() + (char) 95 + reportId)
      .remainTimeToLive();
    // Covers both -1 (key with no TTL) and -2 (no key at all).
    assertTrue(ttl > 0, "the arrival set must carry a TTL, got " + ttl);
  }
}
