package no.entur.antu.stop.changelog;

import java.time.Duration;
import java.time.Instant;
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

  /**
   * Synchronized against {@link #createOrUpdate()}, which is only a same-pod guard: the two run on
   * different threads of one pod when a refresh job lands on the pod that is taking over as leader, and
   * starting the consumer while the refresh has its listener unregistered drops every change it reads.
   * Refresh jobs run on an arbitrary pod, so the cross-pod case is not covered by this and needs a
   * distributed lock or a leader-local refresh.
   */
  @Override
  public synchronized void init() {
    LOGGER.info("Initializing Changelog Stop Place Repository Updater");
    Instant timestamp = changelogUpdateTimestampRepository.getTimestamp();
    if (timestamp == null) {
      timestamp = Instant.now().minus(Duration.ofDays(1));
    }
    antuPublicationTimeRecordFilterStrategy.setPublicationTime(timestamp);
    stopPlaceChangelog.registerStopPlaceChangelogListener(handler);
    changelogConsumerController.start();
    LOGGER.info(
      "Changelog Stop Place Repository Updater initialized with publication timestamp {}",
      timestamp
    );
  }

  /**
   * The consumer comes back whether or not the refresh worked: a refresh that throws would otherwise leave
   * it stopped with its listener unregistered until the next refresh or the next leader.
   *
   * <p>A restart failure is not swallowed. Returning normally tells the refresher the cache was rebuilt,
   * which keeps the recorded version and stops the checks from retrying, so a transient Kafka failure
   * would leave the changelog dead until the next export. When the refresh failed too, that exception is
   * the one that propagates and the restart failure rides along as suppressed.
   */
  @Override
  public synchronized void createOrUpdate() {
    RuntimeException refreshFailure = null;
    try {
      changelogConsumerController.stop();
      stopPlaceChangelog.unregisterStopPlaceChangelogListener(handler);
      Instant publicationTime = stopPlaceRepositoryLoader.refreshCache();
      changelogUpdateTimestampRepository.setTimestamp(publicationTime);
      antuPublicationTimeRecordFilterStrategy.setPublicationTime(
        publicationTime
      );
    } catch (RuntimeException e) {
      refreshFailure = e;
    }

    try {
      stopPlaceChangelog.registerStopPlaceChangelogListener(handler);
      changelogConsumerController.start();
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
}
