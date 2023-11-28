package no.entur.antu.stoptime;

import org.entur.netex.index.api.NetexEntitiesIndex;
import org.rutebanken.netex.model.*;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;

/**
 * Provides a simpler interface for using {@link TimetabledPassingTime},
 * in {@link ServiceJourney}.
 */
public interface SortedStopTimes {

    /**
     * Sort the timetabled passing times according to their order in the journey pattern.
     */
    static List<StopTime> from(ServiceJourney serviceJourney, NetexEntitiesIndex netexEntitiesIndex) {

        JourneyPattern journeyPattern = getJourneyPattern(netexEntitiesIndex, serviceJourney);
        Map<TimetabledPassingTime, Boolean> stopFlexibility = findStopFlexibility(serviceJourney, journeyPattern, netexEntitiesIndex);

        Map<String, Integer> stopPointIdToOrder = getStopPointIdsOrder(journeyPattern);
        return serviceJourney
                .getPassingTimes()
                .getTimetabledPassingTime()
                .stream()
                .filter(timetabledPassingTime -> timetabledPassingTime.getPointInJourneyPatternRef().getValue() instanceof StopPointInJourneyPatternRefStructure)
                .sorted(comparing(timetabledPassingTime ->
                        stopPointIdToOrder.get(getStopPointId(timetabledPassingTime))))
                .map(timetabledPassingTime ->
                        StopTime.of(timetabledPassingTime, stopFlexibility.get(timetabledPassingTime)))
                .toList();
    }

    private static Map<String, Integer> getStopPointIdsOrder(JourneyPattern journeyPattern) {
        return journeyPattern
                .getPointsInSequence()
                .getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern()
                .stream()
                .filter(StopPointInJourneyPattern.class::isInstance)
                .map(StopPointInJourneyPattern.class::cast)
                .collect(Collectors.toMap(
                        EntityStructure::getId,
                        point -> point.getOrder().intValueExact()
                ));
    }

    /**
     * Map a timetabledPassingTime to true if its stop is a flexible stop area, false otherwise.
     */
    private static Map<TimetabledPassingTime, Boolean> findStopFlexibility(ServiceJourney serviceJourney,
                                                                           JourneyPattern journeyPattern,
                                                                           NetexEntitiesIndex netexEntitiesIndex) {

        Map<String, String> scheduledStopPointIdByStopPointId = getScheduledStopPointIdByStopPointId(journeyPattern);
        Predicate<TimetabledPassingTime> hasFlexibleStopPoint = hasFlexibleStopPoint(
                scheduledStopPointIdByStopPointId,
                netexEntitiesIndex
        );

        return serviceJourney
                .getPassingTimes()
                .getTimetabledPassingTime()
                .stream()
                .filter(timetabledPassingTime -> timetabledPassingTime.getPointInJourneyPatternRef().getValue() instanceof StopPointInJourneyPatternRefStructure)
                .collect(Collectors.toMap(Function.identity(), hasFlexibleStopPoint::test));
    }

    private static Predicate<TimetabledPassingTime> hasFlexibleStopPoint(Map<String, String> scheduledStopPointIdByStopPointId,
                                                                         NetexEntitiesIndex netexEntitiesIndex) {
        return timetabledPassingTime -> netexEntitiesIndex
                .getFlexibleStopPlaceIdByStopPointRefIndex()
                .containsKey(
                        scheduledStopPointIdByStopPointId.get(getStopPointId(timetabledPassingTime))
                );
    }

    /**
     * Return the StopPointInJourneyPattern ID of a given TimeTabledPassingTime.
     */
    private static String getStopPointId(TimetabledPassingTime timetabledPassingTime) {
        return timetabledPassingTime.getPointInJourneyPatternRef().getValue().getRef();
    }

    /**
     * Return the mapping between stop point id and scheduled stop point id for the journey
     * pattern.
     */
    private static Map<String, String> getScheduledStopPointIdByStopPointId(JourneyPattern journeyPattern) {
        return journeyPattern
                .getPointsInSequence()
                .getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern()
                .stream()
                .filter(StopPointInJourneyPattern.class::isInstance)
                .map(StopPointInJourneyPattern.class::cast)
                .collect(
                        Collectors.toMap(
                                StopPointInJourneyPattern::getId,
                                stopPointInJourneyPattern -> stopPointInJourneyPattern.getScheduledStopPointRef().getValue().getRef()
                        )
                );
    }

    private static JourneyPattern getJourneyPattern(NetexEntitiesIndex netexEntitiesIndex,
                                                    ServiceJourney serviceJourney) {
        return netexEntitiesIndex.getJourneyPatternIndex().get(
                serviceJourney.getJourneyPatternRef().getValue().getRef()
        );
    }
}
