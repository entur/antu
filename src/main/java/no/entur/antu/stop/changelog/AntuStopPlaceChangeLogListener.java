package no.entur.antu.stop.changelog;

import java.io.IOException;
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

  /**
   * The payload is closed unread and deactivation handled exactly like deletion. Tiamat is asked for
   * the current version only (allVersions=false), and asked after termination, so it returns neither
   * the stop place nor its quays — antu's cache is their only record. Sourcing them from the payload
   * orphans every quay, and the first lookup of its deleted parent fails the whole validation.
   *
   * <p>Closing it releases nothing today — it is a {@code ByteArrayInputStream} — but keeps the one
   * path that never reads its payload from leaking if that ever changes.
   */
  @Override
  public void onStopPlaceDeactivated(String id, InputStream stopPlaceStream) {
    try (stopPlaceStream) {
      deleteStopPlaceAndQuays(new StopPlaceId(id));
    } catch (IOException e) {
      // The delete is done; failing the event here would only redeliver it.
      log.warn("Failed to close the deactivation payload for {}", id, e);
    }
    log.info("Stop place deactivated: {}", id);
  }

  @Override
  public void onStopPlaceDeleted(String id) {
    deleteStopPlaceAndQuays(new StopPlaceId(id));
  }

  private void deleteStopPlaceAndQuays(StopPlaceId stopPlaceId) {
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
