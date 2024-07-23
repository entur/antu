package no.entur.antu.validation.validator.interchange.stoppoints;

import java.util.Objects;
import java.util.function.Consumer;
import no.entur.antu.model.ServiceJourneyStop;
import no.entur.antu.validation.AntuNetexData;
import no.entur.antu.validation.AntuNetexValidator;
import no.entur.antu.validation.RuleCode;
import no.entur.antu.validation.ValidationError;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntryFactory;
import org.entur.netex.validation.validator.xpath.ValidationContext;

/**
 * Validates that the stop points in interchange are part of the respective service journeys in the interchange.
 * Chouette reference: 3-Interchange-6-1, 3-Interchange-6-2
 */
public class StopPointsInVehicleJourneyValidator extends AntuNetexValidator {

  public StopPointsInVehicleJourneyValidator(
    ValidationReportEntryFactory validationReportEntryFactory
  ) {
    super(validationReportEntryFactory);
  }

  @Override
  protected RuleCode[] getRuleCodes() {
    return StopPointsInVehicleJourneyError.RuleCode.values();
  }

  @Override
  protected void validateLineFile(
    ValidationReport validationReport,
    ValidationContext validationContext,
    AntuNetexData antuNetexData
  ) {
    antuNetexData
      .serviceJourneyInterchanges()
      .map(serviceJourneyInterchange ->
        StopPointsInVehicleJourneyContext.of(
          antuNetexData,
          serviceJourneyInterchange
        )
      )
      .filter(StopPointsInVehicleJourneyContext::isValid)
      .forEach(context ->
        validateStopPoint(
          antuNetexData,
          context,
          validationError ->
            addValidationReportEntry(
              validationReport,
              validationContext,
              validationError
            )
        )
      );
  }

  private void validateStopPoint(
    AntuNetexData antuNetexData,
    StopPointsInVehicleJourneyContext context,
    Consumer<ValidationError> reportError
  ) {
    if (
      context
        .serviceJourneyStopsForFromJourneyRef()
        .stream()
        .map(ServiceJourneyStop::scheduledStopPointId)
        .noneMatch(fromStopPoint ->
          Objects.equals(fromStopPoint, context.fromStopPoint())
        )
    ) {
      reportError.accept(
        new StopPointsInVehicleJourneyError(
          StopPointsInVehicleJourneyError.RuleCode.FROM_POINT_REF_IN_INTERCHANGE_IS_NOT_PART_OF_FROM_JOURNEY_REF,
          context.interchangeId(),
          antuNetexData.stopPointName(context.fromStopPoint()),
          context.fromJourneyRef()
        )
      );
    }

    if (
      context
        .serviceJourneyStopsForToJourneyRef()
        .stream()
        .map(ServiceJourneyStop::scheduledStopPointId)
        .noneMatch(toStopPoint ->
          Objects.equals(toStopPoint, context.toStopPoint())
        )
    ) {
      reportError.accept(
        new StopPointsInVehicleJourneyError(
          StopPointsInVehicleJourneyError.RuleCode.TO_POINT_REF_IN_INTERCHANGE_IS_NOT_PART_OF_TO_JOURNEY_REF,
          context.interchangeId(),
          antuNetexData.stopPointName(context.toStopPoint()),
          context.toJourneyRef()
        )
      );
    }
  }
}
