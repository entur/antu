package no.entur.antu.stop;

import no.entur.antu.exception.AntuException;
import no.entur.antu.stop.loader.StopPlacesDatasetLoader;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.rutebanken.netex.model.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

    @Override
    public Map<String, VehicleModeEnumeration> getTransportModesPerStopPlace() {
        List<StopPlace> list = getNetexEntitiesIndex().getSiteFrames().stream()
                .flatMap(siteFrame -> siteFrame.getStopPlaces().getStopPlace().stream())
                .toList();

        return list.stream()
                .filter(stopPlace -> stopPlace.getTransportMode() != null) // TODO: Do we need to check the parent??
                .collect(Collectors.toMap(
                        StopPlace::getId,
                        StopPlace_VersionStructure::getTransportMode
                ));
    }

    public Map<String, String> getTransportSubModesPerStopPlace() {
        List<StopPlace> list = getNetexEntitiesIndex().getSiteFrames().stream()
                .flatMap(siteFrame -> siteFrame.getStopPlaces().getStopPlace().stream())
                .toList();

        return list.stream()
                .filter(stopPlace -> stopPlace.getTransportMode() != null)
                .filter(stopPlace -> findTransportSubMode(stopPlace) != null)
                .collect(Collectors.toMap(
                        StopPlace::getId,
                        this::findTransportSubMode
                ));
        }

    private String findTransportSubMode(StopPlace stopPlace) {
        return switch (stopPlace.getTransportMode()) {
            case AIR -> stopPlace.getAirSubmode() == null ? null : stopPlace.getAirSubmode().value();
            case BUS -> stopPlace.getBusSubmode() == null ? null : stopPlace.getBusSubmode().value();
            case COACH -> stopPlace.getCoachSubmode() == null ? null : stopPlace.getCoachSubmode().value();
            case METRO -> stopPlace.getMetroSubmode() == null ? null : stopPlace.getMetroSubmode().value();
            case RAIL -> stopPlace.getRailSubmode() == null ? null : stopPlace.getRailSubmode().value();
            case TRAM -> stopPlace.getTramSubmode() == null ? null : stopPlace.getTramSubmode().value();
            case WATER -> stopPlace.getWaterSubmode() == null ? null : stopPlace.getWaterSubmode().value();
            case CABLEWAY -> stopPlace.getTelecabinSubmode() == null ? null : stopPlace.getTelecabinSubmode().value();
            case FUNICULAR -> stopPlace.getFunicularSubmode() == null ? null : stopPlace.getFunicularSubmode().value();
            case SNOW_AND_ICE -> stopPlace.getSnowAndIceSubmode() == null ? null : stopPlace.getSnowAndIceSubmode().value();
            default -> null;
        };
    }

    protected NetexEntitiesIndex getNetexEntitiesIndex() {
        if (netexEntitiesIndex == null) {
            throw new AntuException("Stop places dataset not loaded");
        }
        return netexEntitiesIndex;
    }
}
