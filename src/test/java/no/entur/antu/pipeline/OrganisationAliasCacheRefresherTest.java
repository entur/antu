package no.entur.antu.pipeline;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import no.entur.antu.job.JobQueue;
import no.entur.antu.leader.LeaderElection;
import no.entur.antu.validation.validator.organisation.OrganisationAliasRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrganisationAliasCacheRefresherTest {

  private OrganisationAliasRepository organisationAliasRepository;
  private LeaderElection leaderElection;
  private JobQueue jobQueue;
  private OrganisationAliasCacheRefresher refresher;

  @BeforeEach
  void setUp() {
    organisationAliasRepository = mock(OrganisationAliasRepository.class);
    leaderElection = mock(LeaderElection.class);
    jobQueue = mock(JobQueue.class);
    refresher =
      new OrganisationAliasCacheRefresher(
        organisationAliasRepository,
        leaderElection,
        jobQueue
      );
  }

  /**
   * A failed refresh is contained: letting it out nacks the job, and an agreement registry that is
   * down or answering with nothing would then redeliver against the pod's one job consumer, at
   * PubSub's rate rather than the refresh interval's. The retry is the next scheduled refresh.
   */
  @Test
  void aFailedRefreshIsContained() {
    doThrow(new IllegalStateException("agreement registry is down"))
      .when(organisationAliasRepository)
      .refreshCache();

    assertDoesNotThrow(() -> refresher.refresh());

    verify(organisationAliasRepository).refreshCache();
  }

  @Test
  void aSuccessfulRefreshCallsTheRepository() {
    refresher.refresh();

    verify(organisationAliasRepository).refreshCache();
  }
}
