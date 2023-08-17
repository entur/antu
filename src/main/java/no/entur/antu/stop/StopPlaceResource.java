package no.entur.antu.stop;

import org.rutebanken.netex.model.VehicleModeEnumeration;

import java.util.Map;
import java.util.Set;

/**
 * A resource to query the National Stop Place Register.
 */
public interface StopPlaceResource {

    /**
     * Returns all quay ids.
     * @return all quay ids.
     */
    Set<String> getQuayIds();

    /**
     * Returns all stop place ids.
     * @return all stop place ids.
     */
    Set<String> getStopPlaceIds();

    /**
     * Returns transport modes per stop place ids.
     * @return map of stop place ids and transport mode.
     */
    Map<String, VehicleModeEnumeration> getTransportModesPerStopPlace();

    /**
     * Returns transport sub modes per stop place ids.
     * @return map of stop place ids and transport sub mode.
     */
    Map<String, String> getTransportSubModesPerStopPlace();

    void loadStopPlacesDataset();
}
