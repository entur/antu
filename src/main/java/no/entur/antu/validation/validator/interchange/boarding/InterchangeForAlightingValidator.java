package no.entur.antu.validation.validator.interchange.boarding;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.entur.netex.validation.validator.*;
import org.entur.netex.validation.validator.jaxb.NetexDataRepository;
import org.entur.netex.validation.validator.model.ServiceJourneyId;
import org.entur.netex.validation.validator.model.ServiceJourneyInterchangeInfo;
import org.entur.netex.validation.validator.model.ServiceJourneyStop;

public class InterchangeForAlightingValidator extends AbstractDatasetValidator {

  private NetexDataRepository netexDataRepository;

  static final ValidationRule RULE = new ValidationRule(
    "INTERCHANGE_ALIGHTING_NOT_ALLOWED_FOR_ALIGHTING_STOP",
    "Interchange is not possible due to alighting being explicitly disallowed for ScheduledStopPoint",
    "Interchange %s is not possible due to alighting being explicitly disallowed for ScheduledStopPoint %s in ServiceJourney with id %s",
    Severity.WARNING
  );

  public InterchangeForAlightingValidator(
    NetexDataRepository netexDataRepository,
    ValidationReportEntryFactory validationReportEntryFactory
  ) {
    super(validationReportEntryFactory);
    this.netexDataRepository = netexDataRepository;
  }

  @Override
  public ValidationReport validate(ValidationReport validationReport) {
    String validationReportId = validationReport.getValidationReportId();
    Map<ServiceJourneyId, List<ServiceJourneyStop>> serviceJourneyStopsCache =
      netexDataRepository.serviceJourneyStops(validationReportId);
    List<ServiceJourneyInterchangeInfo> interchanges =
      netexDataRepository.serviceJourneyInterchangeInfos(validationReportId);

    for (ServiceJourneyInterchangeInfo serviceJourneyInterchangeInfo : interchanges) {
      ServiceJourneyId toJourneyRef =
        serviceJourneyInterchangeInfo.toJourneyRef();
      String toStopPointRef = serviceJourneyInterchangeInfo.toStopPoint().id();
      List<ServiceJourneyStop> feederStops = serviceJourneyStopsCache.get(
        toJourneyRef
      );

      List<ServiceJourneyStop> feederStopsWithMatchingId = feederStops
        .stream()
        .toList()
        .stream()
        .filter(stop -> stop.scheduledStopPointId().equals(toStopPointRef))
        .collect(Collectors.toUnmodifiableList());
      if (feederStopsWithMatchingId.size() >= 1) {
        ServiceJourneyStop stop = feederStopsWithMatchingId.get(0);
        if (!stop.isForAlighting()) {
          validationReport.addValidationReportEntry(
            createValidationReportEntry(
              new ValidationIssue(
                RULE,
                new DataLocation(
                  serviceJourneyInterchangeInfo.interchangeId(),
                  serviceJourneyInterchangeInfo.filename(),
                  null,
                  null
                ),
                serviceJourneyInterchangeInfo.interchangeId(),
                toStopPointRef,
                toJourneyRef.id()
              )
            )
          );
        }
      }
    }

    return validationReport;
  }
}
