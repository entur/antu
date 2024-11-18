package no.entur.antu.validation.validator.passengerstopassignment;

import java.util.List;
import java.util.function.Predicate;
import no.entur.antu.validation.validator.support.NetexUtils;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
import org.entur.netex.validation.validator.model.ScheduledStopPointId;
import org.rutebanken.netex.model.JourneyPattern;

public record MissingPassengerStopAssignmentContext(
  String journeyPatternRef,
  String stopPointInJourneyPatternRef,
  ScheduledStopPointId scheduledStopPointId,
  boolean hasPassengerStopAssignment
) {
  public static final class Builder {

    private final JAXBValidationContext validationContext;

    public Builder(JAXBValidationContext validationContext) {
      this.validationContext = validationContext;
    }

    public List<MissingPassengerStopAssignmentContext> build(
      JourneyPattern journeyPattern
    ) {
      Predicate<ScheduledStopPointId> hasPassengerStopAssignment =
        scheduledStopPointId ->
          validationContext.quayIdForScheduledStopPoint(scheduledStopPointId) !=
          null;

      return NetexUtils
        .stopPointsInJourneyPattern(journeyPattern)
        .stream()
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
