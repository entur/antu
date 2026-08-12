package no.entur.antu.pipeline;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import no.entur.antu.config.cache.ValidationState;
import no.entur.antu.job.ValidationContext;
import no.entur.antu.leader.LeaderElection;
import no.entur.antu.validation.state.ValidationStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * This is what guarantees that every STARTED reaches a terminal status. Camel's aggregator did it with
 * completionTimeout; if this stops working, a lost hand-off leaves the validation client waiting forever
 * and nothing anywhere reports it.
 */
class StalledValidationSweeperTest {

  private static final long STALLED_AFTER_MILLIS = 1_800_000;

  private static final ValidationContext CONTEXT = ValidationContext
    .builder()
    .referential("rb_tst")
    .codespace("tst")
    .validationReportId("rb_tst_20260811103000000000")
    .correlationId("correlation-1")
    .datasetFileHandle("inbound/received/tst/dataset.zip")
    .build();

  private ValidationStateRepository validationStateRepository;
  private ValidationCompleter validationCompleter;
  private ValidationCacheCleaner validationCacheCleaner;
  private LeaderElection leaderElection;
  private StalledValidationSweeper sweeper;

  @BeforeEach
  void setUp() {
    validationStateRepository = mock(ValidationStateRepository.class);
    validationCompleter = mock(ValidationCompleter.class);
    validationCacheCleaner = mock(ValidationCacheCleaner.class);
    leaderElection = mock(LeaderElection.class);
    when(leaderElection.isLeader()).thenReturn(true);
    sweeper =
      new StalledValidationSweeper(
        validationStateRepository,
        validationCompleter,
        validationCacheCleaner,
        leaderElection,
        STALLED_AFTER_MILLIS
      );
  }

  private void inProgress(ValidationState state) {
    when(validationStateRepository.allValidationStates())
      .thenReturn(Map.of(CONTEXT.validationReportId(), state));
    // The sweep re-reads before concluding, so both views have to agree unless a test is about them
    // disagreeing.
    when(
      validationStateRepository.getValidationState(CONTEXT.validationReportId())
    )
      .thenReturn(state);
  }

  private static ValidationState lastProgress(long minutesAgo) {
    return new ValidationState(
      CONTEXT,
      Instant.now().minus(minutesAgo, ChronoUnit.MINUTES)
    );
  }

  @Test
  void aStalledValidationIsReportedAsTimeoutAndCleanedUp() {
    inProgress(lastProgress(31));

    sweeper.sweep();

    verify(validationCompleter).abandon(CONTEXT);
  }

  /**
   * The threshold is on inactivity, not total duration: a large dataset still working through its files
   * must not be given up on. This is the behaviour Camel's completionTimeout had.
   */
  @Test
  void aValidationStillMakingProgressIsLeftAlone() {
    inProgress(lastProgress(29));

    sweeper.sweep();

    verifyNoInteractions(validationCompleter, validationCacheCleaner);
  }

  /**
   * marduk routes this status to JobEvent.State.TIMEOUT, which is distinct from a dataset that failed
   * validation. Reporting FAILED instead would blame the data for an antu problem.
   *
   * <p>The sweep can only reach the timeout conclusion: abandon() is the one way out of here and it sends
   * nothing else. That the status on the wire is timeout is pinned by ValidationCompleterTest.
   */
  @Test
  void theStatusSaysAntuGaveUpNotThatTheDatasetIsInvalid() {
    inProgress(lastProgress(31));

    sweeper.sweep();

    verify(validationCompleter).abandon(CONTEXT);
    verify(validationCompleter, never()).complete(any(), any());
    verify(validationCompleter, never()).complete(any());
  }

  /**
   * One notification per stalled validation, not one per pod.
   */
  @Test
  void onlyTheLeaderSweeps() {
    when(leaderElection.isLeader()).thenReturn(false);
    inProgress(lastProgress(31));

    sweeper.sweepIfLeader();

    verifyNoInteractions(validationStateRepository, validationCompleter);
  }

  /**
   * A state written before the timestamp existed cannot be judged, and guessing would notify a client
   * whose validation may still be running.
   */
  @Test
  void aStateWithoutATimestampIsLeftToItsTtl() {
    inProgress(new ValidationState());

    sweeper.sweep();

    verifyNoInteractions(validationCompleter, validationCacheCleaner);
  }

  /**
   * The sweep works off a snapshot and does Redis and PubSub work per validation, so a worker can record
   * progress while it runs. Concluding from the snapshot would tell a client its validation timed out while
   * that validation is still going.
   */
  @Test
  void progressRecordedDuringTheSweepCancelsGivingUp() {
    when(validationStateRepository.allValidationStates())
      .thenReturn(Map.of(CONTEXT.validationReportId(), lastProgress(31)));
    when(
      validationStateRepository.getValidationState(CONTEXT.validationReportId())
    )
      .thenReturn(lastProgress(0));

    sweeper.sweep();

    verifyNoInteractions(validationCompleter, validationCacheCleaner);
  }

  /**
   * Completed between the snapshot and the re-read: its state is already gone, so there is nothing to
   * notify and nothing to clean.
   */
  @Test
  void aValidationThatFinishedDuringTheSweepIsLeftAlone() {
    when(validationStateRepository.allValidationStates())
      .thenReturn(Map.of(CONTEXT.validationReportId(), lastProgress(31)));
    when(
      validationStateRepository.getValidationState(CONTEXT.validationReportId())
    )
      .thenReturn(null);

    sweeper.sweep();

    verifyNoInteractions(validationCompleter, validationCacheCleaner);
  }

  /**
   * Without a context there is nothing to notify, but the Redis state still has to go or the sweep keeps
   * finding it every five minutes.
   */
  @Test
  void aStateWithoutAContextIsCleanedUpSilently() {
    ValidationState state = new ValidationState(
      null,
      Instant.now().minus(31, ChronoUnit.MINUTES)
    );
    inProgress(state);

    sweeper.sweep();

    verify(validationCacheCleaner).cleanUp(CONTEXT.validationReportId());
    verifyNoInteractions(validationCompleter);
  }
}
