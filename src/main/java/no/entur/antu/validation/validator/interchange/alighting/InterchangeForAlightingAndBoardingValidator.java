package no.entur.antu.validation.validator.interchange.alighting;

import java.util.List;
import java.util.Map;
import no.entur.antu.validation.validator.interchange.waitingtime.InterchangeWaitingTimeValidator;
import org.entur.netex.validation.validator.*;
import org.entur.netex.validation.validator.jaxb.NetexDataRepository;
import org.entur.netex.validation.validator.model.ServiceJourneyId;
import org.entur.netex.validation.validator.model.ServiceJourneyInterchangeInfo;
import org.entur.netex.validation.validator.model.ServiceJourneyStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This validator checks that alighting is allowed for the FromStopPoint referred to by a ServiceJourneyInterchange.
 * It also checks that boarding is allowed for the ToStopPoint, also referred to from ServiceJourneyInterchange.
 *
 * Chouette reference: 3-Interchange-9-1, 3-Interchange-9-2
 */
public class InterchangeForAlightingAndBoardingValidator
  extends AbstractDatasetValidator {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    InterchangeForAlightingAndBoardingValidator.class
  );

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

      List<ServiceJourneyStop> feederStops = serviceJourneyStopsCache.get(
        fromJourneyRef
      );
      if (feederStops == null || feederStops.isEmpty()) {
        LOGGER.error(
          "No feeder stops found in cache for interchange {} from journey {}",
          serviceJourneyInterchangeInfo.interchangeId(),
          fromJourneyRef.id()
        );
        continue;
      }

      List<ServiceJourneyStop> consumerStops = serviceJourneyStopsCache.get(
        toJourneyRef
      );
      if (consumerStops == null || consumerStops.isEmpty()) {
        LOGGER.error(
          "No consumer stops found in cache for interchange {} from journey {}",
          serviceJourneyInterchangeInfo.interchangeId(),
          toJourneyRef.id()
        );
        continue;
      }

      validationReport =
        validateAlighting(
          validationReport,
          serviceJourneyInterchangeInfo,
          feederStops
        );
      validationReport =
        validateBoarding(
          validationReport,
          serviceJourneyInterchangeInfo,
          consumerStops
        );
    }

    return validationReport;
  }

  ValidationReport validateAlighting(
    ValidationReport validationReport,
    ServiceJourneyInterchangeInfo serviceJourneyInterchangeInfo,
    List<ServiceJourneyStop> fromStopPoints
  ) {
    String fromStopPointRef = serviceJourneyInterchangeInfo
      .fromStopPoint()
      .id();
    List<ServiceJourneyStop> feederStopsWithMatchingId = fromStopPoints
      .stream()
      .filter(stop -> stop.scheduledStopPointId().id().equals(fromStopPointRef))
      .toList();

    if (feederStopsWithMatchingId.size() >= 1) {
      // checking the first element is sufficient because ServiceJourneyStopsCollector
      // aggregate forAlighting values for all stop points matching fromStopPoint in interchange
      ServiceJourneyStop serviceJourneyFromStop = feederStopsWithMatchingId.get(
        0
      );
      if (!serviceJourneyFromStop.isForAlighting()) {
        validationReport.addValidationReportEntry(
          createAlightingValidationReportEntry(
            serviceJourneyInterchangeInfo,
            fromStopPointRef,
            serviceJourneyInterchangeInfo.fromJourneyRef()
          )
        );
      }
    }
    return validationReport;
  }

  ValidationReport validateBoarding(
    ValidationReport validationReport,
    ServiceJourneyInterchangeInfo serviceJourneyInterchangeInfo,
    List<ServiceJourneyStop> toStopPoints
  ) {
    String toStopPointRef = serviceJourneyInterchangeInfo.toStopPoint().id();
    List<ServiceJourneyStop> consumerStopsWithMatchingId = toStopPoints
      .stream()
      .filter(stop -> stop.scheduledStopPointId().id().equals(toStopPointRef))
      .toList();

    if (consumerStopsWithMatchingId.size() >= 1) {
      // checking the first element is sufficient because ServiceJourneyStopsCollector
      // aggregate forBoarding values for all stop points matching fromStopPoint in interchange
      ServiceJourneyStop serviceJourneyToStop = consumerStopsWithMatchingId.get(
        0
      );
      if (!serviceJourneyToStop.isForBoarding()) {
        validationReport.addValidationReportEntry(
          createBoardingValidationReportEntry(
            serviceJourneyInterchangeInfo,
            toStopPointRef,
            serviceJourneyInterchangeInfo.toJourneyRef()
          )
        );
      }
    }
    return validationReport;
  }

  private ValidationReportEntry createValidationReportEntry(
    ValidationRule validationRule,
    ServiceJourneyInterchangeInfo serviceJourneyInterchangeInfo,
    String stopPointRef,
    String journeyRef
  ) {
    return createValidationReportEntry(
      new ValidationIssue(
        validationRule,
        new DataLocation(
          serviceJourneyInterchangeInfo.interchangeId(),
          serviceJourneyInterchangeInfo.filename(),
          null,
          null
        ),
        serviceJourneyInterchangeInfo.interchangeId(),
        stopPointRef,
        journeyRef
      )
    );
  }

  private ValidationReportEntry createBoardingValidationReportEntry(
    ServiceJourneyInterchangeInfo serviceJourneyInterchangeInfo,
    String stopPointRef,
    ServiceJourneyId journeyRef
  ) {
    return createValidationReportEntry(
      BOARDING_RULE,
      serviceJourneyInterchangeInfo,
      stopPointRef,
      journeyRef.id()
    );
  }

  private ValidationReportEntry createAlightingValidationReportEntry(
    ServiceJourneyInterchangeInfo serviceJourneyInterchangeInfo,
    String stopPointRef,
    ServiceJourneyId journeyRef
  ) {
    return createValidationReportEntry(
      ALIGHTING_RULE,
      serviceJourneyInterchangeInfo,
      stopPointRef,
      journeyRef.id()
    );
  }
}
