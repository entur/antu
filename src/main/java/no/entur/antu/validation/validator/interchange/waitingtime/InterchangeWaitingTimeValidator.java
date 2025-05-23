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

/**
 * Validates that the ServiceJourneys referred to by a ServiceJourneyInterchange have possibilities
 * of making interchanges, and that the actual waiting time between service journeys do not exceed
 * the maximumWaitTime specified in ServiceJourneyInterchange.
 *
 * Chouette reference: 3-Interchange-8-1, 3-Interchange-8-2, 3-Interchange-10
 */
public class InterchangeWaitingTimeValidator extends AbstractDatasetValidator {

  // Closest resemblance to 3-Interchange-10, but validated differently
  static final ValidationRule RULE_NO_INTERCHANGE_POSSIBLE = new ValidationRule(
    "RULE_NO_INTERCHANGE_POSSIBLE",
    "Feeder and consumer vehicle journeys have no interchange possibilities",
    "ServiceJourneyInterchange %s has no interchange possibilities for vehicle journey %s and consumer vehicle journey %s",
    Severity.ERROR
  );

  static final ValidationRule RULE_SERVICE_JOURNEYS_HAS_TOO_LONG_WAITING_TIME_WARNING =
    new ValidationRule(
      "RULE_SERVICE_JOURNEYS_HAS_TOO_LONG_WAITING_TIME_WARNING",
      "Waiting time between feeder and consumer vehicle journeys exceed warning treshold",
      "Waiting time between service journeys in ServiceJourneyInterchange %s exceeds warning treshold",
      Severity.WARNING
    );

  private final NetexDataRepository netexDataRepository;

  // TODO: Inject NetexDataRepository instead. Ensure serviceJourneyIdToActiveDates is added to the interface in validator lib
  protected InterchangeWaitingTimeValidator(
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
      .orElseThrow();
  }

  static Duration getShortestActualWaitingTimeForInterchange(
    List<LocalDateTime> fromJourneyActiveDates,
    List<LocalDateTime> toJourneyActiveDates
  ) {
    // Her skal vi gi valideringsfeil kun hvis vi ikke finner en eneste dag hvor de to vil møtes.
    // Den skal også tillate overgang rundt midnatt. DVS at ventetid må være innenfor maximumWaitTime
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

  static ValidationIssue validateServiceJourneyInterchangeInfo(
    ServiceJourneyInterchangeInfo serviceJourneyInterchangeInfo,
    List<LocalDateTime> fromJourneyActiveDates,
    List<LocalDateTime> toJourneyActiveDates,
    ServiceJourneyStop fromJourneyStop,
    ServiceJourneyStop toJourneyStop
  ) {
    List<LocalDateTime> sortedFromJourneySortedLocalDateTimes =
      sortedLocalDateTimesForServiceJourneyAtStop(
        fromJourneyActiveDates,
        fromJourneyStop.arrivalDayOffset(),
        fromJourneyStop.arrivalTime()
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

    Duration shortestActualWaitingTime =
      getShortestActualWaitingTimeForInterchange(
        sortedFromJourneySortedLocalDateTimes,
        sortedToJourneySortedLocalDateTimes
      );

    // If the shortest actual waiting time is over 3 hours, we give a warning
    if (shortestActualWaitingTime.compareTo(Duration.ofHours(2)) > 0) {
      return new ValidationIssue(
        RULE_SERVICE_JOURNEYS_HAS_TOO_LONG_WAITING_TIME_WARNING,
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
    return null;
  }

  // TODO: if guaranteed is set to false, we should give the user a warning
  public ValidationReport validate(ValidationReport validationReport) {
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
    return validationReport;
  }
}
