package no.entur.antu.model;

import org.rutebanken.netex.model.LocationStructure;
import org.rutebanken.netex.model.StopPlace;

// TODO: implement the redis codec for this
public record StopPlaceCoordinates(
        double longitude,
        double latitude) {

    public static StopPlaceCoordinates of(StopPlace stopPlace) {
        if (stopPlace != null && stopPlace.getCentroid() != null) {
            LocationStructure location = stopPlace.getCentroid().getLocation();
            return new StopPlaceCoordinates(location.getLongitude().doubleValue(), location.getLatitude().doubleValue());
        }
        return null;
    }
}
