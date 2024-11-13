package no.entur.antu.validation.validator.support;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.entur.netex.validation.validator.model.ScheduledStopPointId;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.PointInLinkSequence_VersionedChildStructure;
import org.rutebanken.netex.model.PointsInJourneyPattern_RelStructure;
import org.rutebanken.netex.model.StopPointInJourneyPattern;
import org.rutebanken.netex.model.TimetabledPassingTime;

public class NetexUtils {

  private NetexUtils() {}

  /**
   * Return the StopPointInJourneyPattern ID of a given TimeTabledPassingTime.
   */
  public static String stopPointRef(
    TimetabledPassingTime timetabledPassingTime
  ) {
    return timetabledPassingTime
      .getPointInJourneyPatternRef()
      .getValue()
      .getRef();
  }

  /**
   * Return the mapping between stop point id and scheduled stop point id for the journey
   * pattern.
   */
  public static Map<String, ScheduledStopPointId> scheduledStopPointIdByStopPointId(
    JourneyPattern journeyPattern
  ) {
    return stopPointsInJourneyPattern(journeyPattern)
      .stream()
      .collect(
        Collectors.toMap(
          StopPointInJourneyPattern::getId,
          ScheduledStopPointId::of
        )
      );
  }

  /**
   * Find the stop points in journey pattern for the given journey pattern, sorted by order.
   */
  public static List<StopPointInJourneyPattern> stopPointsInJourneyPattern(
    JourneyPattern journeyPattern
  ) {
    return Optional
      .ofNullable(journeyPattern.getPointsInSequence())
      .map(
        PointsInJourneyPattern_RelStructure::getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern
      )
      .map(stopPointsInJourneyPattern ->
        stopPointsInJourneyPattern
          .stream()
          .filter(StopPointInJourneyPattern.class::isInstance)
          .map(StopPointInJourneyPattern.class::cast)
          .sorted(
            Comparator.comparing(
              PointInLinkSequence_VersionedChildStructure::getOrder
            )
          )
      )
      .orElse(Stream.empty())
      .toList();
  }

  /**
   * Find the stop point in journey pattern for the
   * given stop point in journey pattern reference.
   */
  public static StopPointInJourneyPattern stopPointInJourneyPattern(
    String stopPointInJourneyPatternRef,
    JourneyPattern journeyPattern
  ) {
    return stopPointsInJourneyPattern(journeyPattern)
      .stream()
      .filter(stopPointInJourneyPattern ->
        stopPointInJourneyPattern.getId().equals(stopPointInJourneyPatternRef)
      )
      .findFirst()
      .orElse(null);
  }
}
