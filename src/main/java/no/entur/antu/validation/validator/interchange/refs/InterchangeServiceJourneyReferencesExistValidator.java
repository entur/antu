package no.entur.antu.validation.validator.interchange.refs;

import java.util.*;
import org.entur.netex.validation.validator.*;
import org.entur.netex.validation.validator.jaxb.NetexDataRepository;
import org.entur.netex.validation.validator.model.ServiceJourneyId;
import org.entur.netex.validation.validator.model.ServiceJourneyInterchangeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InterchangeServiceJourneyReferencesExistValidator
  extends AbstractDatasetValidator {

  static final ValidationRule RULE_NON_EXISTING_SERVICE_JOURNEY_REF =
    new ValidationRule(
      "RULE_NON_EXISTING_SERVICE_JOURNEY_REF",
      "ServiceJourneyInterchange refers to non-existing service journey",
      "ServiceJourneyInterchange %s refers to non-existing service journey %s",
      Severity.WARNING
    );

  private final NetexDataRepository netexDataRepository;

  private static final Logger LOGGER = LoggerFactory.getLogger(
    InterchangeServiceJourneyReferencesExistValidator.class
  );

  public InterchangeServiceJourneyReferencesExistValidator(
    ValidationReportEntryFactory validationReportEntryFactory,
    NetexDataRepository netexDataRepository
  ) {
    super(validationReportEntryFactory);
    this.netexDataRepository = netexDataRepository;
  }

  static ValidationIssue createValidationIssue(
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
        createValidationIssue(
          serviceJourneyInterchangeInfo,
          fromServiceJourneyRef.id()
        )
      );
    }

    if (!serviceJourneyIds.contains(toServiceJourneyRef)) {
      issues.add(
        createValidationIssue(
          serviceJourneyInterchangeInfo,
          toServiceJourneyRef.id()
        )
      );
    }

    return issues;
  }

  @Override
  public ValidationReport validate(ValidationReport validationReport) {
    LOGGER.info("Starting validation of interchange waiting times");

    String validationReportId = validationReport.getValidationReportId();

    List<ServiceJourneyInterchangeInfo> serviceJourneyInterchangeInfoList =
      netexDataRepository.serviceJourneyInterchangeInfos(validationReportId);
    Set<ServiceJourneyId> existingServiceJourneyIds = new HashSet<>(
      netexDataRepository.serviceJourneyStops(validationReportId).keySet()
    );

    for (ServiceJourneyInterchangeInfo serviceJourneyInterchangeInfo : serviceJourneyInterchangeInfoList) {
      List<ValidationIssue> validationIssues = validateServiceJourneyRefsExists(
        serviceJourneyInterchangeInfo,
        existingServiceJourneyIds
      );
      validationIssues.forEach(validationIssue ->
        validationReport.addValidationReportEntry(
          createValidationReportEntry(validationIssue)
        )
      );
    }

    return validationReport;
  }
}
