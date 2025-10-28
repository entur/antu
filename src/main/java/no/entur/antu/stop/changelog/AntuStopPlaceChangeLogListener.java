package no.entur.antu.stop.changelog;

import java.io.InputStream;
import no.entur.antu.stop.StopPlaceRepositoryLoader;
import no.entur.antu.stop.changelog.support.ChangeLogUtils;
import org.entur.netex.NetexParser;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.model.QuayCoordinates;
import org.entur.netex.validation.validator.model.QuayId;
import org.entur.netex.validation.validator.model.SimpleQuay;
import org.entur.netex.validation.validator.model.SimpleStopPlace;
import org.entur.netex.validation.validator.model.StopPlaceId;
import org.entur.netex.validation.validator.model.TransportModeAndSubMode;
import org.rutebanken.helper.stopplace.changelog.StopPlaceChangelogListener;
import org.rutebanken.netex.model.MultilingualString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AntuStopPlaceChangeLogListener
  implements StopPlaceChangelogListener {

  private final StopPlaceRepositoryLoader stopPlaceRepositoryLoader;
  private final ChangelogUpdateTimestampRepository changelogUpdateTimestampRepository;

  private static final Logger log = LoggerFactory.getLogger(
    AntuStopPlaceChangeLogListener.class
  );

  public AntuStopPlaceChangeLogListener(
    StopPlaceRepositoryLoader stopPlaceRepositoryLoader,
    ChangelogUpdateTimestampRepository changelogUpdateTimestampRepository
  ) {
    this.stopPlaceRepositoryLoader = stopPlaceRepositoryLoader;
    this.changelogUpdateTimestampRepository =
      changelogUpdateTimestampRepository;
  }

  @Override
  public void onStopPlaceCreated(String id, InputStream stopPlaceStream) {
    log.info("Stop place created: {}", id);
    createOrUpdateStopPlace(stopPlaceStream);
  }

  @Override
  public void onStopPlaceUpdated(String id, InputStream stopPlaceStream) {
    log.info("Stop place updated: {}", id);
    createOrUpdateStopPlace(stopPlaceStream);
  }

  @Override
  public void onStopPlaceDeactivated(String id, InputStream stopPlace) {
    log.info("Stop place deactivated: {}. Ignoring", id);
  }

  @Override
  public void onStopPlaceDeleted(String id) {
    log.info("Stop place deleted: {}. Ignoring", id);
  }

  private void createOrUpdateStopPlace(InputStream stopPlaceStream) {
    NetexParser parser = new NetexParser();
    NetexEntitiesIndex netexEntitiesIndex = parser.parse(stopPlaceStream);

    netexEntitiesIndex
      .getStopPlaceIndex()
      .getLatestVersions()
      .forEach(stopPlace -> {
        StopPlaceId stopPlaceId = new StopPlaceId(stopPlace.getId());
        MultilingualString stopPlaceName = stopPlace.getName();
        SimpleStopPlace simpleStopPlace = new SimpleStopPlace(
          stopPlaceName == null ? "" : stopPlaceName.getValue(),
          TransportModeAndSubMode.of(stopPlace)
        );
        stopPlaceRepositoryLoader.createOrUpdateStopPlace(
          stopPlaceId,
          simpleStopPlace
        );
      });

    netexEntitiesIndex
      .getQuayIndex()
      .getLatestVersions()
      .forEach(quay -> {
        QuayId quayId = new QuayId(quay.getId());
        StopPlaceId stopPlaceId = new StopPlaceId(
          netexEntitiesIndex.getStopPlaceIdByQuayIdIndex().get(quay.getId())
        );
        SimpleQuay simpleQuay = new SimpleQuay(
          QuayCoordinates.of(quay),
          stopPlaceId
        );
        stopPlaceRepositoryLoader.createOrUpdateQuay(quayId, simpleQuay);
      });

    changelogUpdateTimestampRepository.setTimestamp(
      ChangeLogUtils.parsePublicationTime(netexEntitiesIndex)
    );
  }
}
