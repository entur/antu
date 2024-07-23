package no.entur.antu.validation.validator.passengerstopassignment;

import java.util.List;
import java.util.function.Predicate;
import no.entur.antu.model.ScheduledStopPointId;
import no.entur.antu.validation.AntuNetexData;
import org.rutebanken.netex.model.JourneyPattern;

public record MissingPassengerStopAssignmentContext(
  String journeyPatternRef,
  String stopPointInJourneyPatternRef,
  ScheduledStopPointId scheduledStopPointId,
  boolean hasPassengerStopAssignment
) {
  public static final class Builder {

    private final AntuNetexData antuNetexData;

    public Builder(AntuNetexData antuNetexData) {
      this.antuNetexData = antuNetexData;
    }

    public List<MissingPassengerStopAssignmentContext> build(
      JourneyPattern journeyPattern
    ) {
      Predicate<ScheduledStopPointId> hasPassengerStopAssignment =
        scheduledStopPointId ->
          antuNetexData.quayIdForScheduledStopPoint(scheduledStopPointId) !=
          null;

      return AntuNetexData
        .stopPointsInJourneyPattern(journeyPattern)
        .map(stopPointInJourneyPattern ->
          new MissingPassengerStopAssignmentContext(
            journeyPattern.getId(),
            stopPointInJourneyPattern.getId(),
            ScheduledStopPointId.of(stopPointInJourneyPattern),
            hasPassengerStopAssignment.test(
              ScheduledStopPointId.of(stopPointInJourneyPattern)
            )
          )
        )
        .toList();
    }
  }
}
