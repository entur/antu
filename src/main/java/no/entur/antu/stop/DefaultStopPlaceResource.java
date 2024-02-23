package no.entur.antu.stop;

import jakarta.xml.bind.JAXBElement;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import no.entur.antu.exception.AntuException;
import no.entur.antu.model.QuayId;
import no.entur.antu.model.StopPlaceCoordinates;
import no.entur.antu.model.TransportModes;
import no.entur.antu.model.TransportSubMode;
import no.entur.antu.stop.loader.StopPlacesDatasetLoader;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.rutebanken.netex.model.Quay;
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
  public Set<String> getQuayIds() {
    return getNetexEntitiesIndex()
      .getQuayIndex()
      .getLatestVersions()
      .stream()
      .map(Quay::getId)
      .collect(Collectors.toSet());
  }

  @Override
  public Set<String> getStopPlaceIds() {
    List<StopPlace> list = getNetexEntitiesIndex()
      .getSiteFrames()
      .stream()
      .flatMap(siteFrame -> siteFrame.getStopPlaces().getStopPlace_().stream())
      .map(JAXBElement::getValue)
      .map(StopPlace.class::cast)
      .toList();

    return list.stream().map(StopPlace::getId).collect(Collectors.toSet());
  }

  @Override
  public Map<QuayId, TransportModes> getTransportModesPerQuayId() {
    return getDataPerQuayId(this::getQuayTransportModesEntries);
  }

  @Override
  public Map<QuayId, StopPlaceCoordinates> getCoordinatesPerQuayId() {
    return getDataPerQuayId(this::getQuayCoordinatesEntries);
  }

  private <D> Map<QuayId, D> getDataPerQuayId(
    Function<StopPlace, List<Map.Entry<QuayId, D>>> dataEntriesFunction
  ) {
    return getNetexEntitiesIndex()
      .getSiteFrames()
      .stream()
      .flatMap(siteFrame -> siteFrame.getStopPlaces().getStopPlace_().stream())
      .map(JAXBElement::getValue)
      .map(StopPlace.class::cast)
      .filter(stopPlace -> Objects.nonNull(stopPlace.getTransportMode()))
      .filter(stopPlace -> Objects.nonNull(stopPlace.getQuays()))
      .map(dataEntriesFunction)
      .flatMap(List::stream)
      .collect(
        Collectors.toMap(
          Map.Entry::getKey,
          Map.Entry::getValue,
          (previous, latest) -> latest
        )
      );
  }

  public List<Map.Entry<QuayId, TransportModes>> getQuayTransportModesEntries(
    StopPlace stopPlace
  ) {
    return makeQuayIdMapEntries(
      stopPlace,
      quayId ->
        Map.entry(
          quayId,
          new TransportModes(
            stopPlace.getTransportMode(),
            TransportSubMode.of(stopPlace).orElse(null)
          )
        )
    );
  }

  public List<Map.Entry<QuayId, StopPlaceCoordinates>> getQuayCoordinatesEntries(
    StopPlace stopPlace
  ) {
    return makeQuayIdMapEntries(
      stopPlace,
      quayId -> {
        StopPlaceCoordinates stopPlaceCoordinates = StopPlaceCoordinates.of(
          stopPlace
        );
        if (stopPlaceCoordinates != null) {
          return Map.entry(quayId, stopPlaceCoordinates);
        }
        return null;
      }
    );
  }

  private <D> List<Map.Entry<QuayId, D>> makeQuayIdMapEntries(
    StopPlace stopPlace,
    Function<QuayId, Map.Entry<QuayId, D>> entryFunction
  ) {
    return stopPlace
      .getQuays()
      .getQuayRefOrQuay()
      .stream()
      .map(JAXBElement::getValue)
      .filter(Quay.class::isInstance)
      .map(Quay.class::cast)
      .map(Quay::getId)
      .map(QuayId::new)
      .map(entryFunction)
      .filter(Objects::nonNull)
      .toList();
  }

  protected NetexEntitiesIndex getNetexEntitiesIndex() {
    if (netexEntitiesIndex == null) {
      throw new AntuException("Stop places dataset not loaded");
    }
    return netexEntitiesIndex;
  }
}
