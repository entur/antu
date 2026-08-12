package no.entur.antu.pipeline;

import no.entur.antu.job.AntuJob;
import no.entur.antu.job.JobQueue;
import no.entur.antu.leader.LeaderElection;
import no.entur.antu.leader.LeadershipGrantedEvent;
import no.entur.antu.validation.validator.organisation.OrganisationAliasRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Keeps the organisation alias cache current, on the same leader-decides, anyone-executes split as
 * {@link StopPlaceCacheRefresher}.
 */
@Component
public class OrganisationAliasCacheRefresher {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    OrganisationAliasCacheRefresher.class
  );

  private final OrganisationAliasRepository organisationAliasRepository;
  private final LeaderElection leaderElection;
  private final JobQueue jobQueue;

  public OrganisationAliasCacheRefresher(
    OrganisationAliasRepository organisationAliasRepository,
    LeaderElection leaderElection,
    JobQueue jobQueue
  ) {
    this.organisationAliasRepository = organisationAliasRepository;
    this.leaderElection = leaderElection;
    this.jobQueue = jobQueue;
  }

  @EventListener
  public void onLeadershipGranted(LeadershipGrantedEvent event) {
    // Contained here: the event is multicast to both cache primers, and an exception escaping one
    // aborts the multicast, so the other would never run and the event is published only once.
    try {
      prime();
    } catch (Exception e) {
      LOGGER.error(
        "Failed to prime the organisation alias cache on taking over as leader",
        e
      );
    }
  }

  private void prime() {
    if (organisationAliasRepository.isEmpty()) {
      LOGGER.info("Organisation alias cache is empty, priming cache");
      organisationAliasRepository.refreshCache();
    } else {
      LOGGER.info("Existing organisation alias cache found");
    }
  }

  @Scheduled(
    fixedDelayString = "${antu.organisation.refresh.millis:1800000}",
    initialDelayString = "${antu.organisation.refresh.millis:1800000}"
  )
  void refreshIfLeader() {
    if (!leaderElection.isLeader()) {
      return;
    }
    enqueueRefresh();
  }

  /**
   * Put the refresh on the job queue. Also reachable through the cache admin API.
   */
  public void enqueueRefresh() {
    LOGGER.info("Scheduling organisation alias cache refresh job");
    jobQueue.submit(new AntuJob.RefreshOrganisationAliasCache());
  }

  public void refresh() {
    LOGGER.info("Refreshing organisation alias cache");
    organisationAliasRepository.refreshCache();
    LOGGER.info("Refreshed organisation alias cache");
  }
}
