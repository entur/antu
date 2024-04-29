package no.entur.antu.validation.utilities;

import static java.lang.Math.abs;
import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static java.lang.Math.toRadians;

import no.entur.antu.model.QuayCoordinates;
import org.locationtech.jts.geom.Coordinate;

/**
 * Utility class for calculating distances between geographical points on the Earth's surface.
 * The class provides methods for both accurate spherical distance calculations and faster
 * approximated flat-earth distance calculations.
 * The logic is copied from OTP (OpenTripPlanner).
 */
public final class SphericalDistanceLibrary {

  private SphericalDistanceLibrary() {
    // Utility class
  }

  public static final double RADIUS_OF_EARTH_IN_KM = 6371.01;
  public static final double RADIUS_OF_EARTH_IN_M =
    RADIUS_OF_EARTH_IN_KM * 1000;

  // Max admissible lat/lon delta for approximated distance computation
  public static final double MAX_LAT_DELTA_DEG = 4.0;
  public static final double MAX_LON_DELTA_DEG = 4.0;

  // 1 / Max over-estimation error of approximated distance,
  // for delta lat/lon in given range
  public static final double MAX_ERR_INV = 0.999462;

  public static double distance(QuayCoordinates from, QuayCoordinates to) {
    return distance(
      from.latitude(),
      from.longitude(),
      to.latitude(),
      to.longitude()
    );
  }

  /**
   * Calculates the great-circle distance between two points on a sphere (like the Earth),
   * based on their latitudes and longitudes. This method is more accurate over longer distances
   * compared to simpler flat-earth approximations, as it accounts for the curvature of the sphere.
   * The radius parameter allows the method to be used for any sphere, not just Earth,
   * by providing the appropriate radius.
   *
   * @param from jts coordinate in degrees (lon, lat
   * @param to   jts coordinate in degrees (lon, lat)
   * @return distance in meters
   */
  public static double distance(Coordinate from, Coordinate to) {
    return distance(from.y, from.x, to.y, to.x);
  }

  /**
   * Calculates an approximate distance between two geographical points (specified in latitude and longitude)
   * using a simplified flat-earth model. This method is generally faster to compute than more accurate
   * spherical models, such as the Haversine formula, but can introduce errors, especially over longer distances
   * or near the poles due to the Earth's curvature.
   *
   * @param from jts coordinate in degrees (lon, lat)
   * @param to   jts coordinate in degrees (lon, lat)
   * @return distance in meters
   */
  public static double fastDistance(Coordinate from, Coordinate to) {
    return fastDistance(from.y, from.x, to.y, to.x);
  }

  private static double distance(
    double lat1,
    double lon1,
    double lat2,
    double lon2
  ) {
    return distance(lat1, lon1, lat2, lon2, RADIUS_OF_EARTH_IN_M);
  }

  /**
   * Compute an (approximated) distance between two points, with a known cos(lat). Be careful, this
   * is approximated and never check for the validity of input cos(lat).
   */
  private static double fastDistance(
    double lat1,
    double lon1,
    double lat2,
    double lon2
  ) {
    return fastDistance(lat1, lon1, lat2, lon2, RADIUS_OF_EARTH_IN_M);
  }

  public static void main(String[] args) {
    //    Coordinate from = new Coordinate(9.256717, 61.984575);
    //    Coordinate to = new Coordinate(9.536109, 61.772692);
    Coordinate from = new Coordinate(61.984575, 9.256717);
    Coordinate to = new Coordinate(61.772692, 9.536109);
    System.out.println(distance(from, to));
    System.out.println(fastDistance(from, to));
    //50.69480700671304
    //50.667533200676786
  }

  public static double distance(
    double lat1,
    double lon1,
    double lat2,
    double lon2,
    double radius
  ) {
    // http://en.wikipedia.org/wiki/Great-circle_distance
    lat1 = toRadians(lat1); // Theta-s
    lon1 = toRadians(lon1); // Lambda-s
    lat2 = toRadians(lat2); // Theta-f
    lon2 = toRadians(lon2); // Lambda-f

    double deltaLon = lon2 - lon1;

    double y = sqrt(
      p2(cos(lat2) * sin(deltaLon)) +
      p2(cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(deltaLon))
    );
    double x = sin(lat1) * sin(lat2) + cos(lat1) * cos(lat2) * cos(deltaLon);

    return radius * atan2(y, x);
  }

  /**
   * Approximated, fast and under-estimated equirectangular distance between two points. Works only
   * for small delta lat/lon, fall-back on exact distance if not the case. See:
   * <a href="http://www.movable-type.co.uk/scripts/latlong.html">...</a>
   */
  public static double fastDistance(
    double lat1,
    double lon1,
    double lat2,
    double lon2,
    double radius
  ) {
    if (
      abs(lat1 - lat2) > MAX_LAT_DELTA_DEG ||
      abs(lon1 - lon2) > MAX_LON_DELTA_DEG
    ) {
      return distance(lat1, lon1, lat2, lon2, radius);
    }
    double dLat = toRadians(lat2 - lat1);
    double dLon = toRadians(lon2 - lon1) * cos(toRadians((lat1 + lat2) / 2));
    return radius * sqrt(dLat * dLat + dLon * dLon) * MAX_ERR_INV;
  }

  private static double p2(double a) {
    return a * a;
  }
}
