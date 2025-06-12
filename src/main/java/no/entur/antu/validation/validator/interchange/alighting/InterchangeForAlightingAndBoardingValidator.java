package no.entur.antu.validation.validator.interchange.alighting;

import java.util.List;
import java.util.Map;
import org.entur.netex.validation.validator.*;
import org.entur.netex.validation.validator.jaxb.NetexDataRepository;
import org.entur.netex.validation.validator.model.ServiceJourneyId;
import org.entur.netex.validation.validator.model.ServiceJourneyInterchangeInfo;
import org.entur.netex.validation.validator.model.ServiceJourneyStop;

public class InterchangeForAlightingAndBoardingValidator extends AbstractDatasetValidator {

  private final NetexDataRepository netexDataRepository;

  static final ValidationRule ALIGHTING_RULE = new ValidationRule(
    "INTERCHANGE_ALIGHTING_NOT_ALLOWED_FOR_ALIGHTING_STOP",
    "Interchange is not possible due to alighting being explicitly disallowed for ScheduledStopPoint",
    "Interchange %s is not possible due to alighting being explicitly disallowed for ScheduledStopPoint %s in ServiceJourney with id %s",
    Severity.WARNING
  );
  static final ValidationRule BOARDING_RULE = new ValidationRule(
    "INTERCHANGE_BOARDING_NOT_ALLOWED_FOR_BOARDING_STOP",
    "Interchange is not possible due to boarding being explicitly disallowed for ScheduledStopPoint",
    "Interchange %s is not possible due to boarding being explicitly disallowed for ScheduledStopPoint %s in ServiceJourney with id %s",
    Severity.WARNING
  );

  public InterchangeForAlightingAndBoardingValidator(
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
      ServiceJourneyId fromJourneyRef =
        serviceJourneyInterchangeInfo.fromJourneyRef();

      String toStopPointRef = serviceJourneyInterchangeInfo.toStopPoint().id();
      String fromStopPointRef = serviceJourneyInterchangeInfo
        .fromStopPoint()
        .id();

      List<ServiceJourneyStop> feederStops = serviceJourneyStopsCache.get(
        fromJourneyRef
      );
      List<ServiceJourneyStop> consumerStops = serviceJourneyStopsCache.get(
        toJourneyRef
      );

      List<ServiceJourneyStop> feederStopsWithMatchingId = feederStops
        .stream()
        .filter(stop ->
          stop.scheduledStopPointId().id().equals(fromStopPointRef)
        )
        .toList();

      if (feederStopsWithMatchingId.size() >= 1) {
        for (ServiceJourneyStop serviceJourneyFromStop : feederStopsWithMatchingId) {
          if (!serviceJourneyFromStop.isForAlighting()) {
            validationReport.addValidationReportEntry(
              createValidationReportEntry(
                new ValidationIssue(
                  ALIGHTING_RULE,
                  new DataLocation(
                    serviceJourneyInterchangeInfo.interchangeId(),
                    serviceJourneyInterchangeInfo.filename(),
                    null,
                    null
                  ),
                  serviceJourneyInterchangeInfo.interchangeId(),
                  fromStopPointRef,
                  fromJourneyRef.id()
                )
              )
            );
          }
        }
      }

      List<ServiceJourneyStop> consumerStopsWithMatchingId = consumerStops
        .stream()
        .filter(stop -> stop.scheduledStopPointId().id().equals(toStopPointRef))
        .toList();

      if (consumerStopsWithMatchingId.size() >= 1) {
        for (ServiceJourneyStop serviceJourneyToStop : consumerStopsWithMatchingId) {
          if (!serviceJourneyToStop.isForBoarding()) {
            validationReport.addValidationReportEntry(
              createValidationReportEntry(
                new ValidationIssue(
                  BOARDING_RULE,
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
    }

    return validationReport;
  }
}
