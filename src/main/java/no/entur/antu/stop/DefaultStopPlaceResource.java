package no.entur.antu.stop;

import jakarta.xml.bind.JAXBElement;
import java.util.Map;
import java.util.stream.Collectors;
import no.entur.antu.model.*;
import no.entur.antu.stop.loader.StopPlacesDatasetLoader;
import org.entur.netex.index.api.NetexEntitiesIndex;
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
  public synchronized void clear() {
    quays = null;
    stopPlaces = null;
    parsedDataset = false;
  }

  private void init() {
    if (parsedDataset) {
      return;
    }
    NetexEntitiesIndex netexEntitiesIndex =
      stopPlacesDatasetLoader.loadNetexEntitiesIndex();
    quays = initQuays(netexEntitiesIndex);
    stopPlaces = initStopPlaces(netexEntitiesIndex);
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
          stopPlace ->
            new SimpleStopPlace(
              stopPlace.getName().getValue(),
              TransportModeAndSubMode.of(stopPlace)
            )
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
