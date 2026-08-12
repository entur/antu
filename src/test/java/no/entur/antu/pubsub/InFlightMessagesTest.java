package no.entur.antu.pubsub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.DefaultLifecycleProcessor;

class InFlightMessagesTest {

  @Test
  void stopWaitsForTheMessageToFinish() throws Exception {
    InFlightMessages inFlightMessages = new InFlightMessages(30);
    inFlightMessages.start();
    CountDownLatch started = new CountDownLatch(1);
    AtomicBoolean finished = new AtomicBoolean();

    Thread worker = processing(inFlightMessages, started, 400, finished);
    assertTrue(started.await(5, TimeUnit.SECONDS));

    inFlightMessages.stop();

    assertTrue(
      finished.get(),
      "stop() returned while a message was still being processed"
    );
    assertEquals(0, inFlightMessages.inFlightCount());
    worker.join();
  }

  @Test
  void stopGivesUpOnAMessageThatOutlastsTheTimeout() throws Exception {
    InFlightMessages inFlightMessages = new InFlightMessages(1);
    inFlightMessages.start();
    CountDownLatch started = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);

    Thread worker = new Thread(() ->
      inFlightMessages.track(() -> {
        started.countDown();
        awaitUninterruptibly(release);
      })
    );
    worker.start();
    assertTrue(started.await(5, TimeUnit.SECONDS));

    long startedAt = System.nanoTime();
    inFlightMessages.stop();
    long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;

    assertTrue(
      elapsedMillis >= 1000,
      "stop() gave up after " + elapsedMillis + " ms, before the 1 s timeout"
    );
    assertEquals(
      1,
      inFlightMessages.inFlightCount(),
      "the abandoned message should still be counted"
    );

    release.countDown();
    worker.join();
  }

  @Test
  void aFailedMessageLeavesNothingInFlight() {
    InFlightMessages inFlightMessages = new InFlightMessages(1);

    assertThrows(
      IllegalStateException.class,
      () ->
        inFlightMessages.track(() -> {
          throw new IllegalStateException("processing failed");
        })
    );

    assertEquals(0, inFlightMessages.inFlightCount());
  }

  /**
   * The drain only works if the lifecycle processor lets {@code stop()} block for as long as it needs. Its
   * shutdown phase timeout is deliberately set well below the drain here: if that timeout applied to
   * {@code stop()}, the close would return before the message finished, and in production the drain would
   * silently cap at 30 s rather than the configured 175.
   */
  @Test
  void closingTheContextWaitsForTheDrainPastTheShutdownPhaseTimeout()
    throws Exception {
    DefaultLifecycleProcessor lifecycleProcessor =
      new DefaultLifecycleProcessor();
    lifecycleProcessor.setTimeoutPerShutdownPhase(200);

    AtomicBoolean finished = new AtomicBoolean();
    try (
      AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()
    ) {
      context.registerBean(
        "lifecycleProcessor",
        DefaultLifecycleProcessor.class,
        () -> lifecycleProcessor
      );
      context.registerBean(
        InFlightMessages.class,
        () -> new InFlightMessages(30)
      );
      context.refresh();

      InFlightMessages inFlightMessages = context.getBean(
        InFlightMessages.class
      );
      CountDownLatch started = new CountDownLatch(1);
      Thread worker = processing(inFlightMessages, started, 1000, finished);
      assertTrue(started.await(5, TimeUnit.SECONDS));

      context.close();

      assertTrue(
        finished.get(),
        "closing the context did not wait for the in-flight message"
      );
      worker.join();
    }
  }

  /**
   * The drain waits for work that is genuinely in flight, so the work has to genuinely take time. A latch
   * cannot stand in for it: releasing one would need the test to know that stop() had already blocked.
   */
  @SuppressWarnings("java:S2925")
  private static Thread processing(
    InFlightMessages inFlightMessages,
    CountDownLatch started,
    long durationMillis,
    AtomicBoolean finished
  ) {
    Thread worker = new Thread(() ->
      inFlightMessages.track(() -> {
        started.countDown();
        try {
          Thread.sleep(durationMillis);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          return;
        }
        finished.set(true);
      })
    );
    worker.start();
    return worker;
  }

  private static void awaitUninterruptibly(CountDownLatch latch) {
    try {
      latch.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
