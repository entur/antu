package no.entur.antu.stop;

import no.entur.antu.exception.AntuException;
import no.entur.antu.stop.loader.StopPlacesDatasetLoader;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.StopPlace;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CurrentStopPlaceResource implements StopPlaceResource {

    private final StopPlacesDatasetLoader stopPlacesDatasetLoader;

    private NetexEntitiesIndex netexEntitiesIndex;

    public CurrentStopPlaceResource(StopPlacesDatasetLoader stopPlacesDatasetLoader) {
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

    protected NetexEntitiesIndex getNetexEntitiesIndex() {
        if (netexEntitiesIndex == null) {
            throw new AntuException("Stop places dataset not loaded");
        }
        return netexEntitiesIndex;
    }
}
