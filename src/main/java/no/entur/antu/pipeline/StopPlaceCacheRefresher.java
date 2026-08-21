package no.entur.antu.pipeline;

import no.entur.antu.job.AntuJob;
import no.entur.antu.job.JobQueue;
import no.entur.antu.leader.LeaderElection;
import no.entur.antu.leader.LeadershipGrantedEvent;
import no.entur.antu.stop.StopPlaceDatasetVersionRepository;
import no.entur.antu.stop.StopPlaceRepositoryLoader;
import no.entur.antu.stop.changelog.StopPlaceRepositoryUpdater;
import no.entur.antu.stop.loader.StopPlacesDatasetLoader;
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
  private final StopPlacesDatasetLoader stopPlacesDatasetLoader;
  private final StopPlaceDatasetVersionRepository datasetVersionRepository;
  private final LeaderElection leaderElection;
  private final JobQueue jobQueue;

  public StopPlaceCacheRefresher(
    StopPlaceRepositoryLoader stopPlaceRepository,
    StopPlaceRepositoryUpdater stopPlaceRepositoryUpdater,
    StopPlacesDatasetLoader stopPlacesDatasetLoader,
    StopPlaceDatasetVersionRepository datasetVersionRepository,
    LeaderElection leaderElection,
    JobQueue jobQueue
  ) {
    this.stopPlaceRepository = stopPlaceRepository;
    this.stopPlaceRepositoryUpdater = stopPlaceRepositoryUpdater;
    this.stopPlacesDatasetLoader = stopPlacesDatasetLoader;
    this.datasetVersionRepository = datasetVersionRepository;
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
      refreshFromDataset();
    } else {
      LOGGER.info(
        "Existing stop place cache found: keep cache and initialize the stop place updater"
      );
      stopPlaceRepositoryUpdater.init();
    }
  }

  /**
   * A second, slower pass at the same check as the watch, kept because it is the schedule operators know
   * and because it still runs if the watch cron is ever misconfigured. It does not refresh
   * unconditionally: with the watch picking up each export within minutes, that only ever parsed a
   * dataset the cache was already built from, twice a day.
   */
  @Scheduled(cron = "${antu.stop.refresh.cron:-}")
  void refreshIfLeader() {
    if (!leaderElection.isLeader()) {
      return;
    }
    refreshIfDatasetChanged();
  }

  /**
   * Keep the two sources of stop place data alive between the scheduled refreshes: the NeTEx export,
   * which Tiamat writes at 02:00 and 12:00 and the cron misses by about an hour each time, and the
   * changelog consumer, which leaves antu blind to everything published since the last export if it is
   * not running.
   */
  @Scheduled(cron = "${antu.stop.watch.cron:-}")
  void watchStopPlaceSources() {
    if (!leaderElection.isLeader()) {
      return;
    }
    try {
      stopPlaceRepositoryUpdater.ensureRunning();
    } catch (Exception e) {
      // Contained: the export is the reliable source of the two, and a broken Kafka consumer must not
      // stop antu from picking up a new export.
      LOGGER.error(
        "System error: could not restart the stop place changelog consumer",
        e
      );
    }
    refreshIfDatasetChanged();
  }

  /**
   * An empty cache is refreshed whatever the version says, so that a cache lost to a flushed or evicted
   * Redis is rebuilt without waiting for the next export.
   */
  private void refreshIfDatasetChanged() {
    String version = stopPlacesDatasetLoader.loadDatasetVersion();
    if (version == null) {
      return;
    }
    if (version.equals(datasetVersionRepository.get())) {
      if (!stopPlaceRepository.isEmpty()) {
        return;
      }
      LOGGER.warn(
        "Stop place cache is empty although it was built from {}, refreshing it",
        version
      );
    } else {
      LOGGER.info("New stop place dataset {} found in the bucket", version);
    }
    enqueueRefresh();
  }

  /**
   * Put the refresh on the job queue, unless one is already on its way. Also reachable through the cache
   * admin API.
   *
   * <p>The claim is what keeps one new export from queueing several parses of it: the version is only
   * written once a refresh has finished, so without it every trigger firing while the job waits its turn
   * behind a night's worth of validation jobs would queue another one, and the daily cron firing in the
   * same minute as the watch would queue two.
   */
  public void enqueueRefresh() {
    if (!datasetVersionRepository.claimRefresh()) {
      LOGGER.info(
        "A stop place cache refresh is already on its way, not scheduling another"
      );
      return;
    }
    boolean submitted = false;
    try {
      LOGGER.info("Scheduling stop place cache refresh job");
      jobQueue.submit(new AntuJob.RefreshStopPlaceCache());
      submitted = true;
    } finally {
      if (!submitted) {
        datasetVersionRepository.releaseRefresh();
      }
    }
  }

  /**
   * Refresh whatever the recorded version says, for the operator who has a reason to think the cache is
   * wrong. Forgetting the version rather than bypassing the check is deliberate: if a refresh is already
   * on its way, this one is dropped, and the next watch tick queues it again.
   */
  public void forceRefresh() {
    LOGGER.info("Forcing a stop place cache refresh");
    datasetVersionRepository.clear();
    enqueueRefresh();
  }

  public void refresh() {
    LOGGER.info("Refreshing stop place cache");
    try {
      refreshFromDataset();
    } finally {
      // Released whether or not the refresh worked. A redelivery of this job does not come through
      // enqueueRefresh, and a claim left behind would keep the next export from being picked up until it
      // expired.
      datasetVersionRepository.releaseRefresh();
    }
    LOGGER.info("Refreshed stop place cache");
  }

  /**
   * The version is read before the refresh, not after: a newer export landing while this one runs
   * would otherwise be recorded as loaded, and skipped.
   */
  private void refreshFromDataset() {
    String version = stopPlacesDatasetLoader.loadDatasetVersion();
    stopPlaceRepositoryUpdater.createOrUpdate();
    datasetVersionRepository.set(version);
  }
}
