package no.entur.antu.pipeline;

import no.entur.antu.job.AntuJob;
import no.entur.antu.job.JobQueue;
import no.entur.antu.leader.LeaderElection;
import no.entur.antu.leader.LeadershipGrantedEvent;
import no.entur.antu.leader.LeadershipLostEvent;
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

  /**
   * The changelog consumer is the leader's, so it goes down with the leadership. Nothing else here needs
   * standing down: the caches are shared, and the next leader picks them up as they are.
   */
  @EventListener
  public void onLeadershipLost(LeadershipLostEvent event) {
    try {
      stopPlaceRepositoryUpdater.standDown();
    } catch (Exception e) {
      LOGGER.error(
        "System error: failed to stop the stop place changelog consumer after losing leadership. It is " +
        "consuming alongside the new leader's.",
        e
      );
    }
  }

  /**
   * Queued rather than parsed here: this runs on the thread delivering the leadership event. Queued before
   * the updater is initialised, so a changelog consumer that cannot start does not cost the cache its only
   * chance to be filled.
   *
   * <p>An empty cache is refreshed whatever the version says, so a cache lost to a flushed or evicted
   * Redis comes back without waiting for the next export. Only here, not on every check: this runs once
   * per leadership grant, and a check that ignored the version would queue a parse every tick until the
   * refresh it queued had finished.
   */
  private void prime() {
    if (stopPlaceRepository.isEmpty()) {
      LOGGER.info("Stop place cache is empty, queueing a refresh");
      enqueueRefresh();
    } else {
      queueRefreshIfStale();
    }
    stopPlaceRepositoryUpdater.init();
  }

  /**
   * The check is one blob metadata read, so it runs often rather than at an hour that has to match the
   * export's.
   */
  @Scheduled(cron = "${antu.stop.refresh.cron:-}")
  void refreshIfDatasetChanged() {
    if (!leaderElection.isLeader()) {
      return;
    }
    queueRefreshIfStale();
  }

  /**
   * A bucket with no dataset at all is left alone, which is an environment nobody has seeded.
   */
  private void queueRefreshIfStale() {
    String version = stopPlacesDatasetLoader.loadDatasetVersion();
    if (version == null || version.equals(datasetVersionRepository.get())) {
      return;
    }
    LOGGER.info("New stop place dataset {} found in the bucket", version);
    enqueueRefresh();
  }

  /**
   * Put the refresh on the job queue. The version is recorded here rather than after the load, so the
   * checks that fire while the job is queued or running do not queue the same dataset again. Also
   * reachable through the cache admin API.
   *
   * <p>Submitted before the version is recorded, not after. Recording first means a pod that dies between
   * the Redis write and PubSub confirming the message leaves the export recorded with no job to load it,
   * and every later check then sees it as loaded: no refresh until the next export lands. This order
   * fails the other way instead, and the other way is bounded - a job that ran without its version being
   * recorded is queued once more at the next check.
   */
  public void enqueueRefresh() {
    LOGGER.info("Scheduling stop place cache refresh job");
    String version = stopPlacesDatasetLoader.loadDatasetVersion();
    jobQueue.submit(new AntuJob.RefreshStopPlaceCache());
    datasetVersionRepository.set(version);
  }

  /**
   * A failure is contained rather than thrown. Letting it out nacks the job, and a dataset that cannot be
   * loaded at all would then redeliver against the pod's one job consumer, each attempt downloading and
   * parsing the whole export. Clearing the recorded version is the retry instead: the next check sees the
   * dataset as new again, so a refresh is retried once per check interval rather than as fast as PubSub
   * can redeliver. The version is read here rather than carried in the job, so what is cleared is
   * whatever is recorded when this job starts. A newer export recorded before that read is cleared with
   * it and parsed again at the next check: one redundant parse, not a lost export, and it needs an export
   * to land in the window between queueing this refresh and starting it.
   */
  public void refresh() {
    LOGGER.info("Refreshing stop place cache");
    String version = datasetVersionRepository.get();
    try {
      stopPlaceRepositoryUpdater.createOrUpdate();
      LOGGER.info("Refreshed stop place cache");
    } catch (Exception e) {
      datasetVersionRepository.clearIfStill(version);
      LOGGER.error(
        "System error: could not refresh the stop place cache. Validations will report stop places " +
        "registered since the last import as unresolved references until it succeeds.",
        e
      );
    }
  }
}
