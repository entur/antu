package no.entur.antu.validation.validator.interchange.waitingtime;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import no.entur.antu.netexdata.DefaultNetexDataRepository;
import org.apache.commons.lang3.time.DateUtils;
import org.entur.netex.validation.validator.*;
import org.entur.netex.validation.validator.jaxb.NetexDataRepository;
import org.entur.netex.validation.validator.model.ScheduledStopPointId;
import org.entur.netex.validation.validator.model.ServiceJourneyId;
import org.entur.netex.validation.validator.model.ServiceJourneyInterchangeInfo;
import org.entur.netex.validation.validator.model.ServiceJourneyStop;

import static org.joda.time.DateTimeConstants.MILLIS_PER_DAY;
import static org.joda.time.DateTimeConstants.SECONDS_PER_DAY;

public class InterchangeWaitingTimeValidator extends AbstractDatasetValidator {

  static final ValidationRule RULE_SERVICE_JOURNEYS_HAS_SHARED_ACTIVE_DATE =
    new ValidationRule(
      "RULE_SERVICE_JOURNEYS_HAS_SHARED_ACTIVE_DATE",
      "Feeder and consumer vehicle journeys have no shared active dates",
      "ServiceJourneyInterchange %s has no shared date for feeder vehicle journey %s and consumer vehicle journey %s",
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

  private boolean serviceJourneysHaveSharedActiveDates(
    String validationReportId,
    ServiceJourneyId fromJourneyRef,
    ServiceJourneyId toJourneyRef,
    int dayOffsetDiff
  ) {
    // TODO: Is this too simple? Does it need to use day offsets aswell, as in Chouette?
    List<LocalDateTime> fromJourneyActiveDates =
      getActiveDatesForServiceJourney(validationReportId, fromJourneyRef);
    List<LocalDateTime> toJourneyActiveDates = getActiveDatesForServiceJourney(
      validationReportId,
      toJourneyRef
    );
    return fromJourneyActiveDates
      .stream()
      .anyMatch(toJourneyActiveDates::contains);
  }

  private LocalTime arrivalTimeFromScheduledStopPointId(List<ServiceJourneyStop> stops, ScheduledStopPointId scheduledStopPointId) {
    return stops.stream().filter(serviceJourneyStop -> serviceJourneyStop.scheduledStopPointId().equals(scheduledStopPointId)).findFirst().orElseThrow().arrivalTime();
  }

  private LocalTime departureTimeFromScheduledStopPointId(List<ServiceJourneyStop> stops, ScheduledStopPointId scheduledStopPointId) {
    return stops.stream().filter(serviceJourneyStop -> serviceJourneyStop.scheduledStopPointId().equals(scheduledStopPointId)).findFirst().orElseThrow().departureTime();
  }

  private int arrivalDayOffsetFromScheduledStopPointId(List<ServiceJourneyStop> stops, ScheduledStopPointId scheduledStopPointId) {
    return stops.stream().filter(serviceJourneyStop -> serviceJourneyStop.scheduledStopPointId().equals(scheduledStopPointId)).findFirst().orElseThrow().arrivalDayOffset();
  }

  private int departureDayOffsetFromScheduledStopPointId(List<ServiceJourneyStop> stops, ScheduledStopPointId scheduledStopPointId) {
    return stops.stream().filter(serviceJourneyStop -> serviceJourneyStop.scheduledStopPointId().equals(scheduledStopPointId)).findFirst().orElseThrow().departureDayOffset();
  }


  private boolean hasAcceptableWaitingTimes(List<LocalDateTime> arrivalTimes, List<LocalDateTime> departureTimes, Duration maximumWaitingTime) {
    int departureTimeIndex = 0;
    for (LocalDateTime arrivalTime : arrivalTimes) {
      while (departureTimeIndex < departureTimes.size()) {
        LocalDateTime departureTime = departureTimes.get(departureTimeIndex);
        if (departureTime.isBefore(arrivalTime)) {
          departureTimeIndex++;
          continue;
        }

        Duration actualWaitingTime = Duration.between(arrivalTime, departureTime);

        // If we find a waiting time which is equal or smaller than maximumWaitingTime, we have an acceptable waiting time
        if (actualWaitingTime.compareTo(maximumWaitingTime) <= 0) {
          return true;
        }

        // If the actual waiting time exceeds the maximumWaitingTime, all subsequent will also be above maximumWaitingTime if
        // the lists are sorted, so we skip the rest and move on to the next arrivalTime
        if (actualWaitingTime.compareTo(maximumWaitingTime) > 0) {
          break;
        }
      }
    }
    return false;
  }




//  private boolean hasSharedActiveDates(List<LocalDateTime> fromJourneyActiveDates, List<LocalDateTime> toJourneyActiveDates, int dayDifference) {
//    for (LocalDateTime consumerActiveDate : toJourneyActiveDates) {
//      LocalDateTime dateAdjustedForDayDifference = consumerActiveDate.minusDays(dayDifference);
//      if (fromJourneyActiveDates.contains(dateAdjustedForDayDifference)) {
//        return true;
//      }
//    }
//    return false;
//  }
//
//  private Duration calculateWaitingTime(LocalTime arrivalTime, LocalTime departureTime, int arrivalDayOffset, int departureDayOffset, LocalDateTime activeDateOfArrival, LocalDateTime activeDateOfDeparture) {
//    LocalDateTime actualArrivalDate = activeDateOfArrival.plusDays(arrivalDayOffset);
//    LocalDateTime actualDepartureDate = activeDateOfDeparture.plusDays(departureDayOffset);
//    LocalDateTime actualArrivalDateWithTime = actualArrivalDate.plusNanos(arrivalTime.toNanoOfDay());
//    LocalDateTime actualDepartureDateWithTime = actualDepartureDate.plusNanos(departureTime.toNanoOfDay());
//    return Duration.between(actualArrivalDateWithTime, actualDepartureDateWithTime);
//  }

  private boolean hasSharedActiveDates(List<LocalDateTime> fromJourneyActiveDates, List<LocalDateTime> toJourneyActiveDates, Duration maximumWaitingTime) {
    // Her skal vi gi valideringsfeil kun hvis vi ikke finner en eneste dag hvor de to vil møtes.
    // Den skal også tillate overgang rundt midnatt. DVS at ventetid må være innenfor maximumWaitTime
    for (LocalDateTime fromJourneyActiveDateTime : fromJourneyActiveDates) {
      int toJourneyActiveDateIndex = 0;
      while (toJourneyActiveDateIndex < toJourneyActiveDates.size()) {
        LocalDateTime toJourneyActiveDateTime = toJourneyActiveDates.get(toJourneyActiveDateIndex);
        if (toJourneyActiveDateTime.isBefore(fromJourneyActiveDateTime)) {
          toJourneyActiveDateIndex++;
          continue;
        }

        Duration actualWaitTime = Duration.between(fromJourneyActiveDateTime, toJourneyActiveDateTime);
        if (actualWaitTime.compareTo(maximumWaitingTime) <= 0) {
          return true;
        }
        break;
      }
    }
    return false;
  }

  public ValidationReport validate(ValidationReport validationReport) {
    String validationReportId = validationReport.getValidationReportId();
    List<ServiceJourneyInterchangeInfo> serviceJourneyInterchangeInfoList =
            netexDataRepository.serviceJourneyInterchangeInfos(validationReportId);
    Map<ServiceJourneyId, List<ServiceJourneyStop>> serviceJourneyStopsByServiceJourneyId = netexDataRepository.serviceJourneyStops(validationReportId);

    for (ServiceJourneyInterchangeInfo serviceJourneyInterchangeInfo : serviceJourneyInterchangeInfoList) {
      List<ServiceJourneyStop> fromJourneyStops = serviceJourneyStopsByServiceJourneyId.get(serviceJourneyInterchangeInfo.fromJourneyRef());
      List<ServiceJourneyStop> toJourneyStops = serviceJourneyStopsByServiceJourneyId.get(serviceJourneyInterchangeInfo.toJourneyRef());


      int arrivalDayOffset = arrivalDayOffsetFromScheduledStopPointId(fromJourneyStops, serviceJourneyInterchangeInfo.fromStopPoint());
      int departureDayOffset = departureDayOffsetFromScheduledStopPointId(toJourneyStops, serviceJourneyInterchangeInfo.toStopPoint());
      LocalTime arrivalTime = arrivalTimeFromScheduledStopPointId(fromJourneyStops, serviceJourneyInterchangeInfo.fromStopPoint());
      LocalTime departureTime = departureTimeFromScheduledStopPointId(toJourneyStops, serviceJourneyInterchangeInfo.toStopPoint());

      // The active dates' origin are from the start of the Service Journey. To get the actual arrival time on the correct stop,
      // we need to add the arrival day offset to the active date, along with the value of arrivalTime.
      List<LocalDateTime> fromJourneyActiveDates =
              getActiveDatesForServiceJourney(validationReportId, serviceJourneyInterchangeInfo.fromJourneyRef()).stream().map(localDateTime ->
                localDateTime.plusDays(arrivalDayOffset).plusNanos(arrivalTime.toNanoOfDay())
              ).sorted().toList();
      List<LocalDateTime> toJourneyActiveDates = getActiveDatesForServiceJourney(
              validationReportId,
              serviceJourneyInterchangeInfo.toJourneyRef()
      ).stream().map(localDateTime -> localDateTime.plusDays(departureDayOffset).plusNanos(departureTime.toNanoOfDay())).sorted().toList();

      Duration maximumWaitingTime = serviceJourneyInterchangeInfo.maximumWaitTime().isPresent() ? serviceJourneyInterchangeInfo.maximumWaitTime().get() : Duration.ZERO;

      // Her skal vi gi valideringsfeil kun hvis vi ikke finner en eneste dag hvor de to vil møtes.
      // Den skal også tillate overgang rundt midnatt. DVS at ventetid må være innenfor maximumWaitTime
      boolean doesMeet = hasSharedActiveDates(fromJourneyActiveDates, toJourneyActiveDates, maximumWaitingTime);
      if (!doesMeet) {
        validationReport.addValidationReportEntry(
                createValidationReportEntry(
                        new ValidationIssue(
                                RULE_SERVICE_JOURNEYS_HAS_SHARED_ACTIVE_DATE,
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
    return validationReport;
  }

//  public ValidationReport validate2(ValidationReport validationReport) {
//    String validationReportId = validationReport.getValidationReportId();
//    List<ServiceJourneyInterchangeInfo> serviceJourneyInterchangeInfoList =
//            netexDataRepository.serviceJourneyInterchangeInfos(validationReportId);
//    Map<ServiceJourneyId, List<ServiceJourneyStop>> serviceJourneyStopsByServiceJourneyId = netexDataRepository.serviceJourneyStops(validationReportId);
//
//    List<ServiceJourneyInterchangeInfo> serviceJourneyInterchangesWithSharedActiveDates = new ArrayList<>();
//    List<ServiceJourneyInterchangeInfo> serviceJourneyInterchangesWithoutSharedActiveDates = new ArrayList<>();
//
//    for (ServiceJourneyInterchangeInfo serviceJourneyInterchangeInfo : serviceJourneyInterchangeInfoList) {
//      List<ServiceJourneyStop> fromJourneyStops = serviceJourneyStopsByServiceJourneyId.get(serviceJourneyInterchangeInfo.fromJourneyRef());
//      List<ServiceJourneyStop> toJourneyStops = serviceJourneyStopsByServiceJourneyId.get(serviceJourneyInterchangeInfo.toJourneyRef());
//
//      int arrivalDayOffset = arrivalDayOffsetFromScheduledStopPointId(fromJourneyStops, serviceJourneyInterchangeInfo.fromStopPoint());
//      int departureDayOffset = departureDayOffsetFromScheduledStopPointId(toJourneyStops, serviceJourneyInterchangeInfo.toStopPoint());
//      LocalTime arrivalTime = arrivalTimeFromScheduledStopPointId(fromJourneyStops, serviceJourneyInterchangeInfo.fromStopPoint());
//      LocalTime departureTime = departureTimeFromScheduledStopPointId(toJourneyStops, serviceJourneyInterchangeInfo.toStopPoint());
//
//
//
//      int dayDifference = departureDayOffset - arrivalDayOffset;
//
//      List<LocalDateTime> fromJourneyActiveDates =
//              getActiveDatesForServiceJourney(validationReportId, serviceJourneyInterchangeInfo.fromJourneyRef());
//      List<LocalDateTime> toJourneyActiveDates = getActiveDatesForServiceJourney(
//              validationReportId,
//              serviceJourneyInterchangeInfo.toJourneyRef()
//      );
//
//      if (hasSharedActiveDates(fromJourneyActiveDates, toJourneyActiveDates, dayDifference)) {
//        serviceJourneyInterchangesWithSharedActiveDates.add(serviceJourneyInterchangeInfo);
//      } else {
//        serviceJourneyInterchangesWithoutSharedActiveDates.add(serviceJourneyInterchangeInfo);
//      }
//    }
//
//    for (ServiceJourneyInterchangeInfo serviceJourneyInterchangeWithoutSharedActiveDate : serviceJourneyInterchangesWithoutSharedActiveDates) {
//      validationReport.addValidationReportEntry(
//              createValidationReportEntry(
//                      new ValidationIssue(
//                              RULE_SERVICE_JOURNEYS_HAS_SHARED_ACTIVE_DATE,
//                              new DataLocation(
//                                      serviceJourneyInterchangeWithoutSharedActiveDate.interchangeId(),
//                                      serviceJourneyInterchangeWithoutSharedActiveDate.filename(),
//                                      null,
//                                      null
//                              ),
//                              serviceJourneyInterchangeWithoutSharedActiveDate.interchangeId(),
//                              serviceJourneyInterchangeWithoutSharedActiveDate.fromJourneyRef().id(),
//                              serviceJourneyInterchangeWithoutSharedActiveDate.toJourneyRef().id()
//                      )
//              )
//      );
//    }
//
//    for (ServiceJourneyInterchangeInfo serviceJourneyInterchangeWithSharedActiveDate : serviceJourneyInterchangesWithSharedActiveDates) {
//      LocalTime arrivalTime =  netexDataRepository.serviceJourneyStops(validationReportId).get(serviceJourneyInterchangeWithSharedActiveDate.fromJourneyRef()).stream().filter(serviceJourneyStop -> serviceJourneyStop.scheduledStopPointId() == serviceJourneyInterchangeWithSharedActiveDate.fromStopPoint()).findFirst().orElseThrow().arrivalTime();
//      LocalTime departureTime =  netexDataRepository.serviceJourneyStops(validationReportId).get(serviceJourneyInterchangeWithSharedActiveDate.toJourneyRef()).stream().filter(serviceJourneyStop -> serviceJourneyStop.scheduledStopPointId() == serviceJourneyInterchangeWithSharedActiveDate.toStopPoint()).findFirst().orElseThrow().departureTime();
//
//
//      int actualWaitingTimeInSeconds = departureTime.toSecondOfDay() - arrivalTime.toSecondOfDay();
//      if (actualWaitingTimeInSeconds < 0) {
//        actualWaitingTimeInSeconds += SECONDS_PER_DAY;
//      }
//
//      // If maximumWaitTime does not exist, it is assumed that consumer will wait for feeder regardless of delay
//      Optional<Duration> maximumWaitingTimeOptional = serviceJourneyInterchangeWithSharedActiveDate.maximumWaitTime();
//      if (maximumWaitingTimeOptional.isPresent()) {
//        Duration maximumWaitingTime = maximumWaitingTimeOptional.get();
//        Duration errorTresholdWaitingTime = maximumWaitingTime.multipliedBy(3);
//        Duration actualWaitingTimeAsDuration = Duration.ofSeconds(actualWaitingTimeInSeconds);
//
//        if (actualWaitingTimeAsDuration.compareTo(errorTresholdWaitingTime) > 0) {
//          // TODO: Give an error due to too long waitingTime
//        }
//        else if (actualWaitingTimeAsDuration.compareTo(maximumWaitingTime) > 0) {
//          // TODO: Give a warning due to too long waitingTime
//        }
//
//      }
//    }
//
//    return validationReport;
//  }
}
