package no.entur.antu.validation.validator.interchange.stoppoints;

import java.util.Objects;
import org.entur.netex.validation.validator.AbstractDatasetValidator;
import org.entur.netex.validation.validator.DataLocation;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.entur.netex.validation.validator.ValidationReportEntryFactory;
import org.entur.netex.validation.validator.jaxb.NetexDataRepository;
import org.entur.netex.validation.validator.model.ScheduledStopPointId;
import org.entur.netex.validation.validator.model.ServiceJourneyId;
import org.entur.netex.validation.validator.model.ServiceJourneyStop;

/**
 * Validates that the stop points in interchange are part of the respective service journeys in the interchange.
 * Chouette reference: 3-Interchange-6-1, 3-Interchange-6-2
 */
public class StopPointsInVehicleJourneyValidator
  extends AbstractDatasetValidator {

  private final NetexDataRepository netexDataRepository;

  public StopPointsInVehicleJourneyValidator(
    ValidationReportEntryFactory validationReportEntryFactory,
    NetexDataRepository netexDataRepository
  ) {
    super(validationReportEntryFactory);
    this.netexDataRepository = netexDataRepository;
  }

  @Override
  public ValidationReport validate(ValidationReport validationReport) {
    netexDataRepository
      .serviceJourneyInterchangeInfos(validationReport.getValidationReportId())
      .stream()
      .map(serviceJourneyInterchange ->
        StopPointsInVehicleJourneyContext.of(
          validationReport.getValidationReportId(),
          netexDataRepository,
          serviceJourneyInterchange
        )
      )
      .filter(StopPointsInVehicleJourneyContext::isValid)
      .map(this::validateStopPoint)
      .filter(Objects::nonNull)
      .forEach(validationReport::addValidationReportEntry);

    return validationReport;
  }

  private ValidationReportEntry validateStopPoint(
    StopPointsInVehicleJourneyContext context
  ) {
    if (
      context
        .serviceJourneyStopsForFromJourneyRef()
        .stream()
        .map(ServiceJourneyStop::scheduledStopPointId)
        .noneMatch(fromStopPoint ->
          Objects.equals(
            fromStopPoint,
            context.serviceJourneyInterchangeInfo().fromStopPoint()
          )
        )
    ) {
      return createValidationReportEntry(
        "FROM_POINT_REF_IN_INTERCHANGE_IS_NOT_PART_OF_FROM_JOURNEY_REF",
        context.serviceJourneyInterchangeInfo().interchangeId(),
        context.serviceJourneyInterchangeInfo().filename(),
        context.serviceJourneyInterchangeInfo().fromStopPoint(),
        context.serviceJourneyInterchangeInfo().fromJourneyRef()
      );
    }

    if (
      context
        .serviceJourneyStopsForToJourneyRef()
        .stream()
        .map(ServiceJourneyStop::scheduledStopPointId)
        .noneMatch(toStopPoint ->
          Objects.equals(
            toStopPoint,
            context.serviceJourneyInterchangeInfo().toStopPoint()
          )
        )
    ) {
      return createValidationReportEntry(
        "TO_POINT_REF_IN_INTERCHANGE_IS_NOT_PART_OF_TO_JOURNEY_REF",
        context.serviceJourneyInterchangeInfo().interchangeId(),
        context.serviceJourneyInterchangeInfo().filename(),
        context.serviceJourneyInterchangeInfo().toStopPoint(),
        context.serviceJourneyInterchangeInfo().toJourneyRef()
      );
    }
    return null;
  }

  private ValidationReportEntry createValidationReportEntry(
    String ruleCode,
    String interchangeId,
    String filename,
    ScheduledStopPointId stopPoint,
    ServiceJourneyId journeyRef
  ) {
    return createValidationReportEntry(
      ruleCode,
      new DataLocation(interchangeId, filename, 0, 0),
      String.format(
        "Stop point (%s) is not a part of journey ref (%s).",
        stopPoint.id(),
        journeyRef.id()
      )
    );
  }
}