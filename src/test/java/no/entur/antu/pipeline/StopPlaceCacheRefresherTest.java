package no.entur.antu.pipeline;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import no.entur.antu.job.AntuJob;
import no.entur.antu.job.JobQueue;
import no.entur.antu.leader.LeaderElection;
import no.entur.antu.leader.LeadershipGrantedEvent;
import no.entur.antu.leader.LeadershipLostEvent;
import no.entur.antu.stop.StopPlaceDatasetVersionRepository;
import no.entur.antu.stop.StopPlaceRepositoryLoader;
import no.entur.antu.stop.changelog.StopPlaceRepositoryUpdater;
import no.entur.antu.stop.loader.StopPlacesDatasetLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class StopPlaceCacheRefresherTest {

  private static final String DATASET = "1787306879708878";

  private StopPlaceRepositoryLoader stopPlaceRepository;
  private StopPlaceRepositoryUpdater stopPlaceRepositoryUpdater;
  private StopPlacesDatasetLoader stopPlacesDatasetLoader;
  private StopPlaceDatasetVersionRepository datasetVersionRepository;
  private LeaderElection leaderElection;
  private JobQueue jobQueue;
  private StopPlaceCacheRefresher refresher;

  @BeforeEach
  void setUp() {
    stopPlaceRepository = mock(StopPlaceRepositoryLoader.class);
    stopPlaceRepositoryUpdater = mock(StopPlaceRepositoryUpdater.class);
    stopPlacesDatasetLoader = mock(StopPlacesDatasetLoader.class);
    datasetVersionRepository = mock(StopPlaceDatasetVersionRepository.class);
    leaderElection = mock(LeaderElection.class);
    jobQueue = mock(JobQueue.class);
    refresher =
      new StopPlaceCacheRefresher(
        stopPlaceRepository,
        stopPlaceRepositoryUpdater,
        stopPlacesDatasetLoader,
        datasetVersionRepository,
        leaderElection,
        jobQueue
      );
  }

  /**
   * A cache lost to a flushed or evicted Redis has to come back without waiting for the next export, so
   * this ignores the recorded version.
   */
  @Test
  void aColdCacheIsQueuedForRefreshOnTakingOverAsLeader() {
    when(stopPlaceRepository.isEmpty()).thenReturn(true);
    when(stopPlacesDatasetLoader.loadDatasetVersion()).thenReturn(DATASET);
    when(datasetVersionRepository.get()).thenReturn(DATASET);

    refresher.onLeadershipGranted(new LeadershipGrantedEvent());

    verify(jobQueue).submit(new AntuJob.RefreshStopPlaceCache());
    verify(stopPlaceRepositoryUpdater, never()).createOrUpdate();
    verify(stopPlaceRepositoryUpdater).init();
  }

  @Test
  void anExistingCacheIsKeptAndTheUpdaterInitialised() {
    when(stopPlaceRepository.isEmpty()).thenReturn(false);
    when(stopPlacesDatasetLoader.loadDatasetVersion()).thenReturn(DATASET);
    when(datasetVersionRepository.get()).thenReturn(DATASET);

    refresher.onLeadershipGranted(new LeadershipGrantedEvent());

    verify(stopPlaceRepositoryUpdater).init();
    verifyNoInteractions(jobQueue);
  }

  /**
   * A cold cache is filled by the queued refresh, so an updater that cannot start must not take the
   * refresh with it.
   */
  @Test
  void aColdCacheIsQueuedEvenIfTheUpdaterCannotBeInitialised() {
    when(stopPlaceRepository.isEmpty()).thenReturn(true);
    when(stopPlacesDatasetLoader.loadDatasetVersion()).thenReturn(DATASET);
    doThrow(new IllegalStateException("no kafka"))
      .when(stopPlaceRepositoryUpdater)
      .init();

    assertDoesNotThrow(() ->
      refresher.onLeadershipGranted(new LeadershipGrantedEvent())
    );

    verify(jobQueue).submit(new AntuJob.RefreshStopPlaceCache());
  }

  /**
   * An existing cache with a newer export in the bucket picks it up on taking over, rather than waiting
   * for the next check.
   */
  @Test
  void aNewDatasetIsQueuedOnTakingOverAsLeader() {
    when(stopPlaceRepository.isEmpty()).thenReturn(false);
    when(stopPlacesDatasetLoader.loadDatasetVersion()).thenReturn(DATASET);
    when(datasetVersionRepository.get()).thenReturn("an-older-generation");

    refresher.onLeadershipGranted(new LeadershipGrantedEvent());

    verify(jobQueue).submit(new AntuJob.RefreshStopPlaceCache());
  }

  @Test
  void onlyTheLeaderDecidesWhenToRefresh() {
    when(leaderElection.isLeader()).thenReturn(false);

    refresher.refreshIfDatasetChanged();

    verifyNoInteractions(jobQueue);
    verifyNoInteractions(stopPlacesDatasetLoader);
    verifyNoInteractions(stopPlaceRepositoryUpdater);
  }

  @Test
  void aNewDatasetInTheBucketIsRefreshed() {
    leaderSees(DATASET, "an-older-generation");

    refresher.refreshIfDatasetChanged();

    verify(jobQueue).submit(new AntuJob.RefreshStopPlaceCache());
  }

  @Test
  void theDatasetTheCacheWasBuiltFromIsNotRefreshedAgain() {
    leaderSees(DATASET, DATASET);

    refresher.refreshIfDatasetChanged();

    verifyNoInteractions(jobQueue);
  }

  @Test
  void aMissingDatasetIsNotRefreshed() {
    leaderSees(null, null);

    refresher.refreshIfDatasetChanged();

    verifyNoInteractions(jobQueue);
  }

  /**
   * Recorded once the job is queued, not before: a pod dying between the Redis write and PubSub confirming
   * the message would otherwise leave the export recorded with no job to load it, and every later check
   * would see it as loaded.
   */
  @Test
  void theDatasetIsRecordedAfterTheJobIsQueued() {
    leaderSees(DATASET, "an-older-generation");

    refresher.refreshIfDatasetChanged();

    InOrder inOrder = inOrder(jobQueue, datasetVersionRepository);
    inOrder.verify(jobQueue).submit(new AntuJob.RefreshStopPlaceCache());
    inOrder.verify(datasetVersionRepository).set(DATASET);
  }

  /**
   * A failed refresh is contained: letting it out nacks the job, and a dataset that cannot be loaded at
   * all would redeliver against the pod's one job consumer, parsing the whole export each time. Clearing
   * the recorded version is the retry, at the next check rather than at PubSub's redelivery rate.
   */
  @Test
  void aFailedRefreshIsContainedAndClearsTheRecordedVersion() {
    when(datasetVersionRepository.get()).thenReturn(DATASET);
    doThrow(new IllegalStateException("boom"))
      .when(stopPlaceRepositoryUpdater)
      .createOrUpdate();

    assertDoesNotThrow(() -> refresher.refresh());

    verify(datasetVersionRepository).clearIfStill(DATASET);
  }

  @Test
  void aSuccessfulRefreshKeepsTheRecordedVersion() {
    refresher.refresh();

    verify(stopPlaceRepositoryUpdater).createOrUpdate();
    verify(datasetVersionRepository, never()).clearIfStill(anyString());
  }

  /**
   * A submit that never reached PubSub must leave nothing recorded, so the next check retries it.
   */
  @Test
  void aDatasetWhoseJobCouldNotBeQueuedIsNotRecorded() {
    leaderSees(DATASET, "an-older-generation");
    doThrow(new IllegalStateException("pubsub is down"))
      .when(jobQueue)
      .submit(new AntuJob.RefreshStopPlaceCache());

    assertThrows(
      IllegalStateException.class,
      () -> refresher.refreshIfDatasetChanged()
    );

    verify(datasetVersionRepository, never()).set(anyString());
  }

  /**
   * The changelog consumer is the leader's, so losing the lease has to take it down.
   */
  @Test
  void losingLeadershipStandsTheUpdaterDown() {
    refresher.onLeadershipLost(new LeadershipLostEvent());

    verify(stopPlaceRepositoryUpdater).standDown();
  }

  @Test
  void aStandDownThatFailsIsContained() {
    doThrow(new IllegalStateException("kafka is down"))
      .when(stopPlaceRepositoryUpdater)
      .standDown();

    assertDoesNotThrow(() ->
      refresher.onLeadershipLost(new LeadershipLostEvent())
    );
  }

  private void leaderSees(String inBucket, String recorded) {
    when(leaderElection.isLeader()).thenReturn(true);
    when(stopPlacesDatasetLoader.loadDatasetVersion()).thenReturn(inBucket);
    when(datasetVersionRepository.get()).thenReturn(recorded);
  }
}
