package no.entur.antu.stop.changelog;

import java.time.Duration;
import java.time.Instant;
import no.entur.antu.leader.LeaderElection;
import no.entur.antu.stop.StopPlaceRepositoryLoader;
import org.rutebanken.helper.stopplace.changelog.StopPlaceChangelog;
import org.rutebanken.helper.stopplace.changelog.kafka.ChangelogConsumerController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Create or update the stop repository from a NeTEx archive and maintain it up to date
 * by applying real-time updates published through the stop place changelog.
 */
public class ChangelogStopPlaceRepositoryUpdater
  implements StopPlaceRepositoryUpdater {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    ChangelogStopPlaceRepositoryUpdater.class
  );

  private final StopPlaceRepositoryLoader stopPlaceRepositoryLoader;
  private final AntuPublicationTimeRecordFilterStrategy antuPublicationTimeRecordFilterStrategy;
  private final RedisChangelogUpdateTimestampRepository changelogUpdateTimestampRepository;
  private final ChangelogConsumerController changelogConsumerController;
  private final StopPlaceChangelog stopPlaceChangelog;
  private final AntuStopPlaceChangeLogListener handler;
  private final LeaderElection leaderElection;

  public ChangelogStopPlaceRepositoryUpdater(
    StopPlaceRepositoryLoader stopPlaceRepositoryLoader,
    AntuPublicationTimeRecordFilterStrategy antuPublicationTimeRecordFilterStrategy,
    RedisChangelogUpdateTimestampRepository changelogUpdateTimestampRepository,
    ChangelogConsumerController changelogConsumerController,
    StopPlaceChangelog stopPlaceChangelog,
    AntuStopPlaceChangeLogListener handler,
    LeaderElection leaderElection
  ) {
    this.stopPlaceRepositoryLoader = stopPlaceRepositoryLoader;
    this.antuPublicationTimeRecordFilterStrategy =
      antuPublicationTimeRecordFilterStrategy;
    this.changelogUpdateTimestampRepository =
      changelogUpdateTimestampRepository;
    this.changelogConsumerController = changelogConsumerController;
    this.stopPlaceChangelog = stopPlaceChangelog;
    this.handler = handler;
    this.leaderElection = leaderElection;
  }

  /**
   * Synchronized against {@link #createOrUpdate()}: the two run on different threads when a refresh job
   * lands on the pod that is taking over as leader, and starting the consumer while the refresh has its
   * listener unregistered drops every change it reads. Only the leader touches the consumer, so both are
   * always on the same pod and the monitor is enough.
   */
  @Override
  public synchronized void init() {
    LOGGER.info("Initializing Changelog Stop Place Repository Updater");
    Instant timestamp = changelogUpdateTimestampRepository.getTimestamp();
    if (timestamp == null) {
      timestamp = Instant.now().minus(Duration.ofDays(1));
    }
    antuPublicationTimeRecordFilterStrategy.setPublicationTime(timestamp);
    startConsumerIfLeader();
    LOGGER.info(
      "Changelog Stop Place Repository Updater initialized with publication timestamp {}",
      timestamp
    );
  }

  /**
   * Synchronized like {@link #init()}: this arrives on the event thread while a refresh may be running,
   * and stopping the consumer between the refresh's restart check and the start itself would be undone.
   */
  @Override
  public synchronized void standDown() {
    LOGGER.info("No longer leader, stopping the stop place changelog consumer");
    // Unregistered before the consumer is stopped, and stopped even if that throws: it is the listener,
    // not the consumer, that puts records into the shared cache, so a Kafka shutdown that fails leaves a
    // consumer reading records nobody applies rather than one still writing as the new leader takes over.
    try {
      stopPlaceChangelog.unregisterStopPlaceChangelogListener(handler);
    } finally {
      changelogConsumerController.stop();
    }
  }

  /**
   * The cache is rebuilt on whichever pod picks the refresh job off the queue, but the changelog consumer
   * belongs to the leader: init() is the only thing that starts one, and that runs on taking over as
   * leader. A pod without a consumer must not come out of a refresh with one - nothing would stop it
   * again, and it would replay the whole retained topic on top of the leader's.
   */
  @Override
  public synchronized void createOrUpdate() {
    if (leaderElection.isLeader()) {
      refreshWithConsumerStopped();
    } else {
      refreshCache();
    }
  }

  /**
   * The consumer is stopped while the dataset is parsed, so that changes it reads are not overwritten by
   * the older export, and comes back whether or not the refresh worked.
   *
   * <p>A restart that fails is not swallowed. Returning normally tells the refresher the cache was
   * rebuilt, which keeps the recorded dataset version and stops the checks from retrying, so a transient
   * Kafka failure would leave the changelog dead until the next export. When the refresh failed too, that
   * exception is the one that carries and the restart failure rides along as suppressed.
   */
  private void refreshWithConsumerStopped() {
    RuntimeException refreshFailure = null;
    try {
      stopPlaceChangelog.unregisterStopPlaceChangelogListener(handler);
      changelogConsumerController.stop();
      refreshCache();
    } catch (RuntimeException e) {
      refreshFailure = e;
    }

    try {
      startConsumerIfLeader();
    } catch (RuntimeException e) {
      if (refreshFailure == null) {
        throw e;
      }
      refreshFailure.addSuppressed(e);
    }

    if (refreshFailure != null) {
      throw refreshFailure;
    }
  }

  /**
   * The only place the consumer is started, and it checks the lease itself rather than trusting the caller.
   * A refresh takes long enough to lose the lease while it runs, and taking over and standing down are
   * delivered on separate threads, so neither caller can promise leadership still holds by the time this
   * runs. Whichever of this and {@link #standDown()} runs last decides, and both agree once the lease has
   * moved.
   */
  private void startConsumerIfLeader() {
    if (!leaderElection.isLeader()) {
      LOGGER.info(
        "Not the leader any more, leaving the stop place changelog consumer stopped"
      );
      return;
    }
    stopPlaceChangelog.registerStopPlaceChangelogListener(handler);
    changelogConsumerController.start();
  }

  private void refreshCache() {
    Instant publicationTime = stopPlaceRepositoryLoader.refreshCache();
    changelogUpdateTimestampRepository.setTimestamp(publicationTime);
    antuPublicationTimeRecordFilterStrategy.setPublicationTime(publicationTime);
  }
}
