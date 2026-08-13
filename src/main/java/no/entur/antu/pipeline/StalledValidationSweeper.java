package no.entur.antu.pipeline;

import java.time.Duration;
import java.time.Instant;
import no.entur.antu.config.cache.ValidationState;
import no.entur.antu.job.ValidationContext;
import no.entur.antu.job.ValidationMdc;
import no.entur.antu.leader.LeaderElection;
import no.entur.antu.validation.state.ValidationStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Gives up on a validation that has stopped making progress, and tells the client so.
 *
 * <p>Every step hands off to the next through the job queue, and a hand-off can be lost: a job can be
 * redelivered until it ages out of the subscription's retention, a pod can be killed between passing a
 * barrier and publishing the next job, Redis can evict an arrival set under memory pressure. None of those
 * produce an error anywhere — the dataset simply stops, and the client is left holding a STARTED that never
 * resolves.
 *
 * <p>Camel bounded this with the aggregator's {@code completionTimeout}: a dataset whose files stopped
 * arriving was concluded anyway. Nothing about the Redis barriers reproduces that, so this does. It is what
 * makes a STARTED eventually reach a terminal status in every case but one: {@link ValidationInitializer}
 * deliberately drops the state of a report whose first hand-off failed, so that id is never concluded and
 * the redelivery's id is the one the client hears about.
 *
 * <p>Like {@code completionTimeout}, the threshold is on inactivity rather than total duration, so a large
 * dataset still working through its files is not given up on.
 */
@Component
public class StalledValidationSweeper {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    StalledValidationSweeper.class
  );

  private final ValidationStateRepository validationStateRepository;
  private final ValidationCompleter validationCompleter;
  private final ValidationCacheCleaner validationCacheCleaner;
  private final LeaderElection leaderElection;
  private final Duration stalledAfter;

  public StalledValidationSweeper(
    ValidationStateRepository validationStateRepository,
    ValidationCompleter validationCompleter,
    ValidationCacheCleaner validationCacheCleaner,
    LeaderElection leaderElection,
    @Value(
      "${antu.validation.stalled.after.millis:1800000}"
    ) long stalledAfterMillis
  ) {
    this.validationStateRepository = validationStateRepository;
    this.validationCompleter = validationCompleter;
    this.validationCacheCleaner = validationCacheCleaner;
    this.leaderElection = leaderElection;
    this.stalledAfter = Duration.ofMillis(stalledAfterMillis);
  }

  /**
   * On the leader only, so a stalled validation produces one notification rather than one per pod.
   */
  @Scheduled(
    fixedDelayString = "${antu.validation.sweep.millis:300000}",
    initialDelayString = "${antu.validation.sweep.millis:300000}"
  )
  void sweepIfLeader() {
    if (!leaderElection.isLeader()) {
      return;
    }
    sweep();
  }

  void sweep() {
    Instant cutoff = Instant.now().minus(stalledAfter);
    validationStateRepository
      .allValidationStates()
      .forEach((validationReportId, state) -> {
        if (hasStalled(state, cutoff)) {
          giveUp(validationReportId);
        }
      });
  }

  /**
   * A state written before this field existed has no timestamp. Leave it to expire on its own TTL rather
   * than guess that it stalled.
   */
  private static boolean hasStalled(ValidationState state, Instant cutoff) {
    return (
      state.getLastProgressAt() != null &&
      state.getLastProgressAt().isBefore(cutoff)
    );
  }

  /**
   * Re-reads the state before concluding. {@code allValidationStates()} is a snapshot and giving up on one
   * validation does Redis and PubSub work, so a worker can have recorded progress in between. Telling a
   * client its validation timed out while that validation is in fact still running is worse than sweeping
   * it up one round later.
   *
   * <p>The re-read is a weaker check than it looks: {@code allValidationStates()} iterates, which Redisson
   * serves from Redis, while {@code getValidationState} is a {@code get}, which an {@code RLocalCachedMap}
   * serves from the local cache. Invalidation is pub/sub only and {@code reconnectionStrategy} defaults to
   * {@code NONE}, so after a reconnect this can read an entry another pod has already removed. It catches
   * the ordinary progress-during-the-sweep case and nothing more; what actually stops a finished validation
   * being reported as timed out is the {@code .status} marker {@link ValidationCompleter#abandon} checks,
   * which is read from the blob store rather than from any cache.
   */
  private void giveUp(String validationReportId) {
    ValidationState state = validationStateRepository.getValidationState(
      validationReportId
    );
    if (state == null) {
      return;
    }
    if (!hasStalled(state, Instant.now().minus(stalledAfter))) {
      LOGGER.info(
        "Validation {} made progress while the sweep was running, leaving it alone",
        validationReportId
      );
      return;
    }

    ValidationContext context = state.getContext();
    if (context == null) {
      LOGGER.warn(
        "Validation {} has stalled but carries no context, so its client cannot be notified. Cleaning up.",
        validationReportId
      );
      validationCacheCleaner.cleanUp(validationReportId);
      return;
    }

    ValidationMdc.set(context);
    try {
      // Worded to match the log-based metric that alerts on "System error": a stalled validation is
      // exactly what an operator needs woken for, and nothing else reports it.
      // The timestamp rather than an elapsed time: it is what an operator scopes a log query to.
      LOGGER.error(
        "System error: validation {} has made no progress since {}, giving up",
        validationReportId,
        state.getLastProgressAt()
      );
      // Through the completer, not straight to the notifier: the state read above comes from a local
      // cache that can be stale, so this is the last point at which a validation that already finished
      // can be caught before its client is told it timed out.
      validationCompleter.abandon(context);
    } finally {
      ValidationMdc.clear();
    }
  }
}
