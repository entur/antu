package no.entur.antu.pipeline;

import no.entur.antu.job.AntuJob;
import no.entur.antu.job.JobQueue;
import no.entur.antu.leader.LeaderElection;
import no.entur.antu.leader.LeadershipGrantedEvent;
import no.entur.antu.stop.StopPlaceRepositoryLoader;
import no.entur.antu.stop.changelog.StopPlaceRepositoryUpdater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Keeps the stop place cache current.
 *
 * <p>The refresh itself is expensive, it parses the national stop dataset, so it is done by whichever
 * pod picks the job off the queue rather than by the pod that decided it was time. Only the leader
 * decides, otherwise every pod would enqueue the same refresh.
 */
@Component
public class StopPlaceCacheRefresher {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    StopPlaceCacheRefresher.class
  );

  private final StopPlaceRepositoryLoader stopPlaceRepository;
  private final StopPlaceRepositoryUpdater stopPlaceRepositoryUpdater;
  private final LeaderElection leaderElection;
  private final JobQueue jobQueue;

  public StopPlaceCacheRefresher(
    StopPlaceRepositoryLoader stopPlaceRepository,
    StopPlaceRepositoryUpdater stopPlaceRepositoryUpdater,
    LeaderElection leaderElection,
    JobQueue jobQueue
  ) {
    this.stopPlaceRepository = stopPlaceRepository;
    this.stopPlaceRepositoryUpdater = stopPlaceRepositoryUpdater;
    this.leaderElection = leaderElection;
    this.jobQueue = jobQueue;
  }

  /**
   * Fill the cache if it is cold, otherwise pick up where the previous leader left off. Starting the
   * stop place changelog consumer is part of this, which is why it belongs to the leader.
   */
  @EventListener
  public void onLeadershipGranted(LeadershipGrantedEvent event) {
    // Contained here: the event is multicast to both cache primers, and an exception escaping one
    // aborts the multicast, so the other would never run and the event is published only once.
    try {
      prime();
    } catch (Exception e) {
      // An empty stop place cache is not a degraded mode, it is a wrong answer: every reference to a stop
      // place comes back unresolved and good datasets are reported as failed. Worded to match the
      // log-based metric that alerts on "System error", because the only automatic recovery is the next
      // scheduled refresh, which is hours away. Deliberately not retried through the job queue: a
      // permanent failure would then redeliver until the subscription's retention runs out, and there is
      // no dead-letter topic to catch it.
      LOGGER.error(
        "System error: failed to prime the stop place cache on taking over as leader. Validations will " +
        "report unresolved stop place references until it is refreshed.",
        e
      );
    }
  }

  private void prime() {
    if (stopPlaceRepository.isEmpty()) {
      LOGGER.info("Stop place cache is empty, priming cache");
      stopPlaceRepositoryUpdater.createOrUpdate();
    } else {
      LOGGER.info(
        "Existing stop place cache found: keep cache and initialize the stop place updater"
      );
      stopPlaceRepositoryUpdater.init();
    }
  }

  @Scheduled(cron = "${antu.stop.refresh.cron:-}")
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
    LOGGER.info("Scheduling stop place cache refresh job");
    jobQueue.submit(new AntuJob.RefreshStopPlaceCache());
  }

  public void refresh() {
    LOGGER.info("Refreshing stop place cache");
    stopPlaceRepositoryUpdater.createOrUpdate();
    LOGGER.info("Refreshed stop place cache");
  }
}
