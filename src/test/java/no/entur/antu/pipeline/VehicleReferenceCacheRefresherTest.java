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
import no.entur.antu.validation.validator.vehicletype.VehicleRefRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VehicleReferenceCacheRefresherTest {

  private VehicleRefRepository vehicleRefRepository;
  private LeaderElection leaderElection;
  private JobQueue jobQueue;
  private VehicleReferenceCacheRefresher refresher;

  @BeforeEach
  void setUp() {
    vehicleRefRepository = mock(VehicleRefRepository.class);
    leaderElection = mock(LeaderElection.class);
    jobQueue = mock(JobQueue.class);
    refresher =
      new VehicleReferenceCacheRefresher(
        vehicleRefRepository,
        leaderElection,
        jobQueue
      );
  }

  /**
   * A cache lost to a flushed or evicted Redis has to come back without waiting for the next scheduled
   * refresh, so this primes immediately when empty.
   */
  @Test
  void aColdCacheIsPrimedOnTakingOverAsLeader() {
    when(vehicleRefRepository.isEmpty()).thenReturn(true);

    refresher.onLeadershipGranted(new LeadershipGrantedEvent());

    verify(vehicleRefRepository).refreshCache();
    verifyNoInteractions(jobQueue);
  }

  @Test
  void anExistingCacheIsKept() {
    when(vehicleRefRepository.isEmpty()).thenReturn(false);

    refresher.onLeadershipGranted(new LeadershipGrantedEvent());

    verify(vehicleRefRepository, never()).refreshCache();
    verifyNoInteractions(jobQueue);
  }

  /**
   * A cold cache is filled by priming on leadership granted, but a refresh that cannot complete must not
   * take down the leadership event.
   */
  @Test
  void aColdCachePrimingFailureIsContained() {
    when(vehicleRefRepository.isEmpty()).thenReturn(true);
    doThrow(new IllegalStateException("vehicle registry is down"))
      .when(vehicleRefRepository)
      .refreshCache();

    assertDoesNotThrow(() ->
      refresher.onLeadershipGranted(new LeadershipGrantedEvent())
    );

    verify(vehicleRefRepository).refreshCache();
  }

  @Test
  void onlyTheLeaderSchedulesRefresh() {
    when(leaderElection.isLeader()).thenReturn(false);

    refresher.refreshIfLeader();

    verifyNoInteractions(jobQueue);
    verifyNoInteractions(vehicleRefRepository);
  }

  @Test
  void theLeaderEnqueuesARefreshJob() {
    when(leaderElection.isLeader()).thenReturn(true);

    refresher.refreshIfLeader();

    verify(jobQueue).submit(new AntuJob.RefreshVehicleReferenceCache());
  }

  @Test
  void manualEnqueueSubmitsARefreshJob() {
    refresher.enqueueRefresh();

    verify(jobQueue).submit(new AntuJob.RefreshVehicleReferenceCache());
  }

  /**
   * A failed refresh is contained: letting it out would nack the job, and a vehicle registry that cannot
   * be reached would redeliver against the pod's one job consumer repeatedly.
   */
  @Test
  void aFailedRefreshIsContained() {
    doThrow(new IllegalStateException("vehicle registry timeout"))
      .when(vehicleRefRepository)
      .refreshCache();

    assertDoesNotThrow(() -> refresher.refresh());

    verify(vehicleRefRepository).refreshCache();
  }

  @Test
  void aSuccessfulRefreshCallsTheRepository() {
    refresher.refresh();

    verify(vehicleRefRepository).refreshCache();
  }

  /**
   * A submit that never reached PubSub must propagate the exception so the caller can handle it.
   */
  @Test
  void aJobThatCannotBeQueuedThrowsTheException() {
    doThrow(new IllegalStateException("pubsub is down"))
      .when(jobQueue)
      .submit(new AntuJob.RefreshVehicleReferenceCache());

    assertThrows(IllegalStateException.class, () -> refresher.enqueueRefresh());
  }
}
