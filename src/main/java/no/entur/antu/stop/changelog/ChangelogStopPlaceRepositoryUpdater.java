package no.entur.antu.stop.changelog;

import java.time.Duration;
import java.time.Instant;
import no.entur.antu.stop.StopPlaceRepositoryLoader;
import org.rutebanken.helper.stopplace.changelog.StopPlaceChangelog;
import org.rutebanken.helper.stopplace.changelog.kafka.ChangelogConsumerController;

/**
 * Create or update the stop repository from a NeTEx archive and maintain it up to date
 * by applying real-time updates published through the stop place changelog.
 */
public class ChangelogStopPlaceRepositoryUpdater
  implements StopPlaceRepositoryUpdater {

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

  @Override
  public void init() {
    Instant timestamp = changelogUpdateTimestampRepository.getTimestamp();
    if (timestamp == null) {
      timestamp = Instant.now().minus(Duration.ofDays(1));
    }
    antuPublicationTimeRecordFilterStrategy.setPublicationTime(timestamp);
    stopPlaceChangelog.registerStopPlaceChangelogListener(handler);
    changelogConsumerController.start();
  }

  @Override
  public void createOrUpdate() {
    changelogConsumerController.stop();
    stopPlaceChangelog.unregisterStopPlaceChangelogListener(handler);
    Instant publicationTime = stopPlaceRepositoryLoader.refreshCache();
    changelogUpdateTimestampRepository.setTimestamp(publicationTime);
    antuPublicationTimeRecordFilterStrategy.setPublicationTime(publicationTime);
    stopPlaceChangelog.registerStopPlaceChangelogListener(handler);
    changelogConsumerController.start();
  }
}
