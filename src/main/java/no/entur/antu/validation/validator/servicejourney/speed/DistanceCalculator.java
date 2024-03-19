package no.entur.antu.validation.validator.servicejourney.speed;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import no.entur.antu.model.QuayCoordinates;
import no.entur.antu.model.ScheduledStopPointId;
import no.entur.antu.stoptime.PassingTimes;
import no.entur.antu.stoptime.StopTime;

public class DistanceCalculator {

  private final Map<String, Double> distances = new HashMap<>();

  public double calculateDistance(
    PassingTimes passingTimes,
    Function<StopTime, QuayCoordinates> getQuayCoordinates
  ) {
    return calculateDistance(
      passingTimes.from().scheduledStopPointId(),
      getQuayCoordinates.apply(passingTimes.from()),
      passingTimes.to().scheduledStopPointId(),
      getQuayCoordinates.apply(passingTimes.to())
    );
  }

  private double calculateDistance(
    ScheduledStopPointId previousScheduledStopPointId,
    QuayCoordinates previousQuayCoordinates,
    ScheduledStopPointId currentScheduledStopPointId,
    QuayCoordinates currentQuayCoordinates
  ) {
    String key =
      previousScheduledStopPointId.id() +
      "#" +
      currentScheduledStopPointId.id();

    if (distances.containsKey(key)) {
      return distances.get(key);
    }

    key =
      currentScheduledStopPointId.id() +
      "#" +
      previousScheduledStopPointId.id();

    if (distances.containsKey(key)) {
      return distances.get(key);
    }

    double distance = calculateDistanceWithHaversineFormula(
      previousQuayCoordinates,
      currentQuayCoordinates
    );

    distances.put(key, distance);

    return distance;
  }

  /**
   * @see <a href="https://www.geeksforgeeks.org/haversine-formula-to-find-distance-between-two-points-on-a-sphere/">
   * Haversine formula to find distance between two points on a sphere
   * </a>
   */
  public static double calculateDistanceWithHaversineFormula(
    QuayCoordinates from,
    QuayCoordinates to
  ) {
    // distance between latitudes and longitudes
    double distanceLatitude = Math.toRadians(to.latitude() - from.latitude());
    double distanceLongitude = Math.toRadians(
      to.longitude() - from.longitude()
    );

    // convert to radians
    double fromLatitude = Math.toRadians(from.latitude());
    double toLatitude = Math.toRadians(to.latitude());

    // apply formulae
    double a =
      Math.pow(Math.sin(distanceLatitude / 2), 2) +
      Math.pow(Math.sin(distanceLongitude / 2), 2) *
      Math.cos(fromLatitude) *
      Math.cos(toLatitude);

    double earthRadius = 6371008.8;
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return earthRadius * c;
  }
}
