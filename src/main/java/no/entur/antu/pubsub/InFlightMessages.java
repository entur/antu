package no.entur.antu.pubsub;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

/**
 * Holds shutdown open until the messages being processed right now have finished.
 *
 * <p>The HPA scales antu down routinely, so a pod being terminated mid-validation is normal operation
 * rather than an incident. The consumer base class stops the subscribers on {@code ContextClosedEvent} and
 * waits only 10 seconds for the in-flight callbacks, which is far less than a NeTEx file takes: the message
 * is left unacked, redelivered to another pod, and its XSD validation is paid for all over again. Enough of
 * those and a dataset takes considerably longer than it should. Camel bounded the same window with
 * {@code antu.shutdown.timeout=175}, and this is what replaces it.
 *
 * <p>{@code SmartLifecycle.stop()} is the right hook for it. It runs after the {@code ContextClosedEvent}
 * listeners have stopped the subscribers, so nothing new arrives while this waits, and before the beans are
 * destroyed, so Redis, the blob store and the publisher are all still usable by the work being drained.
 */
@Component
public class InFlightMessages implements SmartLifecycle {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    InFlightMessages.class
  );

  private static final Duration POLL_INTERVAL = Duration.ofMillis(200);

  private final AtomicInteger inFlight = new AtomicInteger();
  private final Duration drainTimeout;
  private volatile boolean running;

  public InFlightMessages(
    @Value(
      "${antu.shutdown.drain.timeout.seconds:150}"
    ) long drainTimeoutSeconds
  ) {
    this.drainTimeout = Duration.ofSeconds(drainTimeoutSeconds);
  }

  /**
   * Runs {@code processing}, counting it as in flight for as long as it takes. Failures propagate, so the
   * consumer base class still nacks the message.
   */
  public void track(Runnable processing) {
    inFlight.incrementAndGet();
    try {
      processing.run();
    } finally {
      inFlight.decrementAndGet();
    }
  }

  int inFlightCount() {
    return inFlight.get();
  }

  @Override
  public void start() {
    running = true;
  }

  /** Has to report running, or the lifecycle processor never calls {@link #stop()}. */
  @Override
  public boolean isRunning() {
    return running;
  }

  @Override
  public void stop() {
    running = false;
    if (inFlight.get() == 0) {
      return;
    }
    LOGGER.info(
      "Waiting up to {} s for {} message(s) still being processed",
      drainTimeout.toSeconds(),
      inFlight.get()
    );

    long deadline = System.nanoTime() + drainTimeout.toNanos();
    while (inFlight.get() > 0 && System.nanoTime() - deadline < 0) {
      try {
        Thread.sleep(POLL_INTERVAL);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        LOGGER.warn(
          "Interrupted while draining. {} message(s) will be redelivered.",
          inFlight.get()
        );
        return;
      }
    }

    int abandoned = inFlight.get();
    if (abandoned > 0) {
      LOGGER.warn(
        "Gave up draining after {} s. {} message(s) will be redelivered and revalidated.",
        drainTimeout.toSeconds(),
        abandoned
      );
    } else {
      LOGGER.info("Finished the in-flight messages, shutting down");
    }
  }
}
