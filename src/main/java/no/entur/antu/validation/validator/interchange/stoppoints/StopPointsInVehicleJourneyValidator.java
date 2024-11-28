package no.entur.antu.validation.validator.interchange.stoppoints;

import java.util.List;
import java.util.Objects;
import org.entur.netex.validation.validator.AbstractDatasetValidator;
import org.entur.netex.validation.validator.DataLocation;
import org.entur.netex.validation.validator.Severity;
import org.entur.netex.validation.validator.ValidationIssue;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.entur.netex.validation.validator.ValidationReportEntryFactory;
import org.entur.netex.validation.validator.ValidationRule;
import org.entur.netex.validation.validator.jaxb.NetexDataRepository;
import org.entur.netex.validation.validator.model.ScheduledStopPointId;
import org.entur.netex.validation.validator.model.ServiceJourneyId;
import org.entur.netex.validation.validator.model.ServiceJourneyInterchangeInfo;
import org.entur.netex.validation.validator.model.ServiceJourneyStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates that the stop points in interchange are part of the respective service journeys in the interchange.
 * Chouette reference: 3-Interchange-6-1, 3-Interchange-6-2
 */
public class StopPointsInVehicleJourneyValidator
  extends AbstractDatasetValidator {

  static final ValidationRule RULE_FROM_POINT_REF_IN_INTERCHANGE_IS_NOT_PART_OF_FROM_JOURNEY_REF =
    new ValidationRule(
      "FROM_POINT_REF_IN_INTERCHANGE_IS_NOT_PART_OF_FROM_JOURNEY_REF",
      "FromPointRef in interchange is not a part of FromJourneyRef",
      "Stop point (%s) is not a part of journey ref (%s).",
      Severity.WARNING
    );

  static final ValidationRule RULE_TO_POINT_REF_IN_INTERCHANGE_IS_NOT_PART_OF_TO_JOURNEY_REF =
    new ValidationRule(
      "TO_POINT_REF_IN_INTERCHANGE_IS_NOT_PART_OF_TO_JOURNEY_REF",
      "ToPointRef in interchange is not a part of FromJourneyRef",
      "Stop point (%s) is not a part of journey ref (%s).",
      Severity.WARNING
    );

  private static final Logger LOGGER = LoggerFactory.getLogger(
    StopPointsInVehicleJourneyValidator.class
  );

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
    LOGGER.info("Validating interchange stop points in vehicle journey.");

    List<ServiceJourneyInterchangeInfo> serviceJourneyInterchangeInfos =
      netexDataRepository.serviceJourneyInterchangeInfos(
        validationReport.getValidationReportId()
      );

    if (
      serviceJourneyInterchangeInfos == null ||
      serviceJourneyInterchangeInfos.isEmpty()
    ) {
      return validationReport;
    }

    StopPointsInVehicleJourneyContext.Builder builder =
      new StopPointsInVehicleJourneyContext.Builder(
        validationReport.getValidationReportId(),
        netexDataRepository
      );

    builder.primeCache();

    serviceJourneyInterchangeInfos
      .stream()
      .map(builder::build)
      .filter(Objects::nonNull)
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
      context.serviceJourneyStopsForFromJourneyRef() == null ||
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
        RULE_FROM_POINT_REF_IN_INTERCHANGE_IS_NOT_PART_OF_FROM_JOURNEY_REF,
        context.serviceJourneyInterchangeInfo().interchangeId(),
        context.serviceJourneyInterchangeInfo().filename(),
        context.serviceJourneyInterchangeInfo().fromStopPoint(),
        context.serviceJourneyInterchangeInfo().fromJourneyRef()
      );
    }

    if (
      context.serviceJourneyStopsForToJourneyRef() == null ||
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
        RULE_TO_POINT_REF_IN_INTERCHANGE_IS_NOT_PART_OF_TO_JOURNEY_REF,
        context.serviceJourneyInterchangeInfo().interchangeId(),
        context.serviceJourneyInterchangeInfo().filename(),
        context.serviceJourneyInterchangeInfo().toStopPoint(),
        context.serviceJourneyInterchangeInfo().toJourneyRef()
      );
    }
    return null;
  }

  private ValidationReportEntry createValidationReportEntry(
    ValidationRule rule,
    String interchangeId,
    String filename,
    ScheduledStopPointId stopPoint,
    ServiceJourneyId journeyRef
  ) {
    return createValidationReportEntry(
      new ValidationIssue(
        rule,
        new DataLocation(interchangeId, filename, 0, 0),
        stopPoint.id(),
        journeyRef.id()
      )
    );
  }
}
