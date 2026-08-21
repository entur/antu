package no.entur.antu.pipeline;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import no.entur.antu.job.AntuJob;
import no.entur.antu.job.JobQueue;
import no.entur.antu.leader.LeaderElection;
import no.entur.antu.leader.LeadershipGrantedEvent;
import no.entur.antu.stop.StopPlaceDatasetVersionRepository;
import no.entur.antu.stop.StopPlaceRepositoryLoader;
import no.entur.antu.stop.changelog.StopPlaceRepositoryUpdater;
import no.entur.antu.stop.loader.StopPlacesDatasetLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StopPlaceCacheRefresherTest {

  private static final String DATASET =
    "tiamat-export-CurrentAndFuture-202608211200010574.xml";

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
    when(datasetVersionRepository.claimRefresh()).thenReturn(true);
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

  /**
   * With the watch picking up each export within minutes, an unconditional daily refresh only ever
   * parsed a dataset the cache was already built from.
   */
  @Test
  void theCronDoesNotReparseTheDatasetTheCacheWasBuiltFrom() {
    when(leaderElection.isLeader()).thenReturn(true);
    when(stopPlacesDatasetLoader.loadDatasetVersion()).thenReturn(DATASET);
    when(datasetVersionRepository.get()).thenReturn(DATASET);

    refresher.refreshIfLeader();

    verifyNoInteractions(jobQueue);
  }

  @Test
  void theCronRefreshesWhenTheDatasetHasChanged() {
    when(leaderElection.isLeader()).thenReturn(true);
    when(stopPlacesDatasetLoader.loadDatasetVersion()).thenReturn(DATASET);
    when(datasetVersionRepository.get()).thenReturn("an-older-export.xml");

    refresher.refreshIfLeader();

    verify(jobQueue).submit(new AntuJob.RefreshStopPlaceCache());
  }

  /**
   * A cache lost to a flushed or evicted Redis has to be rebuilt without waiting for the next export.
   */
  @Test
  void anEmptyCacheIsRefreshedEvenFromTheDatasetItWasBuiltFrom() {
    when(leaderElection.isLeader()).thenReturn(true);
    when(stopPlacesDatasetLoader.loadDatasetVersion()).thenReturn(DATASET);
    when(datasetVersionRepository.get()).thenReturn(DATASET);
    when(stopPlaceRepository.isEmpty()).thenReturn(true);

    refresher.watchStopPlaceSources();

    verify(jobQueue).submit(new AntuJob.RefreshStopPlaceCache());
  }

  /**
   * The admin endpoint is the way to rebuild a cache the operator has a reason to distrust, so it must
   * not be subject to the version check.
   */
  @Test
  void aForcedRefreshForgetsTheRecordedVersion() {
    refresher.forceRefresh();

    verify(datasetVersionRepository).clear();
    verify(jobQueue).submit(new AntuJob.RefreshStopPlaceCache());
  }

  @Test
  void onlyTheLeaderDecidesWhenToRefresh() {
    when(leaderElection.isLeader()).thenReturn(false);

    refresher.refreshIfLeader();

    verifyNoInteractions(jobQueue);
  }

  @Test
  void onlyTheLeaderWatchesTheDataset() {
    when(leaderElection.isLeader()).thenReturn(false);

    refresher.watchStopPlaceSources();

    verifyNoInteractions(jobQueue);
    verifyNoInteractions(stopPlacesDatasetLoader);
    verifyNoInteractions(stopPlaceRepositoryUpdater);
  }

  @Test
  void aNewDatasetInTheBucketIsRefreshedWithoutWaitingForTheCron() {
    when(leaderElection.isLeader()).thenReturn(true);
    when(stopPlacesDatasetLoader.loadDatasetVersion()).thenReturn(DATASET);
    when(datasetVersionRepository.get()).thenReturn("an-older-export.xml");

    refresher.watchStopPlaceSources();

    verify(jobQueue).submit(new AntuJob.RefreshStopPlaceCache());
  }

  @Test
  void theDatasetTheCacheWasBuiltFromIsNotRefreshedAgain() {
    when(leaderElection.isLeader()).thenReturn(true);
    when(stopPlacesDatasetLoader.loadDatasetVersion()).thenReturn(DATASET);
    when(datasetVersionRepository.get()).thenReturn(DATASET);

    refresher.watchStopPlaceSources();

    verifyNoInteractions(jobQueue);
  }

  /**
   * A missing dataset is the state of a fresh environment, and enqueueing a refresh for it would fail
   * the job on every tick.
   */
  @Test
  void aMissingDatasetIsNotRefreshed() {
    when(leaderElection.isLeader()).thenReturn(true);
    when(stopPlacesDatasetLoader.loadDatasetVersion()).thenReturn(null);

    refresher.watchStopPlaceSources();

    verifyNoInteractions(jobQueue);
  }

  @Test
  void theChangelogConsumerIsRestartedIfItStopped() {
    when(leaderElection.isLeader()).thenReturn(true);

    refresher.watchStopPlaceSources();

    verify(stopPlaceRepositoryUpdater).ensureRunning();
  }

  /**
   * The export is the reliable source of the two: a changelog consumer that cannot be restarted must not
   * stop a new export from being picked up.
   */
  @Test
  void aFailedConsumerRestartStillLetsANewDatasetThrough() {
    when(leaderElection.isLeader()).thenReturn(true);
    when(stopPlacesDatasetLoader.loadDatasetVersion()).thenReturn(DATASET);
    when(datasetVersionRepository.get()).thenReturn(null);
    doThrow(new IllegalStateException("no kafka"))
      .when(stopPlaceRepositoryUpdater)
      .ensureRunning();

    refresher.watchStopPlaceSources();

    verify(jobQueue).submit(new AntuJob.RefreshStopPlaceCache());
  }

  /**
   * The version is only written once a refresh has finished, so without the claim every tick between a
   * new export appearing and the job running would queue another parse of it.
   */
  @Test
  void aRefreshAlreadyOnItsWayIsNotQueuedAgain() {
    when(leaderElection.isLeader()).thenReturn(true);
    when(stopPlacesDatasetLoader.loadDatasetVersion()).thenReturn(DATASET);
    when(datasetVersionRepository.get()).thenReturn("an-older-export.xml");
    when(datasetVersionRepository.claimRefresh()).thenReturn(false);

    refresher.watchStopPlaceSources();

    verifyNoInteractions(jobQueue);
  }

  @Test
  void aClaimTakenForAJobThatCouldNotBeQueuedIsReleased() {
    doThrow(new IllegalStateException("pubsub is down"))
      .when(jobQueue)
      .submit(new AntuJob.RefreshStopPlaceCache());

    assertThrows(IllegalStateException.class, () -> refresher.enqueueRefresh());

    verify(datasetVersionRepository).releaseRefresh();
  }

  @Test
  void theClaimIsReleasedWhenTheRefreshIsDone() {
    when(stopPlacesDatasetLoader.loadDatasetVersion()).thenReturn(DATASET);

    refresher.refresh();

    verify(datasetVersionRepository).releaseRefresh();
  }

  /**
   * A redelivery of the refresh job does not come through enqueueRefresh, so a claim held after a failure
   * would keep the next export from being picked up until it expired.
   */
  @Test
  void theClaimIsReleasedWhenTheRefreshFails() {
    when(stopPlacesDatasetLoader.loadDatasetVersion()).thenReturn(DATASET);
    doThrow(new IllegalStateException("boom"))
      .when(stopPlaceRepositoryUpdater)
      .createOrUpdate();

    assertThrows(IllegalStateException.class, () -> refresher.refresh());

    verify(datasetVersionRepository).releaseRefresh();
  }

  /**
   * The version is recorded from the dataset that was in the bucket when the refresh started, and only
   * once it succeeded, so a failed refresh is retried and a newer export is not skipped.
   */
  @Test
  void theLoadedDatasetVersionIsRecorded() {
    when(stopPlacesDatasetLoader.loadDatasetVersion()).thenReturn(DATASET);

    refresher.refresh();

    verify(datasetVersionRepository).set(DATASET);
  }

  @Test
  void aFailedRefreshDoesNotRecordTheDatasetVersion() {
    when(stopPlacesDatasetLoader.loadDatasetVersion()).thenReturn(DATASET);
    doThrow(new IllegalStateException("boom"))
      .when(stopPlaceRepositoryUpdater)
      .createOrUpdate();

    assertThrows(IllegalStateException.class, () -> refresher.refresh());

    verify(datasetVersionRepository, never()).set(DATASET);
  }
}
