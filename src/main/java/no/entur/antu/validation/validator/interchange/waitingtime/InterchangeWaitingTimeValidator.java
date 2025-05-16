package no.entur.antu.validation.validator.interchange.waitingtime;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import no.entur.antu.netexdata.DefaultNetexDataRepository;
import org.apache.commons.lang3.time.DateUtils;
import org.entur.netex.validation.validator.*;
import org.entur.netex.validation.validator.jaxb.NetexDataRepository;
import org.entur.netex.validation.validator.model.ScheduledStopPointId;
import org.entur.netex.validation.validator.model.ServiceJourneyId;
import org.entur.netex.validation.validator.model.ServiceJourneyInterchangeInfo;
import org.entur.netex.validation.validator.model.ServiceJourneyStop;

public class InterchangeWaitingTimeValidator extends AbstractDatasetValidator {

  static final ValidationRule RULE_SERVICE_JOURNEYS_HAS_SHARED_ACTIVE_DATE =
    new ValidationRule(
      "RULE_SERVICE_JOURNEYS_HAS_SHARED_ACTIVE_DATE",
      "Feeder and consumer vehicle journeys have no shared active dates",
      "ServiceJourneyInterchange %s has no shared date for feeder vehicle journey %s and consumer vehicle journey %s",
      Severity.ERROR
    );

  private final DefaultNetexDataRepository netexDataRepository;

  // TODO: Inject NetexDataRepository instead. Ensure serviceJourneyIdToActiveDates is added to the interface in validator lib
  protected InterchangeWaitingTimeValidator(
    ValidationReportEntryFactory validationReportEntryFactory,
    DefaultNetexDataRepository netexDataRepository
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
      .get(serviceJourneyId.id());
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

  private int arrivalDayOffsetFromScheduledStopPointId(List<ServiceJourneyStop> stops, ScheduledStopPointId scheduledStopPointId) {
    return stops.stream().filter(serviceJourneyStop -> serviceJourneyStop.scheduledStopPointId() == scheduledStopPointId).findFirst().orElseThrow().arrivalDayOffset();
  }

  private int departureDayOffsetFromScheduledStopPointId(List<ServiceJourneyStop> stops, ScheduledStopPointId scheduledStopPointId) {
    return stops.stream().filter(serviceJourneyStop -> serviceJourneyStop.scheduledStopPointId() == scheduledStopPointId).findFirst().orElseThrow().departureDayOffset();
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

  @Override
  public ValidationReport validate(ValidationReport validationReport) {
    String validationReportId = validationReport.getValidationReportId();



    List<ServiceJourneyInterchangeInfo> serviceJourneyInterchangeInfoList =
      netexDataRepository.serviceJourneyInterchangeInfos(validationReportId);
    Map<ServiceJourneyId, List<ServiceJourneyStop>> serviceJourneyStopsByServiceJourneyId = netexDataRepository.serviceJourneyStops(validationReportId);

//    netexDataRepository.serviceJourneyStops(validationReportId).get(0).stream().forEach(entry -> {entry.});

    serviceJourneyInterchangeInfoList
      .stream()
      .filter(serviceJourneyInterchangeInfo -> {
        List<ServiceJourneyStop> fromJourneyStops = serviceJourneyStopsByServiceJourneyId.get(serviceJourneyInterchangeInfo.fromJourneyRef());
        List<ServiceJourneyStop> toJourneyStops = serviceJourneyStopsByServiceJourneyId.get(serviceJourneyInterchangeInfo.toJourneyRef());
        int arrivalDayOffset = arrivalDayOffsetFromScheduledStopPointId(fromJourneyStops, serviceJourneyInterchangeInfo.fromStopPoint());
        int departureDayOffset = departureDayOffsetFromScheduledStopPointId(toJourneyStops, serviceJourneyInterchangeInfo.toStopPoint());



        serviceJourneyStopsByServiceJourneyId.get(serviceJourneyInterchangeInfo.fromJourneyRef()).stream().filter(serviceJourneyStop -> serviceJourneyStop.scheduledStopPointId() == serviceJourneyInterchangeInfo.fromStopPoint()).findFirst().orElseThrow().arrivalDayOffset();;

        int dayOffsetDiff = departureDayOffset - arrivalDayOffset;
//fromJourneyStops.get(0).


//          int dayOffsetDiff = consumerVJAtStop.getDepartureDayOffset() - feederVJAtStop.getArrivalDayOffset();
//          long msWait = TimeUtil.toMillisecondsOfDay(consumerVJAtStop.getDepartureTime())- TimeUtil.toMillisecondsOfDay(feederVJAtStop.getArrivalTime());
//          if (msWait < 0) {
//              msWait = DateUtils.MILLIS_PER_DAY + msWait;
//              dayOffsetDiff--;
//          }


            return !serviceJourneysHaveSharedActiveDates(
              validationReportId,
              serviceJourneyInterchangeInfo.fromJourneyRef(),
              serviceJourneyInterchangeInfo.toJourneyRef(),
                    dayOffsetDiff
            );
              }
      )
      .forEach(serviceJourneyInterchangeInfo -> {
        createValidationReportEntry(
          new ValidationIssue(
            RULE_SERVICE_JOURNEYS_HAS_SHARED_ACTIVE_DATE,
            new DataLocation(
              serviceJourneyInterchangeInfo.interchangeId(),
              serviceJourneyInterchangeInfo.filename(),
              null,
              null
            )
          )
        );
      });

    return validationReport;
  }



  private boolean hasSharedActiveDates(List<LocalDateTime> fromJourneyActiveDates, List<LocalDateTime> toJourneyActiveDates, int dayDifference) {
    for (LocalDateTime consumerActiveDate : toJourneyActiveDates) {
      LocalDateTime dateAdjustedForDayDifference = consumerActiveDate.minusDays(dayDifference);
      if (fromJourneyActiveDates.contains(dateAdjustedForDayDifference)) {
        return true;
      }
    }
    return false;
  }



  public ValidationReport validate2(ValidationReport validationReport) {
    String validationReportId = validationReport.getValidationReportId();
    List<ServiceJourneyInterchangeInfo> serviceJourneyInterchangeInfoList =
            netexDataRepository.serviceJourneyInterchangeInfos(validationReportId);
    Map<ServiceJourneyId, List<ServiceJourneyStop>> serviceJourneyStopsByServiceJourneyId = netexDataRepository.serviceJourneyStops(validationReportId);

    List<ServiceJourneyInterchangeInfo> serviceJourneyInterchangesWithSharedActiveDates = new ArrayList<>();
    List<ServiceJourneyInterchangeInfo> serviceJourneyInterchangesWithoutSharedActiveDates = new ArrayList<>();

    serviceJourneyInterchangeInfoList.forEach((serviceJourneyInterchangeInfo) -> {
      List<ServiceJourneyStop> fromJourneyStops = serviceJourneyStopsByServiceJourneyId.get(serviceJourneyInterchangeInfo.fromJourneyRef());
      List<ServiceJourneyStop> toJourneyStops = serviceJourneyStopsByServiceJourneyId.get(serviceJourneyInterchangeInfo.toJourneyRef());

      int arrivalDayOffset = arrivalDayOffsetFromScheduledStopPointId(fromJourneyStops, serviceJourneyInterchangeInfo.fromStopPoint());
      int departureDayOffset = departureDayOffsetFromScheduledStopPointId(toJourneyStops, serviceJourneyInterchangeInfo.toStopPoint());
      int dayDifference = departureDayOffset - arrivalDayOffset;

      List<LocalDateTime> fromJourneyActiveDates =
              getActiveDatesForServiceJourney(validationReportId, serviceJourneyInterchangeInfo.fromJourneyRef());
      List<LocalDateTime> toJourneyActiveDates = getActiveDatesForServiceJourney(
              validationReportId,
              serviceJourneyInterchangeInfo.toJourneyRef()
      );

      if (hasSharedActiveDates(fromJourneyActiveDates, toJourneyActiveDates, dayDifference)) {
        serviceJourneyInterchangesWithSharedActiveDates.add(serviceJourneyInterchangeInfo);
      } else {
        serviceJourneyInterchangesWithoutSharedActiveDates.add(serviceJourneyInterchangeInfo);
      }
    });

    for (ServiceJourneyInterchangeInfo serviceJourneyInterchangeWithoutSharedActiveDate : serviceJourneyInterchangesWithoutSharedActiveDates) {
      validationReport.addValidationReportEntry(
              createValidationReportEntry(
                      new ValidationIssue(
                              RULE_SERVICE_JOURNEYS_HAS_SHARED_ACTIVE_DATE,
                              new DataLocation(
                                      serviceJourneyInterchangeWithoutSharedActiveDate.interchangeId(),
                                      serviceJourneyInterchangeWithoutSharedActiveDate.filename(),
                                      null,
                                      null
                              )
                      )
              )
      );
    }

    for (ServiceJourneyInterchangeInfo serviceJourneyInterchangeWithSharedActiveDate : serviceJourneyInterchangesWithSharedActiveDates) {
      // TODO: Validate waiting time
      List<ServiceJourneyStop> fromJourneyStops = serviceJourneyStopsByServiceJourneyId.get(serviceJourneyInterchangeWithSharedActiveDate.fromJourneyRef());
      List<ServiceJourneyStop> toJourneyStops = serviceJourneyStopsByServiceJourneyId.get(serviceJourneyInterchangeWithSharedActiveDate.toJourneyRef());

      fromJourneyStops.stream().forEach(serviceJourneyStop -> {
        serviceJourneyStop.arrivalTime() - serviceJourneyStop.departureTime()
      });

    }

    return validationReport;
  }
}
