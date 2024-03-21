package no.entur.antu.validation.validator.servicejourney.speed;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import no.entur.antu.model.QuayCoordinates;
import no.entur.antu.model.ScheduledStopPointId;
import no.entur.antu.stoptime.PassingTimes;
import no.entur.antu.stoptime.StopTime;
import no.entur.antu.validation.utilities.SphericalDistanceLibrary;

public class Distances {

  private final Map<String, Double> calculatedDistances = new HashMap<>();

  public double findDistance(
    PassingTimes passingTimes,
    Function<StopTime, QuayCoordinates> getQuayCoordinates
  ) {
    return findDistance(
      passingTimes.from().scheduledStopPointId(),
      getQuayCoordinates.apply(passingTimes.from()),
      passingTimes.to().scheduledStopPointId(),
      getQuayCoordinates.apply(passingTimes.to())
    );
  }

  private double findDistance(
    ScheduledStopPointId previousScheduledStopPointId,
    QuayCoordinates previousQuayCoordinates,
    ScheduledStopPointId currentScheduledStopPointId,
    QuayCoordinates currentQuayCoordinates
  ) {
    String key =
      previousScheduledStopPointId.id() +
      "#" +
      currentScheduledStopPointId.id();

    if (calculatedDistances.containsKey(key)) {
      return calculatedDistances.get(key);
    }

    key =
      currentScheduledStopPointId.id() +
      "#" +
      previousScheduledStopPointId.id();

    if (calculatedDistances.containsKey(key)) {
      return calculatedDistances.get(key);
    }

    double distance = SphericalDistanceLibrary.distance(
      previousQuayCoordinates,
      currentQuayCoordinates
    );

    calculatedDistances.put(key, distance);

    return distance;
  }
}
