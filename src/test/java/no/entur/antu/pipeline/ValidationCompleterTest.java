package no.entur.antu.pipeline;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import no.entur.antu.config.EmbeddedRedisTestBase;
import no.entur.antu.job.ValidationContext;
import no.entur.antu.job.ValidationStatus;
import no.entur.antu.job.ValidationStatusNotifier;
import no.entur.antu.metrics.AntuPrometheusMetricsService;
import org.entur.netex.validation.validator.ValidationReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ValidationCompleterTest extends EmbeddedRedisTestBase {

  private ValidationContext context;
  private ValidationReportStore validationReportStore;
  private ValidationStatusNotifier validationStatusNotifier;
  private ValidationCacheCleaner validationCacheCleaner;

  @BeforeEach
  void setUp() {
    // A fresh report id per test, so one test's completion claim cannot decide another's outcome.
    context =
      ValidationContext
        .builder()
        .referential("rb_tst")
        .codespace("tst")
        .validationReportId("report-" + UUID.randomUUID())
        .build();
    validationReportStore = mock(ValidationReportStore.class);
    validationStatusNotifier = mock(ValidationStatusNotifier.class);
    validationCacheCleaner = mock(ValidationCacheCleaner.class);
  }

  private ValidationCompleter completer() {
    return new ValidationCompleter(
      validationReportStore,
      validationStatusNotifier,
      mock(AntuPrometheusMetricsService.class),
      validationCacheCleaner,
      redissonClient
    );
  }

  /**
   * A failed clean-up is the likeliest reason a completed job comes back at all. Returning without cleaning
   * up would leave the validation state live, and the sweeper would later send a TIMEOUT for a validation
   * the client already has an OK or FAILED for.
   */
  @Test
  void aRedeliveredCompletionCleansUpAgain() {
    when(validationReportStore.reportAlreadyPublished(context))
      .thenReturn(true);

    completer().complete(context, cleanReport());

    verify(validationCacheCleaner).cleanUp(context.validationReportId());
  }

  /**
   * Cleaning up again must not turn into telling the client twice.
   */
  @Test
  void aRedeliveredCompletionDoesNotNotifyOrPublishTwice() {
    when(validationReportStore.reportAlreadyPublished(context))
      .thenReturn(true);

    completer().complete(context, cleanReport());

    verify(validationStatusNotifier, never()).notifyStatus(any(), any());
    verify(validationReportStore, never()).publishReport(any(), any());
    verify(validationReportStore, never()).markReportPublished(any());
  }

  /**
   * The published marker cannot serialise concurrent deliveries: both workers look before either writes, so
   * both publish and both notify, and marduk routes an OK onward. This is the case the atomic claim exists
   * for. The marker is left absent throughout on purpose, to isolate the claim.
   */
  @Test
  void concurrentCompletionsNotifyTheClientOnce() throws Exception {
    when(validationReportStore.reportAlreadyPublished(context))
      .thenReturn(false);
    int pods = 8;

    try (ExecutorService executor = Executors.newFixedThreadPool(4)) {
      List<Callable<Void>> completions = IntStream
        .range(0, pods)
        .mapToObj(i ->
          (Callable<Void>) () -> {
            completer().complete(context, cleanReport());
            return null;
          }
        )
        .toList();
      for (Future<Void> result : executor.invokeAll(completions)) {
        result.get();
      }
    }

    verify(validationStatusNotifier, times(1))
      .notifyStatus(context, ValidationStatus.OK);
    verify(validationReportStore, times(1)).publishReport(any(), any());
  }

  /**
   * A claim taken before work that then failed would leave the validation with no terminal status at all:
   * the redelivery finds no marker and a taken claim, and does nothing.
   */
  @Test
  void aFailedCompletionReleasesTheClaimForTheRedelivery() {
    when(validationReportStore.reportAlreadyPublished(context))
      .thenReturn(false);
    doThrow(new IllegalStateException("the bucket is unreachable"))
      .when(validationReportStore)
      .publishReport(any(), any());

    assertThrows(
      IllegalStateException.class,
      () -> completer().complete(context, cleanReport())
    );
    assertThrows(
      IllegalStateException.class,
      () -> completer().complete(context, cleanReport())
    );

    // The second attempt got as far as publishing, which it could not if the claim were still held.
    verify(validationReportStore, times(2)).publishReport(any(), any());
  }

  /**
   * The case that made abandon go through the same guards as completion. A duplicated AggregateReports
   * delivery finds the per file reports the first delivery cleaned up, reads them as lost, and concludes
   * the dataset timed out. marduk maps timeout to a failed import, so without this guard a dataset that
   * validated cleanly is reported as a failure. The stale-local-cache path in the sweeper arrives here the
   * same way.
   */
  @Test
  void abandoningAnAlreadyConcludedReportDoesNotContradictIt() {
    when(validationReportStore.reportAlreadyPublished(context))
      .thenReturn(true);

    completer().abandon(context);

    verify(validationStatusNotifier, never()).notifyStatus(any(), any());
    verify(validationCacheCleaner).cleanUp(context.validationReportId());
  }

  /**
   * Completion and abandonment share one claim, so a validation cannot be concluded twice in either order.
   * Here the sweeper gives up on a report while its completion is on the way.
   */
  @Test
  void aCompletionAfterAnAbandonmentDoesNotSendASecondStatus() {
    // The marker is written by whichever conclusion runs first, so track it rather than pinning it false.
    when(validationReportStore.reportAlreadyPublished(context))
      .thenReturn(false);
    ValidationCompleter completer = completer();
    doAnswer(invocation -> {
        when(validationReportStore.reportAlreadyPublished(context))
          .thenReturn(true);
        return null;
      })
      .when(validationReportStore)
      .markReportPublished(context);

    completer.abandon(context);
    completer.complete(context, cleanReport());

    verify(validationStatusNotifier, times(1))
      .notifyStatus(context, ValidationStatus.TIMEOUT);
    verify(validationStatusNotifier, never())
      .notifyStatus(context, ValidationStatus.OK);
    verify(validationReportStore, never()).publishReport(any(), any());
  }

  /**
   * Same race as concurrent completions, on the path that had no claim at all: several pods can decide a
   * validation has timed out at the same instant.
   */
  @Test
  void concurrentAbandonmentsNotifyTheClientOnce() throws Exception {
    when(validationReportStore.reportAlreadyPublished(context))
      .thenReturn(false);
    int pods = 8;

    try (ExecutorService executor = Executors.newFixedThreadPool(4)) {
      List<Callable<Void>> abandonments = IntStream
        .range(0, pods)
        .mapToObj(i ->
          (Callable<Void>) () -> {
            completer().abandon(context);
            return null;
          }
        )
        .toList();
      for (Future<Void> result : executor.invokeAll(abandonments)) {
        result.get();
      }
    }

    verify(validationStatusNotifier, times(1))
      .notifyStatus(context, ValidationStatus.TIMEOUT);
  }

  /**
   * As for completion: a claim taken before work that then failed would leave the validation with no
   * terminal status at all.
   */
  @Test
  void aFailedAbandonmentReleasesTheClaimForTheRedelivery() {
    when(validationReportStore.reportAlreadyPublished(context))
      .thenReturn(false);
    doThrow(new IllegalStateException("the status topic is unreachable"))
      .when(validationStatusNotifier)
      .notifyStatus(context, ValidationStatus.TIMEOUT);

    assertThrows(
      IllegalStateException.class,
      () -> completer().abandon(context)
    );
    assertThrows(
      IllegalStateException.class,
      () -> completer().abandon(context)
    );

    verify(validationStatusNotifier, times(2))
      .notifyStatus(context, ValidationStatus.TIMEOUT);
  }

  private ValidationReport cleanReport() {
    return new ValidationReport(
      context.codespace(),
      context.validationReportId()
    );
  }
}
