package no.entur.antu.stop.changelog;

import java.io.InputStream;
import java.util.Set;

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
import org.entur.netex.validation.validator.utils.StopPlaceUtils;
import org.rutebanken.helper.stopplace.changelog.StopPlaceChangelogListener;
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
    log.info("Stop place deactivated: {}", id);
    onStopPlaceDeleted(id);
  }

  @Override
  public void onStopPlaceDeleted(String id) {
    StopPlaceId stopPlaceId = new StopPlaceId(id);
    if (stopPlaceIsEligibleForDeletion(stopPlaceId)) {
        log.info("Stop place with id {} is eligible for deletion. Proceeding with deletion", id);
        deleteStopPlace(stopPlaceId);
        Set<String> quaysToDelete = stopPlaceRepositoryLoader.getQuaysForStopPlaceId(stopPlaceId);
        for (String quayId : quaysToDelete) {
            deleteQuay(quayId, stopPlaceId);
        }
    }
  }

  private boolean stopPlaceIsEligibleForDeletion(StopPlaceId stopPlaceId) {
      log.info("Checking whether stop place with ID {} is eligible for deletion", stopPlaceId.id());
      if (!stopPlaceRepositoryLoader.hasStopPlaceId(stopPlaceId)) {
          log.info("Stop place with ID {} is ineligible for deletion because it does not exist in the cache. Ignoring", stopPlaceId.id());
          return false;
      }
      if (stopPlaceRepositoryLoader.isParentStop(stopPlaceId)) {
          log.warn("Deletion of parent stops is unsupported, and stop place with id {} is a parent stop. Ignoring", stopPlaceId.id());
          return false;
      }
      return true;
  }

  private void deleteStopPlace(StopPlaceId stopPlaceId) {
      stopPlaceRepositoryLoader.deleteStopPlace(stopPlaceId);
      log.info("Deleted stop place with ID {}", stopPlaceId.id());
  }

  private void deleteQuay(String quayIdStr, StopPlaceId stopPlaceId) {
      QuayId quayId = new QuayId(quayIdStr);
      stopPlaceRepositoryLoader.deleteQuay(quayId);
      log.info("Deleted quay with ID {} associated with deleted stop place ID {}", quayIdStr, stopPlaceId.id());
  }

  private void createOrUpdateStopPlace(InputStream stopPlaceStream) {
    NetexParser parser = new NetexParser();
    NetexEntitiesIndex netexEntitiesIndex = parser.parse(stopPlaceStream);

    netexEntitiesIndex
      .getStopPlaceIndex()
      .getLatestVersions()
      .forEach(stopPlace -> {
        StopPlaceId stopPlaceId = new StopPlaceId(stopPlace.getId());
        SimpleStopPlace simpleStopPlace = new SimpleStopPlace(
          stopPlace.getName().getValue(),
          TransportModeAndSubMode.of(stopPlace),
          StopPlaceUtils.isParentStopPlace(stopPlace),
          netexEntitiesIndex.getQuayIdsByStopPlaceIdIndex().get(stopPlace.getId())
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
