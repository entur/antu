package no.entur.antu.stop;

import jakarta.xml.bind.JAXBElement;
import java.util.Map;
import java.util.stream.Collectors;
import no.entur.antu.exception.AntuException;
import no.entur.antu.model.*;
import no.entur.antu.stop.loader.StopPlacesDatasetLoader;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.rutebanken.netex.model.StopPlace;

public class DefaultStopPlaceResource implements StopPlaceResource {

  private final StopPlacesDatasetLoader stopPlacesDatasetLoader;

  private NetexEntitiesIndex netexEntitiesIndex;

  public DefaultStopPlaceResource(
    StopPlacesDatasetLoader stopPlacesDatasetLoader
  ) {
    this.stopPlacesDatasetLoader = stopPlacesDatasetLoader;
  }

  public void loadStopPlacesDataset() {
    netexEntitiesIndex = stopPlacesDatasetLoader.loadNetexEntitiesIndex();
  }

  @Override
  public Map<QuayId, SimpleQuay> getQuays() {
    return getNetexEntitiesIndex()
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
                getNetexEntitiesIndex()
                  .getStopPlaceIdByQuayIdIndex()
                  .get(quay.getId())
              )
            )
        )
      );
  }

  @Override
  public Map<StopPlaceId, SimpleStopPlace> getStopPlaces() {
    return getNetexEntitiesIndex()
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

  protected NetexEntitiesIndex getNetexEntitiesIndex() {
    if (netexEntitiesIndex == null) {
      throw new AntuException("Stop places dataset not loaded");
    }
    return netexEntitiesIndex;
  }
}
