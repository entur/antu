package no.entur.antu.validator.passengerstopassignment;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import no.entur.antu.commondata.CommonDataRepository;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.StopPointInJourneyPattern;

public record MissingPassengerStopAssignmentContext(
  String journeyPatternRef,
  String stopPointInJourneyPatternRef,
  String scheduledStopPointRef,
  boolean hasPassengerStopAssignment
) {
  public static final class Builder {

    private final String validationReportId;
    private final CommonDataRepository commonDataRepository;
    private final NetexEntitiesIndex netexEntitiesIndex;

    public Builder(
      String validationReportId,
      CommonDataRepository commonDataRepository,
      NetexEntitiesIndex netexEntitiesIndex
    ) {
      this.validationReportId = validationReportId;
      this.commonDataRepository = commonDataRepository;
      this.netexEntitiesIndex = netexEntitiesIndex;
    }

    public List<MissingPassengerStopAssignmentContext> build(
      JourneyPattern journeyPattern
    ) {
      Predicate<String> hasPassengerStopAssignment = scheduledStopPointRef -> {
        if (commonDataRepository.hasQuayIds(validationReportId)) {
          return (
            commonDataRepository.findQuayIdForScheduledStopPoint(
              scheduledStopPointRef,
              validationReportId
            ) !=
            null
          );
        } else {
          return (
            netexEntitiesIndex
              .getQuayIdByStopPointRefIndex()
              .get(scheduledStopPointRef) !=
            null
          );
        }
      };

      Function<StopPointInJourneyPattern, String> scheduleStopPointRef =
        stopPointInJourneyPattern ->
          stopPointInJourneyPattern
            .getScheduledStopPointRef()
            .getValue()
            .getRef();

      return journeyPattern
        .getPointsInSequence()
        .getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern()
        .stream()
        .filter(StopPointInJourneyPattern.class::isInstance)
        .map(StopPointInJourneyPattern.class::cast)
        .map(stopPointInJourneyPattern ->
          new MissingPassengerStopAssignmentContext(
            journeyPattern.getId(),
            stopPointInJourneyPattern.getId(),
            scheduleStopPointRef.apply(stopPointInJourneyPattern),
            hasPassengerStopAssignment.test(
              scheduleStopPointRef.apply(stopPointInJourneyPattern)
            )
          )
        )
        .toList();
    }
  }
}
