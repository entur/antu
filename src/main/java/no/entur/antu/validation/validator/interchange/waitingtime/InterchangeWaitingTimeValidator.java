package no.entur.antu.validation.validator.interchange.waitingtime;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.entur.netex.validation.validator.*;
import org.entur.netex.validation.validator.jaxb.NetexDataRepository;
import org.entur.netex.validation.validator.model.ScheduledStopPointId;
import org.entur.netex.validation.validator.model.ServiceJourneyId;
import org.entur.netex.validation.validator.model.ServiceJourneyInterchangeInfo;
import org.entur.netex.validation.validator.model.ServiceJourneyStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates that the ServiceJourneys referred to by a ServiceJourneyInterchange have possibilities
 * of making interchanges, and that the shortest actual waiting time between the service journeys
 * do not exceed a duration of two hours.
 *
 * Chouette reference: 3-Interchange-8-1, 3-Interchange-8-2, 3-Interchange-10
 */
public class InterchangeWaitingTimeValidator extends AbstractDatasetValidator {

  // Closest resemblance to 3-Interchange-10, but validated differently
  static final ValidationRule RULE_NO_INTERCHANGE_POSSIBLE = new ValidationRule(
    "RULE_NO_INTERCHANGE_POSSIBLE",
    "Feeder and consumer vehicle journeys have no interchange possibilities",
    "ServiceJourneyInterchange %s has no interchange possibilities for vehicle journey %s and consumer vehicle journey %s",
    Severity.WARNING
  );

  static final ValidationRule RULE_SERVICE_JOURNEYS_HAS_TOO_LONG_WAITING_TIME_WARNING =
    new ValidationRule(
      "RULE_SERVICE_JOURNEYS_HAS_TOO_LONG_WAITING_TIME_WARNING",
      "Waiting time between feeder and consumer vehicle journeys exceed warning treshold",
      "ServiceJourneyInterchange %s has waiting time of %s seconds which exceeds waiting time threshold of %s seconds",
      Severity.WARNING
    );

  private final NetexDataRepository netexDataRepository;

  private static final Logger LOGGER = LoggerFactory.getLogger(
    InterchangeWaitingTimeValidator.class
  );

  static final Duration waitingTimeWarningThreshold = Duration.ofHours(2);

  public InterchangeWaitingTimeValidator(
    ValidationReportEntryFactory validationReportEntryFactory,
    NetexDataRepository netexDataRepository
  ) {
    super(validationReportEntryFactory);
    this.netexDataRepository = netexDataRepository;
  }

  private List<LocalDateTime> getActiveDatesForServiceJourney(
    String validationReportId,
    ServiceJourneyId serviceJourneyId
  ) {
    return this.netexDataRepository.serviceJourneyIdToActiveDates(
        validationReportId
      )
      .get(serviceJourneyId);
  }

  private ServiceJourneyStop getServiceJourneyStopByScheduledStopPointId(
    List<ServiceJourneyStop> stops,
    ScheduledStopPointId scheduledStopPointId
  ) {
    return stops
      .stream()
      .filter(serviceJourneyStop ->
        serviceJourneyStop.scheduledStopPointId().equals(scheduledStopPointId)
      )
      .findFirst()
      .orElse(null);
  }

  static Duration getShortestActualWaitingTimeForInterchange(
    List<LocalDateTime> fromJourneyActiveDates,
    List<LocalDateTime> toJourneyActiveDates
  ) {
    Duration minimumDuration = null;
    int toJourneyActiveDateIndex = 0;
    for (LocalDateTime fromJourneyActiveDateTime : fromJourneyActiveDates) {
      while (toJourneyActiveDateIndex < toJourneyActiveDates.size()) {
        LocalDateTime toJourneyActiveDateTime = toJourneyActiveDates.get(
          toJourneyActiveDateIndex
        );
        if (toJourneyActiveDateTime.isBefore(fromJourneyActiveDateTime)) {
          toJourneyActiveDateIndex++;
          continue;
        }

        Duration actualWaitTime = Duration.between(
          fromJourneyActiveDateTime,
          toJourneyActiveDateTime
        );
        if (
          minimumDuration == null ||
          actualWaitTime.compareTo(minimumDuration) <= 0
        ) {
          minimumDuration = actualWaitTime;
        }
        break;
      }
    }
    return minimumDuration;
  }

  static LocalDateTime createLocalDateTimeFromDayOffsetAndPassingTime(
    LocalDateTime localDateTime,
    int dayOffset,
    LocalTime passingTime
  ) {
    return localDateTime
      .plusDays(dayOffset)
      .plusNanos(passingTime.toNanoOfDay());
  }

  static List<LocalDateTime> sortedLocalDateTimesForServiceJourneyAtStop(
    List<LocalDateTime> activeDates,
    int dayOffset,
    LocalTime passingTime
  ) {
    return activeDates
      .stream()
      .map(localDateTime ->
        createLocalDateTimeFromDayOffsetAndPassingTime(
          localDateTime,
          dayOffset,
          passingTime
        )
      )
      .sorted()
      .toList();
  }

  static ValidationIssue createNoInterchangePossibleValidationIssue(
    ServiceJourneyInterchangeInfo serviceJourneyInterchangeInfo
  ) {
    return new ValidationIssue(
      RULE_NO_INTERCHANGE_POSSIBLE,
      new DataLocation(
        serviceJourneyInterchangeInfo.interchangeId(),
        serviceJourneyInterchangeInfo.filename(),
        null,
        null
      ),
      serviceJourneyInterchangeInfo.interchangeId(),
      serviceJourneyInterchangeInfo.fromJourneyRef().id(),
      serviceJourneyInterchangeInfo.toJourneyRef().id()
    );
  }

  static ValidationIssue validateServiceJourneyInterchangeInfo(
    ServiceJourneyInterchangeInfo serviceJourneyInterchangeInfo,
    List<LocalDateTime> fromJourneyActiveDates,
    List<LocalDateTime> toJourneyActiveDates,
    ServiceJourneyStop fromJourneyStop,
    ServiceJourneyStop toJourneyStop
  ) {
    if (fromJourneyActiveDates.isEmpty() || toJourneyActiveDates.isEmpty()) {
      return createNoInterchangePossibleValidationIssue(
        serviceJourneyInterchangeInfo
      );
    }

    LocalTime arrivalTime = fromJourneyStop.arrivalTime() == null
      ? fromJourneyStop.departureTime()
      : fromJourneyStop.arrivalTime();
    List<LocalDateTime> sortedFromJourneySortedLocalDateTimes =
      sortedLocalDateTimesForServiceJourneyAtStop(
        fromJourneyActiveDates,
        fromJourneyStop.arrivalDayOffset(),
        arrivalTime
      );

    List<LocalDateTime> sortedToJourneySortedLocalDateTimes =
      sortedLocalDateTimesForServiceJourneyAtStop(
        toJourneyActiveDates,
        toJourneyStop.departureDayOffset(),
        toJourneyStop.departureTime()
      );

    // If the latest arrival is later than the earliest departure, there will never be an interchange
    LocalDateTime earliestArrivalTime =
      sortedFromJourneySortedLocalDateTimes.get(0);
    LocalDateTime latestDepartureTime = sortedToJourneySortedLocalDateTimes.get(
      toJourneyActiveDates.size() - 1
    );
    if (earliestArrivalTime.isAfter(latestDepartureTime)) {
      return createNoInterchangePossibleValidationIssue(
        serviceJourneyInterchangeInfo
      );
    }

    Duration shortestActualWaitingTime =
      getShortestActualWaitingTimeForInterchange(
        sortedFromJourneySortedLocalDateTimes,
        sortedToJourneySortedLocalDateTimes
      );

    if (shortestActualWaitingTime.compareTo(waitingTimeWarningThreshold) > 0) {
      return new ValidationIssue(
        RULE_SERVICE_JOURNEYS_HAS_TOO_LONG_WAITING_TIME_WARNING,
        new DataLocation(
          serviceJourneyInterchangeInfo.interchangeId(),
          serviceJourneyInterchangeInfo.filename(),
          null,
          null
        ),
        serviceJourneyInterchangeInfo.interchangeId(),
        shortestActualWaitingTime.getSeconds(),
        waitingTimeWarningThreshold.getSeconds()
      );
    }
    return null;
  }

  public ValidationReport validate(ValidationReport validationReport) {
    LOGGER.info("Starting validation of interchange waiting times");

    String validationReportId = validationReport.getValidationReportId();
    List<ServiceJourneyInterchangeInfo> serviceJourneyInterchangeInfoList =
      netexDataRepository.serviceJourneyInterchangeInfos(validationReportId);

    Map<ServiceJourneyId, List<ServiceJourneyStop>> serviceJourneyStopsByServiceJourneyId =
      netexDataRepository.serviceJourneyStops(validationReportId);

    for (ServiceJourneyInterchangeInfo serviceJourneyInterchangeInfo : serviceJourneyInterchangeInfoList) {
      ServiceJourneyStop fromJourneyStop =
        getServiceJourneyStopByScheduledStopPointId(
          serviceJourneyStopsByServiceJourneyId.get(
            serviceJourneyInterchangeInfo.fromJourneyRef()
          ),
          serviceJourneyInterchangeInfo.fromStopPoint()
        );
      ServiceJourneyStop toJourneyStop =
        getServiceJourneyStopByScheduledStopPointId(
          serviceJourneyStopsByServiceJourneyId.get(
            serviceJourneyInterchangeInfo.toJourneyRef()
          ),
          serviceJourneyInterchangeInfo.toStopPoint()
        );

      if (fromJourneyStop == null || toJourneyStop == null) {
        // If the ScheduledStopPoint does not exist for the referred ServiceJourney, no interchange is possible.
        // However: we do not want to validate this here, because it is a follow-up error to validation errors already caught by StopPointsInVehicleJourneyValidator.
        break;
      }

      List<LocalDateTime> fromJourneyActiveDates =
        getActiveDatesForServiceJourney(
          validationReportId,
          serviceJourneyInterchangeInfo.fromJourneyRef()
        );

      List<LocalDateTime> toJourneyActiveDates =
        getActiveDatesForServiceJourney(
          validationReportId,
          serviceJourneyInterchangeInfo.toJourneyRef()
        );

      ValidationIssue validationIssue = validateServiceJourneyInterchangeInfo(
        serviceJourneyInterchangeInfo,
        fromJourneyActiveDates,
        toJourneyActiveDates,
        fromJourneyStop,
        toJourneyStop
      );
      if (validationIssue != null) {
        validationReport.addValidationReportEntry(
          createValidationReportEntry(validationIssue)
        );
      }
    }

    LOGGER.info("Completed validation of interchange waiting times");
    return validationReport;
  }
}
