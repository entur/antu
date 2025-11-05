package no.entur.antu.stop.changelog;

import java.io.InputStream;
import java.util.Set;
import java.util.stream.Collectors;
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
import org.rutebanken.netex.model.EntityStructure;
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
  public void onStopPlaceDeactivated(String id, InputStream stopPlaceStream) {
    deactivateStopPlace(new StopPlaceId(id), stopPlaceStream);
    log.info("Stop place deactivated: {}", id);
  }

  private void deactivateStopPlace(
    StopPlaceId stopPlaceId,
    InputStream stopPlaceStream
  ) {
    NetexParser parser = new NetexParser();
    NetexEntitiesIndex netexEntitiesIndex = parser.parse(stopPlaceStream);
    Set<String> quaysToDelete = getQuaysIdsFromIndex(netexEntitiesIndex);
    deleteStopPlace(stopPlaceId);
    for (String quayId : quaysToDelete) {
      deleteQuay(quayId, stopPlaceId);
    }
  }

  private Set<String> getQuaysIdsFromIndex(
    NetexEntitiesIndex netexEntitiesIndex
  ) {
    return netexEntitiesIndex
      .getQuayIndex()
      .getLatestVersions()
      .stream()
      .map(EntityStructure::getId)
      .collect(Collectors.toSet());
  }

  @Override
  public void onStopPlaceDeleted(String id) {
    StopPlaceId stopPlaceId = new StopPlaceId(id);
    Set<String> quaysToDelete =
      stopPlaceRepositoryLoader.getQuaysForStopPlaceId(stopPlaceId);
    deleteStopPlace(stopPlaceId);
    for (String quayId : quaysToDelete) {
      deleteQuay(quayId, stopPlaceId);
    }
  }

  private void deleteStopPlace(StopPlaceId stopPlaceId) {
    stopPlaceRepositoryLoader.deleteStopPlace(stopPlaceId);
    log.info("Deleted stop place with ID {}", stopPlaceId.id());
  }

  private void deleteQuay(String quayIdStr, StopPlaceId stopPlaceId) {
    QuayId quayId = new QuayId(quayIdStr);
    stopPlaceRepositoryLoader.deleteQuay(quayId);
    log.info(
      "Deleted quay with ID {} associated with deleted stop place ID {}",
      quayIdStr,
      stopPlaceId.id()
    );
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
