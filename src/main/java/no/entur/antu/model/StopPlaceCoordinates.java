package no.entur.antu.model;

import no.entur.antu.exception.AntuException;
import org.rutebanken.netex.model.LocationStructure;
import org.rutebanken.netex.model.StopPlace;

public record StopPlaceCoordinates(double longitude, double latitude) {
  public static StopPlaceCoordinates of(StopPlace stopPlace) {
    if (stopPlace != null && stopPlace.getCentroid() != null) {
      LocationStructure location = stopPlace.getCentroid().getLocation();
      return new StopPlaceCoordinates(
        location.getLongitude().doubleValue(),
        location.getLatitude().doubleValue()
      );
    }
    return null;
  }

  /*
   * Used to encode data to store in redis.
   * Caution: Changes in this method can effect data stored in redis.
   */
  @Override
  public String toString() {
    return longitude + "§" + latitude;
  }

  /*
   * Used to decode data stored in redis.
   * Caution: Changes in this method can effect data stored in redis.
   */
  public static StopPlaceCoordinates fromString(String stopPlaceCoordinates) {
    if (stopPlaceCoordinates != null) {
      String[] split = stopPlaceCoordinates.split("§");
      if (split.length == 2) {
        return new StopPlaceCoordinates(
          Double.parseDouble(split[0]),
          Double.parseDouble(split[1])
        );
      } else {
        throw new AntuException(
          "Invalid stopPlaceCoordinates string: " + stopPlaceCoordinates
        );
      }
    }
    return null;
  }
}
