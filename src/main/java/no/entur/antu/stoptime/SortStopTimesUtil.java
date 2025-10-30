package no.entur.antu.stoptime;

import static java.util.Comparator.comparing;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import no.entur.antu.validation.validator.support.NetexUtils;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
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
    JAXBValidationContext validationContext
  ) {
    JourneyPattern journeyPattern = validationContext.journeyPattern(
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
      NetexUtils.scheduledStopPointIdByStopPointId(journeyPattern);

    return validationContext
      .timetabledPassingTimes(serviceJourney)
      .stream()
      .filter(SortStopTimesUtil::hasStopPointInJourneyPatternRef)
      .sorted(
        comparing(timetabledPassingTime ->
          stopPointIdToOrder.get(NetexUtils.stopPointRef(timetabledPassingTime))
        )
      )
      .map(timetabledPassingTime ->
        StopTime.of(
          scheduledStopPointIdByStopPointId.get(
            NetexUtils.stopPointRef(timetabledPassingTime)
          ),
          timetabledPassingTime,
          hasFlexibleStopPoint(
            scheduledStopPointIdByStopPointId
              .get(NetexUtils.stopPointRef(timetabledPassingTime))
              .id(),
            validationContext
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
    return NetexUtils
      .stopPointsInJourneyPattern(journeyPattern)
      .stream()
      .collect(
        Collectors.toMap(
          EntityStructure::getId,
          point -> point.getOrder().intValueExact(),
          (previous, latest) -> latest
        )
      );
  }

  private static boolean hasFlexibleStopPoint(
    String scheduledStopPointId,
    JAXBValidationContext validationContext
  ) {
    return (
      validationContext.flexibleStopPlaceRefFromScheduledStopPointRef(
        scheduledStopPointId
      ) !=
      null
    );
  }
}
