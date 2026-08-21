package no.entur.antu.stop.changelog;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;
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

  /**
   * Held while the consumer is deliberately stopped, which is most of the time a refresh takes. Both the
   * refresh job and the watchdog can run on this pod at the same time, and starting the consumer while
   * the refresh has the listener unregistered would drop every change it read until the next restart.
   */
  private final ReentrantLock consumerLifecycle = new ReentrantLock();

  public ChangelogStopPlaceRepositoryUpdater(
    StopPlaceRepositoryLoader stopPlaceRepositoryLoader,
    AntuPublicationTimeRecordFilterStrategy antuPublicationTimeRecordFilterStrategy,
    RedisChangelogUpdateTimestampRepository changelogUpdateTimestampRepository,
    ChangelogConsumerController changelogConsumerController,
    StopPlaceChangelog stopPlaceChangelog,
    AntuStopPlaceChangeLogListener handler
  ) {
    this.stopPlaceRepositoryLoader = stopPlaceRepositoryLoader;
    this.antuPublicationTimeRecordFilterStrategy =
      antuPublicationTimeRecordFilterStrategy;
    this.changelogUpdateTimestampRepository =
      changelogUpdateTimestampRepository;
    this.changelogConsumerController = changelogConsumerController;
    this.stopPlaceChangelog = stopPlaceChangelog;
    this.handler = handler;
  }

  @Override
  public void init() {
    LOGGER.info("Initializing Changelog Stop Place Repository Updater");
    Instant timestamp = changelogUpdateTimestampRepository.getTimestamp();
    if (timestamp == null) {
      timestamp = Instant.now().minus(Duration.ofDays(1));
    }
    consumerLifecycle.lock();
    try {
      antuPublicationTimeRecordFilterStrategy.setPublicationTime(timestamp);
      stopPlaceChangelog.registerStopPlaceChangelogListener(handler);
      changelogConsumerController.start();
    } finally {
      consumerLifecycle.unlock();
    }
    LOGGER.info(
      "Changelog Stop Place Repository Updater initialized with publication timestamp {}",
      timestamp
    );
  }

  /**
   * A stopped consumer is silent: the cache keeps answering from the last export while every change
   * published since then is missed, which reads as a bad dataset rather than as a broken antu. The
   * consumer resets to the earliest retained offset on start and the publication time filter drops
   * what the export already covers, so restarting it replays the gap instead of skipping it.
   *
   * <p>Does nothing while a refresh on this pod holds the lifecycle: that stop is deliberate, and it
   * starts the consumer again itself.
   */
  @Override
  public void ensureRunning() {
    if (!consumerLifecycle.tryLock()) {
      return;
    }
    try {
      if (changelogConsumerController.isRunning()) {
        return;
      }
      LOGGER.error(
        "System error: the stop place changelog consumer is not running, restarting it. Stop places " +
        "changed since the last dataset import were not applied to the cache."
      );
      changelogConsumerController.start();
    } finally {
      consumerLifecycle.unlock();
    }
  }

  @Override
  public void createOrUpdate() {
    consumerLifecycle.lock();
    try {
      changelogConsumerController.stop();
      stopPlaceChangelog.unregisterStopPlaceChangelogListener(handler);
      Instant publicationTime = stopPlaceRepositoryLoader.refreshCache();
      changelogUpdateTimestampRepository.setTimestamp(publicationTime);
      antuPublicationTimeRecordFilterStrategy.setPublicationTime(
        publicationTime
      );
      stopPlaceChangelog.registerStopPlaceChangelogListener(handler);
      changelogConsumerController.start();
    } finally {
      consumerLifecycle.unlock();
    }
  }
}
