package no.entur.antu.validation.validator.interchange.refs;

import java.util.*;
import org.entur.netex.validation.validator.*;
import org.entur.netex.validation.validator.jaxb.NetexDataRepository;
import org.entur.netex.validation.validator.model.ServiceJourneyId;
import org.entur.netex.validation.validator.model.ServiceJourneyInterchangeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceJourneyInterchangeReferencesExistValidator
  extends AbstractDatasetValidator {

  static final ValidationRule RULE_NON_EXISTING_SERVICE_JOURNEY_REF =
    new ValidationRule(
      "RULE_NON_EXISTING_SERVICE_JOURNEY_REF",
      "ServiceJourneyInterchange refers to non-existing service journey",
      "ServiceJourneyInterchange %s refers to non-existing service journey %s",
      Severity.ERROR
    );

  static final ValidationRule RULE_NON_EXISTING_STOP_POINT_REF =
    new ValidationRule(
      "RULE_NON_EXISTING_STOP_POINT_REF",
      "ServiceJourneyInterchange refers to non-existing scheduled stop point",
      "ServiceJourneyInterchange %s refers to non-existing scheduled stop point %s",
      Severity.ERROR
    );

  private final NetexDataRepository netexDataRepository;

  private static final Logger LOGGER = LoggerFactory.getLogger(
    ServiceJourneyInterchangeReferencesExistValidator.class
  );

  public ServiceJourneyInterchangeReferencesExistValidator(
    ValidationReportEntryFactory validationReportEntryFactory,
    NetexDataRepository netexDataRepository
  ) {
    super(validationReportEntryFactory);
    this.netexDataRepository = netexDataRepository;
  }

  static ValidationIssue createValidationIssueOnMissingStopPoint(
    ServiceJourneyInterchangeInfo serviceJourneyInterchangeInfo,
    String scheduledStopPointId
  ) {
    return new ValidationIssue(
      RULE_NON_EXISTING_STOP_POINT_REF,
      new DataLocation(
        serviceJourneyInterchangeInfo.interchangeId(),
        serviceJourneyInterchangeInfo.filename(),
        null,
        null
      ),
      serviceJourneyInterchangeInfo.interchangeId(),
      scheduledStopPointId
    );
  }

  static List<ValidationIssue> validateStopPointRefsExists(
    ServiceJourneyInterchangeInfo serviceJourneyInterchangeInfo,
    Set<String> stopPointIds
  ) {
    List<ValidationIssue> issues = new ArrayList<>();
    String fromPointRef = serviceJourneyInterchangeInfo.fromStopPoint().id();
    String toPointRef = serviceJourneyInterchangeInfo.toStopPoint().id();

    if (!stopPointIds.contains(fromPointRef)) {
      issues.add(
        createValidationIssueOnMissingStopPoint(
          serviceJourneyInterchangeInfo,
          fromPointRef
        )
      );
    }

    if (!stopPointIds.contains(toPointRef)) {
      issues.add(
        createValidationIssueOnMissingStopPoint(
          serviceJourneyInterchangeInfo,
          toPointRef
        )
      );
    }

    return issues;
  }

  static ValidationIssue createValidationIssueOnMissingServiceJourney(
    ServiceJourneyInterchangeInfo serviceJourneyInterchangeInfo,
    String journeyRef
  ) {
    return new ValidationIssue(
      RULE_NON_EXISTING_SERVICE_JOURNEY_REF,
      new DataLocation(
        serviceJourneyInterchangeInfo.interchangeId(),
        serviceJourneyInterchangeInfo.filename(),
        null,
        null
      ),
      serviceJourneyInterchangeInfo.interchangeId(),
      journeyRef
    );
  }

  static List<ValidationIssue> validateServiceJourneyRefsExists(
    ServiceJourneyInterchangeInfo serviceJourneyInterchangeInfo,
    Set<ServiceJourneyId> serviceJourneyIds
  ) {
    List<ValidationIssue> issues = new ArrayList<>();
    ServiceJourneyId fromServiceJourneyRef =
      serviceJourneyInterchangeInfo.fromJourneyRef();
    ServiceJourneyId toServiceJourneyRef =
      serviceJourneyInterchangeInfo.toJourneyRef();

    if (!serviceJourneyIds.contains(fromServiceJourneyRef)) {
      issues.add(
        createValidationIssueOnMissingServiceJourney(
          serviceJourneyInterchangeInfo,
          fromServiceJourneyRef.id()
        )
      );
    }

    if (!serviceJourneyIds.contains(toServiceJourneyRef)) {
      issues.add(
        createValidationIssueOnMissingServiceJourney(
          serviceJourneyInterchangeInfo,
          toServiceJourneyRef.id()
        )
      );
    }

    return issues;
  }

  @Override
  public ValidationReport validate(ValidationReport validationReport) {
    LOGGER.info(
      "Starting validation of service journey references in interchanges"
    );

    String validationReportId = validationReport.getValidationReportId();

    List<ServiceJourneyInterchangeInfo> serviceJourneyInterchangeInfoList =
      netexDataRepository.serviceJourneyInterchangeInfos(validationReportId);
    Set<ServiceJourneyId> existingServiceJourneyIds = new HashSet<>(
      netexDataRepository.serviceJourneyStops(validationReportId).keySet()
    );

    for (ServiceJourneyInterchangeInfo serviceJourneyInterchangeInfo : serviceJourneyInterchangeInfoList) {
      List<ValidationIssue> validationIssuesOnServiceJourneyRefs =
        validateServiceJourneyRefsExists(
          serviceJourneyInterchangeInfo,
          existingServiceJourneyIds
        );

      validationIssuesOnServiceJourneyRefs.forEach(validationIssue ->
        validationReport.addValidationReportEntry(
          createValidationReportEntry(validationIssue)
        )
      );

      List<ValidationIssue> validationIssuesOnStopPointRefs =
        validateStopPointRefsExists(
          serviceJourneyInterchangeInfo,
          netexDataRepository.scheduledStopPointIds(validationReportId)
        );

      validationIssuesOnStopPointRefs.forEach(validationIssue ->
        validationReport.addValidationReportEntry(
          createValidationReportEntry(validationIssue)
        )
      );
    }

    LOGGER.info("Done validating service journey references in interchanges");
    return validationReport;
  }
}
