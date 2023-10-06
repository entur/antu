package no.entur.antu.stop;

import no.entur.antu.exception.AntuException;
import no.entur.antu.stop.loader.StopPlacesDatasetLoader;
import no.entur.antu.stop.model.QuayId;
import no.entur.antu.stop.model.TransportModes;
import no.entur.antu.stop.model.TransportSubMode;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.rutebanken.netex.model.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class StopPlaceResourceImpl implements StopPlaceResource {

    private final StopPlacesDatasetLoader stopPlacesDatasetLoader;

    private NetexEntitiesIndex netexEntitiesIndex;

    public StopPlaceResourceImpl(StopPlacesDatasetLoader stopPlacesDatasetLoader) {
        this.stopPlacesDatasetLoader = stopPlacesDatasetLoader;
    }

    public void loadStopPlacesDataset() {
        netexEntitiesIndex = stopPlacesDatasetLoader.loadNetexEntitiesIndex();
    }

    @Override
    public Set<String> getQuayIds() {
        return getNetexEntitiesIndex().getQuayIndex().getLatestVersions().stream()
                .map(Quay::getId)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> getStopPlaceIds() {
        List<StopPlace> list = getNetexEntitiesIndex().getSiteFrames().stream()
                .flatMap(siteFrame -> siteFrame.getStopPlaces().getStopPlace().stream())
                .toList();

        return list.stream()
                .map(StopPlace::getId)
                .collect(Collectors.toSet());
    }

    @Override
    public Map<QuayId, TransportModes> getTransportModesPerQuayId() {
        return getNetexEntitiesIndex().getSiteFrames().stream()
                .flatMap(siteFrame -> siteFrame.getStopPlaces().getStopPlace().stream())
                .filter(stopPlace -> Objects.nonNull(stopPlace.getTransportMode()))
                .filter(stopPlace -> Objects.nonNull(stopPlace.getQuays()))
                .map(this::getQuayTransportModesEntries)
                .flatMap(List::stream)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (previous, latest) -> latest
                ));
    }

    public List<Map.Entry<QuayId, TransportModes>> getQuayTransportModesEntries(StopPlace stopPlace) {
        return stopPlace.getQuays().getQuayRefOrQuay().stream()
                .filter(Quay.class::isInstance)
                .map(Quay.class::cast)
                .map(Quay::getId)
                .map(QuayId::new)
                .map(quayId -> Map.entry(
                        quayId,
                        new TransportModes(
                                stopPlace.getTransportMode(),
                                TransportSubMode.from(stopPlace).orElse(null)
                        )))
                .toList();
    }

    protected NetexEntitiesIndex getNetexEntitiesIndex() {
        if (netexEntitiesIndex == null) {
            throw new AntuException("Stop places dataset not loaded");
        }
        return netexEntitiesIndex;
    }
}
