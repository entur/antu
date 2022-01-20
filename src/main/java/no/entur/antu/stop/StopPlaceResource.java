package no.entur.antu.stop;

import java.util.Set;

/**
 * A resource to query the National Stop Place Register.
 */
public interface StopPlaceResource {

    /**
     * Returns all quay ids, present and future.
     * @return all quay ids, present and future.
     */
    Set<String> getQuayIds();

    /**
     * Returns all stop place ids, present and future.
     * @return all stop place ids, present and future.
     */
    Set<String> getStopPlaceIds();
}
