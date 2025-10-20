package no.entur.antu.stop;

import static no.entur.antu.stop.changelog.support.ChangeLogUtils.parsePublicationTime;

import jakarta.xml.bind.JAXBElement;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import no.entur.antu.stop.loader.StopPlacesDatasetLoader;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.model.QuayCoordinates;
import org.entur.netex.validation.validator.model.QuayId;
import org.entur.netex.validation.validator.model.SimpleQuay;
import org.entur.netex.validation.validator.model.SimpleStopPlace;
import org.entur.netex.validation.validator.model.StopPlaceId;
import org.entur.netex.validation.validator.model.TransportModeAndSubMode;
import org.entur.netex.validation.validator.utils.StopPlaceUtils;
import org.rutebanken.netex.model.StopPlace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultStopPlaceResource implements StopPlaceResource {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    DefaultStopPlaceResource.class
  );

  private final StopPlacesDatasetLoader stopPlacesDatasetLoader;

  private Map<StopPlaceId, SimpleStopPlace> stopPlaces;
  private Map<QuayId, SimpleQuay> quays;
  private boolean parsedDataset;
  private Instant publicationTime;

  public DefaultStopPlaceResource(
    StopPlacesDatasetLoader stopPlacesDatasetLoader
  ) {
    this.stopPlacesDatasetLoader = stopPlacesDatasetLoader;
  }

  @Override
  public synchronized Map<QuayId, SimpleQuay> getQuays() {
    init();
    return quays;
  }

  @Override
  public synchronized Map<StopPlaceId, SimpleStopPlace> getStopPlaces() {
    init();
    return stopPlaces;
  }

  @Override
  public synchronized Instant getPublicationTime() {
    init();
    return publicationTime;
  }

  @Override
  public synchronized void clear() {
    quays = null;
    stopPlaces = null;
    parsedDataset = false;
    publicationTime = null;
  }

  private void init() {
    if (parsedDataset) {
      return;
    }
    NetexEntitiesIndex netexEntitiesIndex =
      stopPlacesDatasetLoader.loadNetexEntitiesIndex();
    quays = initQuays(netexEntitiesIndex);
    stopPlaces = initStopPlaces(netexEntitiesIndex);
    publicationTime = parsePublicationTime(netexEntitiesIndex);
    parsedDataset = true;
    LOGGER.info(
      "Loaded {} stop places and {} quays from NeTEx dataset",
      stopPlaces.size(),
      quays.size()
    );
  }

  private static Map<StopPlaceId, SimpleStopPlace> initStopPlaces(
    NetexEntitiesIndex netexEntitiesIndex
  ) {
    return netexEntitiesIndex
      .getSiteFrames()
      .stream()
      .flatMap(siteFrame -> siteFrame.getStopPlaces().getStopPlace_().stream())
      .map(JAXBElement::getValue)
      .map(StopPlace.class::cast)
      .collect(
        Collectors.toUnmodifiableMap(
          stopPlace -> new StopPlaceId(stopPlace.getId()),
          stopPlace -> {
            Set<String> quayIds = netexEntitiesIndex
              .getQuayIdsByStopPlaceIdIndex()
              .get(stopPlace.getId());
            if (quayIds == null) {
              quayIds = Set.of();
            }
            return new SimpleStopPlace(
              stopPlace.getName().getValue(),
              TransportModeAndSubMode.of(stopPlace),
              StopPlaceUtils.isParentStopPlace(stopPlace),
              quayIds
            );
          }
        )
      );
  }

  private static Map<QuayId, SimpleQuay> initQuays(
    NetexEntitiesIndex netexEntitiesIndex
  ) {
    return netexEntitiesIndex
      .getQuayIndex()
      .getLatestVersions()
      .stream()
      .collect(
        Collectors.toUnmodifiableMap(
          quay -> new QuayId(quay.getId()),
          quay ->
            new SimpleQuay(
              QuayCoordinates.of(quay),
              new StopPlaceId(
                netexEntitiesIndex
                  .getStopPlaceIdByQuayIdIndex()
                  .get(quay.getId())
              )
            )
        )
      );
  }
}
