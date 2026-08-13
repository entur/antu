package no.entur.antu.pipeline;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import no.entur.antu.job.JobQueue;
import no.entur.antu.leader.LeaderElection;
import no.entur.antu.leader.LeadershipGrantedEvent;
import no.entur.antu.stop.StopPlaceRepositoryLoader;
import no.entur.antu.stop.changelog.StopPlaceRepositoryUpdater;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StopPlaceCacheRefresherTest {

  private StopPlaceRepositoryLoader stopPlaceRepository;
  private StopPlaceRepositoryUpdater stopPlaceRepositoryUpdater;
  private LeaderElection leaderElection;
  private JobQueue jobQueue;
  private StopPlaceCacheRefresher refresher;

  @BeforeEach
  void setUp() {
    stopPlaceRepository = mock(StopPlaceRepositoryLoader.class);
    stopPlaceRepositoryUpdater = mock(StopPlaceRepositoryUpdater.class);
    leaderElection = mock(LeaderElection.class);
    jobQueue = mock(JobQueue.class);
    refresher =
      new StopPlaceCacheRefresher(
        stopPlaceRepository,
        stopPlaceRepositoryUpdater,
        leaderElection,
        jobQueue
      );
  }

  @Test
  void aColdCacheIsFilledOnTakingOverAsLeader() {
    when(stopPlaceRepository.isEmpty()).thenReturn(true);

    refresher.onLeadershipGranted(new LeadershipGrantedEvent());

    verify(stopPlaceRepositoryUpdater).createOrUpdate();
    verifyNoInteractions(jobQueue);
  }

  @Test
  void anExistingCacheIsKeptAndTheUpdaterInitialised() {
    when(stopPlaceRepository.isEmpty()).thenReturn(false);

    refresher.onLeadershipGranted(new LeadershipGrantedEvent());

    verify(stopPlaceRepositoryUpdater).init();
  }

  /**
   * A failed prime must not queue a retry: a permanent failure would redeliver until the subscription's
   * retention runs out, and there is no dead-letter topic. Recovery is the scheduled refresh, and the
   * operator is told through the log metric.
   */
  @Test
  void aFailedPrimeDoesNotQueueARetry() {
    when(stopPlaceRepository.isEmpty()).thenReturn(true);
    doThrow(new NullPointerException("stop dataset not in the bucket yet"))
      .when(stopPlaceRepositoryUpdater)
      .createOrUpdate();

    refresher.onLeadershipGranted(new LeadershipGrantedEvent());

    verifyNoInteractions(jobQueue);
  }

  /**
   * The exception must not escape: the event is multicast to both cache primers, and one throwing would
   * abandon the other, which is only ever published once.
   */
  @Test
  void aFailedPrimeDoesNotEscapeTheListener() {
    when(stopPlaceRepository.isEmpty()).thenReturn(true);
    doThrow(new IllegalStateException("boom"))
      .when(stopPlaceRepositoryUpdater)
      .createOrUpdate();

    assertDoesNotThrow(() ->
      refresher.onLeadershipGranted(new LeadershipGrantedEvent())
    );
  }

  @Test
  void onlyTheLeaderDecidesWhenToRefresh() {
    when(leaderElection.isLeader()).thenReturn(false);

    refresher.refreshIfLeader();

    verifyNoInteractions(jobQueue);
  }
}
