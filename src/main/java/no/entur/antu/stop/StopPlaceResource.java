package no.entur.antu.stop;

import no.entur.antu.stop.model.QuayId;
import no.entur.antu.stop.model.StopPlaceTransportModes;

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
     * Returns transport modes per quay ids.
     *
     * @return map of quay ids and transport mode.
     */
    Map<QuayId, StopPlaceTransportModes> getTransportModesPerQuayId();

    void loadStopPlacesDataset();
}
