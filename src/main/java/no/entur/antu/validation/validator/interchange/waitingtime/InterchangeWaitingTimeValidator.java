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
    "ServiceJourneyInterchange %s have no interchange possibilities for vehicle journey %s and consumer vehicle journey %s",
    Severity.ERROR
  );

  static final ValidationRule RULE_SERVICE_JOURNEYS_HAS_TOO_LONG_WAITING_TIME_WARNING =
    new ValidationRule(
      "RULE_SERVICE_JOURNEYS_HAS_TOO_LONG_WAITING_TIME_WARNING",
      "Waiting time between feeder and consumer vehicle journeys exceed warning treshold",
      "Waiting time between service journeys in ServiceJourneyInterchange %s exceeds warning treshold",
      Severity.WARNING
    );

  static final ValidationRule RULE_SERVICE_JOURNEYS_HAS_TOO_LONG_WAITING_TIME_ERROR =
    new ValidationRule(
      "RULE_SERVICE_JOURNEYS_HAS_TOO_LONG_WAITING_TIME_ERROR",
      "Waiting time between feeder and consumer vehicle journeys exceed error treshold",
      "Waiting time between service journeys in ServiceJourneyInterchange %s exceeds error treshold",
      Severity.ERROR
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

  private LocalTime arrivalTimeFromScheduledStopPointId(
    List<ServiceJourneyStop> stops,
    ScheduledStopPointId scheduledStopPointId
  ) {
    return stops
      .stream()
      .filter(serviceJourneyStop ->
        serviceJourneyStop.scheduledStopPointId().equals(scheduledStopPointId)
      )
      .findFirst()
      .orElseThrow()
      .arrivalTime();
  }

  private LocalTime departureTimeFromScheduledStopPointId(
    List<ServiceJourneyStop> stops,
    ScheduledStopPointId scheduledStopPointId
  ) {
    return stops
      .stream()
      .filter(serviceJourneyStop ->
        serviceJourneyStop.scheduledStopPointId().equals(scheduledStopPointId)
      )
      .findFirst()
      .orElseThrow()
      .departureTime();
  }

  private int arrivalDayOffsetFromScheduledStopPointId(
    List<ServiceJourneyStop> stops,
    ScheduledStopPointId scheduledStopPointId
  ) {
    return stops
      .stream()
      .filter(serviceJourneyStop ->
        serviceJourneyStop.scheduledStopPointId().equals(scheduledStopPointId)
      )
      .findFirst()
      .orElseThrow()
      .arrivalDayOffset();
  }

  private int departureDayOffsetFromScheduledStopPointId(
    List<ServiceJourneyStop> stops,
    ScheduledStopPointId scheduledStopPointId
  ) {
    return stops
      .stream()
      .filter(serviceJourneyStop ->
        serviceJourneyStop.scheduledStopPointId().equals(scheduledStopPointId)
      )
      .findFirst()
      .orElseThrow()
      .departureDayOffset();
  }

  public Duration getShortestActualWaitingTimeForInterchange(
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

  // TODO: if guaranteed is set to false, we should give the user a warning
  public ValidationReport validate(ValidationReport validationReport) {
    String validationReportId = validationReport.getValidationReportId();
    List<ServiceJourneyInterchangeInfo> serviceJourneyInterchangeInfoList =
      netexDataRepository.serviceJourneyInterchangeInfos(validationReportId);
    Map<ServiceJourneyId, List<ServiceJourneyStop>> serviceJourneyStopsByServiceJourneyId =
      netexDataRepository.serviceJourneyStops(validationReportId);

    for (ServiceJourneyInterchangeInfo serviceJourneyInterchangeInfo : serviceJourneyInterchangeInfoList) {
      List<ServiceJourneyStop> fromJourneyStops =
        serviceJourneyStopsByServiceJourneyId.get(
          serviceJourneyInterchangeInfo.fromJourneyRef()
        );
      List<ServiceJourneyStop> toJourneyStops =
        serviceJourneyStopsByServiceJourneyId.get(
          serviceJourneyInterchangeInfo.toJourneyRef()
        );

      int arrivalDayOffset = arrivalDayOffsetFromScheduledStopPointId(
        fromJourneyStops,
        serviceJourneyInterchangeInfo.fromStopPoint()
      );
      int departureDayOffset = departureDayOffsetFromScheduledStopPointId(
        toJourneyStops,
        serviceJourneyInterchangeInfo.toStopPoint()
      );
      LocalTime arrivalTime = arrivalTimeFromScheduledStopPointId(
        fromJourneyStops,
        serviceJourneyInterchangeInfo.fromStopPoint()
      );
      LocalTime departureTime = departureTimeFromScheduledStopPointId(
        toJourneyStops,
        serviceJourneyInterchangeInfo.toStopPoint()
      );

      // The active dates' origin are from the start of the Service Journey. To get the actual arrival time on the correct stop,
      // we need to add the arrival day offset to the active date, along with the value of arrivalTime.
      List<LocalDateTime> fromJourneyActiveDates =
        getActiveDatesForServiceJourney(
          validationReportId,
          serviceJourneyInterchangeInfo.fromJourneyRef()
        )
          .stream()
          .map(localDateTime ->
            localDateTime
              .plusDays(arrivalDayOffset)
              .plusNanos(arrivalTime.toNanoOfDay())
          )
          .sorted()
          .toList();
      List<LocalDateTime> toJourneyActiveDates =
        getActiveDatesForServiceJourney(
          validationReportId,
          serviceJourneyInterchangeInfo.toJourneyRef()
        )
          .stream()
          .map(localDateTime ->
            localDateTime
              .plusDays(departureDayOffset)
              .plusNanos(departureTime.toNanoOfDay())
          )
          .sorted()
          .toList();

      // If the latest arrival is later than the earliest departure, there will never be an interchange
      LocalDateTime earliestArrivalTime = fromJourneyActiveDates.get(0);
      LocalDateTime latestDepartureTime = toJourneyActiveDates.get(
        toJourneyActiveDates.size() - 1
      );
      if (earliestArrivalTime.isAfter(latestDepartureTime)) {
        validationReport.addValidationReportEntry(
          createValidationReportEntry(
            new ValidationIssue(
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
            )
          )
        );
        // No reason to check waiting times when the service journeys never interchange
        continue;
      }

      Duration maximumWaitingTime = serviceJourneyInterchangeInfo
          .maximumWaitTime()
          .isPresent()
        ? serviceJourneyInterchangeInfo.maximumWaitTime().get()
        : null;
      // if maximumWaitTime does not exist, it is assumed that consumer will wait for feeder regardless of delay.
      if (maximumWaitingTime != null) {
        Duration shortestActualWaitingTime =
          getShortestActualWaitingTimeForInterchange(
            fromJourneyActiveDates,
            toJourneyActiveDates
          );
        Duration errorTresholdWaitingTime = maximumWaitingTime.multipliedBy(3);

        // If the shortest actual waiting time is 3 times the maximum waiting time, give an error
        if (
          shortestActualWaitingTime.compareTo(errorTresholdWaitingTime) >= 0
        ) {
          validationReport.addValidationReportEntry(
            createValidationReportEntry(
              new ValidationIssue(
                RULE_SERVICE_JOURNEYS_HAS_TOO_LONG_WAITING_TIME_ERROR,
                new DataLocation(
                  serviceJourneyInterchangeInfo.interchangeId(),
                  serviceJourneyInterchangeInfo.filename(),
                  null,
                  null
                ),
                serviceJourneyInterchangeInfo.interchangeId(),
                serviceJourneyInterchangeInfo.fromJourneyRef().id(),
                serviceJourneyInterchangeInfo.toJourneyRef().id()
              )
            )
          );
        }
        // If shortest actual waiting time is above maximum waiting time, but less than error treshold, give a warning
        else if (shortestActualWaitingTime.compareTo(maximumWaitingTime) > 0) {
          validationReport.addValidationReportEntry(
            createValidationReportEntry(
              new ValidationIssue(
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
              )
            )
          );
        }
      }
    }
    return validationReport;
  }
}
