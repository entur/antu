package no.entur.antu.pipeline;

import no.entur.antu.job.AntuJob;
import no.entur.antu.job.JobQueue;
import no.entur.antu.leader.LeaderElection;
import no.entur.antu.leader.LeadershipGrantedEvent;
import no.entur.antu.validation.validator.organisation.OrganisationAliasRepository;
import no.entur.antu.validation.validator.vehicletype.VehicleRefRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Keeps the vehicle reference cache current, on the same leader-decides, anyone-executes split as
 * {@link StopPlaceCacheRefresher}.
 */
@Component
public class VehicleReferenceCacheRefresher {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    VehicleReferenceCacheRefresher.class
  );

  private final VehicleRefRepository vehicleRefRepository;
  private final LeaderElection leaderElection;
  private final JobQueue jobQueue;

  public VehicleReferenceCacheRefresher(
    VehicleRefRepository vehicleRefRepository,
    LeaderElection leaderElection,
    JobQueue jobQueue
  ) {
    this.vehicleRefRepository = vehicleRefRepository;
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
        "Failed to prime the vehicle reference cache on taking over as leader",
        e
      );
    }
  }

  private void prime() {
    if (vehicleRefRepository.isEmpty()) {
      LOGGER.info("Vehicle reference cache is empty, priming cache");
      vehicleRefRepository.refreshCache();
    } else {
      LOGGER.info("Existing vehicle reference cache found");
    }
  }

  @Scheduled(
    fixedDelayString = "${antu.vehicle.refresh.millis:1800000}",
    initialDelayString = "${antu.vehicle.refresh.millis:1800000}"
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
    LOGGER.info("Scheduling vehicle reference cache refresh job");
    jobQueue.submit(new AntuJob.RefreshVehicleReferenceCache());
  }

  public void refresh() {
    LOGGER.info("Refreshing vehicle reference cache");
    try {
      vehicleRefRepository.refreshCache();
      LOGGER.info("Refreshed vehicle reference cache");
    } catch (Exception e) {
      LOGGER.error("Failed to refresh vehicle reference cache", e);
    }
  }
}
