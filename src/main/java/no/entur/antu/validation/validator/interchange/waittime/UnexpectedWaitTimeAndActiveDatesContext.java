package no.entur.antu.validation.validator.interchange.waittime;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.entur.netex.validation.validator.jaxb.NetexDataRepository;
import org.entur.netex.validation.validator.model.ActiveDates;
import org.entur.netex.validation.validator.model.ActiveDatesId;
import org.entur.netex.validation.validator.model.DayTypeId;
import org.entur.netex.validation.validator.model.OperatingDayId;
import org.entur.netex.validation.validator.model.ScheduledStopPointId;
import org.entur.netex.validation.validator.model.ServiceJourneyId;
import org.entur.netex.validation.validator.model.ServiceJourneyInterchangeInfo;
import org.entur.netex.validation.validator.model.ServiceJourneyStop;

public record UnexpectedWaitTimeAndActiveDatesContext(
  ServiceJourneyInterchangeInfo serviceJourneyInterchangeInfo,
  // ServiceJourneyStop at the fromStopPoint in fromJourneyRef from Cache
  ServiceJourneyStop fromServiceJourneyStop,
  // ServiceJourneySStop at the toStopPoint in toJourneyRef from Cache
  ServiceJourneyStop toServiceJourneyStop,
  // Active dates for the fromJourneyRef from Cache
  List<LocalDate> fromServiceJourneyActiveDates,
  // Active dates for the toJourneyRef from Cache
  List<LocalDate> toServiceJourneyActiveDates
) {
  public static class Builder {

    private final String validationReportId;
    private final NetexDataRepository netexDataRepository;
    private Map<ServiceJourneyId, List<ServiceJourneyStop>> serviceJourneyIdListMap;
    private Map<ServiceJourneyId, List<DayTypeId>> serviceJourneyDayTypesMap;
    private Map<ActiveDatesId, ActiveDates> activeDatesMap;
    private Map<ServiceJourneyId, List<OperatingDayId>> serviceJourneyOperatingDaysMap;

    public Builder(
      String validationReportId,
      NetexDataRepository netexDataRepository
    ) {
      this.validationReportId = validationReportId;
      this.netexDataRepository = netexDataRepository;
    }

    public UnexpectedWaitTimeAndActiveDatesContext.Builder primeCache() {
      serviceJourneyIdListMap =
        netexDataRepository.serviceJourneyStops(validationReportId);
      serviceJourneyDayTypesMap =
        netexDataRepository.serviceJourneyDayTypes(validationReportId);
      activeDatesMap = netexDataRepository.activeDates(validationReportId);
      serviceJourneyOperatingDaysMap =
        netexDataRepository.serviceJourneyOperatingDays(validationReportId);
      return this;
    }

    public UnexpectedWaitTimeAndActiveDatesContext build(
      ServiceJourneyInterchangeInfo serviceJourneyInterchangeInfo
    ) {
      return new UnexpectedWaitTimeAndActiveDatesContext(
        serviceJourneyInterchangeInfo,
        serviceJourneyStopAtScheduleStopPoint(
          serviceJourneyInterchangeInfo.fromJourneyRef(),
          serviceJourneyInterchangeInfo.fromStopPoint()
        ),
        serviceJourneyStopAtScheduleStopPoint(
          serviceJourneyInterchangeInfo.toJourneyRef(),
          serviceJourneyInterchangeInfo.toStopPoint()
        ),
        activeDatesForServiceJourney(
          serviceJourneyInterchangeInfo.fromJourneyRef()
        ),
        activeDatesForServiceJourney(
          serviceJourneyInterchangeInfo.toJourneyRef()
        )
      );
    }

    private List<LocalDate> activeDatesForServiceJourney(
      ServiceJourneyId serviceJourneyId
    ) {
      List<LocalDate> activeDateOfDayTypes = Optional
        .ofNullable(serviceJourneyId)
        .map(serviceJourneyDayTypesMap::get)
        .map(dayTypeIds ->
          dayTypeIds
            .stream()
            .map(activeDatesMap::get)
            .map(ActiveDates::dates)
            .flatMap(List::stream)
            .toList()
        )
        .orElse(List.of());

      List<LocalDate> activeDateOfDatedServiceJourneys = Optional
        .ofNullable(serviceJourneyId)
        .map(serviceJourneyOperatingDaysMap::get)
        .map(dayTypeIds ->
          dayTypeIds
            .stream()
            .map(activeDatesMap::get)
            .map(ActiveDates::dates)
            .flatMap(List::stream)
            .toList()
        )
        .orElse(List.of());

      return Stream
        .of(activeDateOfDayTypes, activeDateOfDatedServiceJourneys)
        .flatMap(List::stream)
        .toList();
    }

    private ServiceJourneyStop serviceJourneyStopAtScheduleStopPoint(
      ServiceJourneyId serviceJourneyId,
      ScheduledStopPointId scheduledStopPointId
    ) {
      return Optional
        .ofNullable(serviceJourneyId)
        .map(serviceJourneyIdListMap::get)
        .flatMap(serviceJourneyStops ->
          serviceJourneyStops
            .stream()
            .filter(serviceJourneyStop ->
              serviceJourneyStop
                .scheduledStopPointId()
                .equals(scheduledStopPointId)
            )
            .map(ServiceJourneyStop::fixMissingTimeValues)
            .findFirst()
        )
        .orElse(null);
    }
  }

  public boolean isValid() {
    return (
      serviceJourneyInterchangeInfo != null &&
      serviceJourneyInterchangeInfo.isValid() &&
      fromServiceJourneyStop != null &&
      toServiceJourneyStop != null
    );
  }
}
