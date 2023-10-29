package no.entur.antu.validator.speedprogressionvalidator;

import no.entur.antu.stop.model.StopPlaceCoordinates;

import java.util.HashMap;
import java.util.Map;

public class DistanceCalculator {

    private Map<String, Double> distances = new HashMap<>();

    public double calculateDistance(String previousTimetabledPassingTimeId,
                                    StopPlaceCoordinates previousStopPlaceCoordinates,
                                    String currentTimetabledPassingTimeId,
                                    StopPlaceCoordinates currentStopPlaceCoordinates) {

        String key = previousTimetabledPassingTimeId + "#" + currentTimetabledPassingTimeId;

        if (distances.containsKey(key)) {
            return distances.get(key);
        }

        key = currentTimetabledPassingTimeId + "#" + previousTimetabledPassingTimeId;

        if (distances.containsKey(key)) {
            return distances.get(key);
        }

        double distance = calculateDistanceWithHaversineFormula(
                previousStopPlaceCoordinates, currentStopPlaceCoordinates
        );

        distances.put(key, distance);

        return distance;
    }

    /**
     * @see <a href="https://www.geeksforgeeks.org/haversine-formula-to-find-distance-between-two-points-on-a-sphere/">
     * Haversine formula to find distance between two points on a sphere
     * </a>
     */
    private static double calculateDistanceWithHaversineFormula(StopPlaceCoordinates from,
                                                                StopPlaceCoordinates to) {

        // distance between latitudes and longitudes
        double distanceLatitude = Math.toRadians(to.latitude() - from.latitude());
        double distanceLongitude = Math.toRadians(to.longitude() - from.longitude());

        // convert to radians
        double fromLatitude = Math.toRadians(from.latitude());
        double toLatitude = Math.toRadians(to.latitude());

        // apply formulae
        double a = Math.pow(Math.sin(distanceLatitude / 2), 2) +
                   Math.pow(Math.sin(distanceLongitude / 2), 2) *
                   Math.cos(fromLatitude) *
                   Math.cos(toLatitude);

        double earthRadius = 6371008.8;
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }
}
