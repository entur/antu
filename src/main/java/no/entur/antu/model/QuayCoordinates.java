package no.entur.antu.model;

import no.entur.antu.exception.AntuException;
import org.locationtech.jts.geom.Coordinate;
import org.rutebanken.netex.model.LocationStructure;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.StopPlace;

public record QuayCoordinates(double longitude, double latitude) {
  public static QuayCoordinates of(StopPlace stopPlace) {
    if (stopPlace != null && stopPlace.getCentroid() != null) {
      LocationStructure location = stopPlace.getCentroid().getLocation();
      return new QuayCoordinates(
        location.getLongitude().doubleValue(),
        location.getLatitude().doubleValue()
      );
    }
    return null;
  }

  public static QuayCoordinates of(Quay quay) {
    if (quay != null && quay.getCentroid() != null) {
      LocationStructure location = quay.getCentroid().getLocation();
      return new QuayCoordinates(
        location.getLongitude().doubleValue(),
        location.getLatitude().doubleValue()
      );
    }
    return null;
  }

  /** Return Antu domain coordinate as JTS GeoTools Library coordinate. */
  public Coordinate asJtsCoordinate() {
    return new Coordinate(longitude, latitude);
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
  public static QuayCoordinates fromString(String stopPlaceCoordinates) {
    if (stopPlaceCoordinates != null) {
      String[] split = stopPlaceCoordinates.split("§");
      if (split.length == 2) {
        return new QuayCoordinates(
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
