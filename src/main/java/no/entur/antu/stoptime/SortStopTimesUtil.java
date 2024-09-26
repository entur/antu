package no.entur.antu.stoptime;

import static java.util.Comparator.comparing;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import no.entur.antu.validation.AntuNetexData;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.model.ScheduledStopPointId;
import org.rutebanken.netex.model.EntityStructure;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.StopPointInJourneyPatternRefStructure;
import org.rutebanken.netex.model.TimetabledPassingTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This utility class is used to sort the timetabled passing times of a service journey according to
 * their order in the journey pattern.
 * The order of the passing times is determined by the order of the stop points in the journey pattern.
 * The passing times are sorted by their order in the journey pattern, and warped in a StopTime object.
 */
public final class SortStopTimesUtil {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    SortStopTimesUtil.class
  );

  /**
   * Prevent instantiation of this utility class.
   */
  private SortStopTimesUtil() {}

  /**
   * Sort the timetabled passing times according to their order in the journey pattern.
   */
  public static List<StopTime> getSortedStopTimes(
    ServiceJourney serviceJourney,
    AntuNetexData antuNetexData
  ) {
    JourneyPattern journeyPattern = antuNetexData.journeyPattern(
      serviceJourney
    );

    if (journeyPattern == null) {
      LOGGER.debug(
        "No journey pattern ref found on service journey {}",
        serviceJourney.getId()
      );
      return List.of();
    }

    Map<String, Integer> stopPointIdToOrder = getStopPointIdsOrder(
      journeyPattern
    );

    Map<String, ScheduledStopPointId> scheduledStopPointIdByStopPointId =
      AntuNetexData.scheduledStopPointIdByStopPointId(journeyPattern);

    return antuNetexData
      .timetabledPassingTimes(serviceJourney)
      .filter(SortStopTimesUtil::hasStopPointInJourneyPatternRef)
      .sorted(
        comparing(timetabledPassingTime ->
          stopPointIdToOrder.get(
            AntuNetexData.stopPointRef(timetabledPassingTime)
          )
        )
      )
      .map(timetabledPassingTime ->
        StopTime.of(
          scheduledStopPointIdByStopPointId.get(
            AntuNetexData.stopPointRef(timetabledPassingTime)
          ),
          timetabledPassingTime,
          hasFlexibleStopPoint(
            antuNetexData.netexEntitiesIndex(),
            scheduledStopPointIdByStopPointId.get(
              AntuNetexData.stopPointRef(timetabledPassingTime)
            )
          )
        )
      )
      .toList();
  }

  private static boolean hasStopPointInJourneyPatternRef(
    TimetabledPassingTime timetabledPassingTime
  ) {
    return (
      timetabledPassingTime
        .getPointInJourneyPatternRef()
        .getValue() instanceof StopPointInJourneyPatternRefStructure
    );
  }

  private static Map<String, Integer> getStopPointIdsOrder(
    JourneyPattern journeyPattern
  ) {
    return AntuNetexData
      .stopPointsInJourneyPattern(journeyPattern)
      .collect(
        Collectors.toMap(
          EntityStructure::getId,
          point -> point.getOrder().intValueExact()
        )
      );
  }

  private static boolean hasFlexibleStopPoint(
    NetexEntitiesIndex netexEntitiesIndex,
    ScheduledStopPointId scheduledStopPointId
  ) {
    return netexEntitiesIndex
      .getFlexibleStopPlaceIdByStopPointRefIndex()
      .containsKey(scheduledStopPointId.id());
  }
}
