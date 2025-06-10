package no.entur.antu.netexdata.collectors;

import jakarta.xml.bind.JAXBElement;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
import org.entur.netex.validation.validator.jaxb.NetexDataCollector;
import org.entur.netex.validation.validator.model.ServiceJourneyId;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.rutebanken.netex.model.*;

/**
 * Collects active dates in the dataset and connects them to their respective ServiceJourneys and DatedServiceJourneys.
 * Gets active dates as referred to by DayTypeRefs, and in the case of DatedServiceJourneys, OperatingDayRefs.
 **/
public class ServiceJourneyActiveDatesCollector extends NetexDataCollector {

  private final RedissonClient redissonClient;
  private final Map<String, Map<String, List<LocalDateTime>>> dayTypeActiveDates;
  private final Map<String, Map<ServiceJourneyId, List<LocalDateTime>>> serviceJourneyActiveDates;
  private final Map<String, Map<String, LocalDateTime>> operatingDaysToCalendarDate;

  public ServiceJourneyActiveDatesCollector(
    RedissonClient redissonClient,
    // maps validationReportId -> map of dayTypeRef-> activeDate[]
    Map<String, Map<String, List<LocalDateTime>>> dayTypeActiveDatesCache,
    // maps validationReportId -> map of serviceJourney -> activeDate[]
    Map<String, Map<ServiceJourneyId, List<LocalDateTime>>> serviceJourneyActiveDatesCache,
    // maps validationReportId -> map of operatingDayRef -> activeDate
    Map<String, Map<String, LocalDateTime>> operatingDaysToCalendarDate
  ) {
    this.redissonClient = redissonClient;
    this.dayTypeActiveDates = dayTypeActiveDatesCache;
    this.serviceJourneyActiveDates = serviceJourneyActiveDatesCache;
    this.operatingDaysToCalendarDate = operatingDaysToCalendarDate;
  }

  @Override
  protected void collectDataFromLineFile(
    JAXBValidationContext jaxbValidationContext
  ) {
    var commonOperatingDaysToCalendarDate =
      this.operatingDaysToCalendarDate.getOrDefault(
          jaxbValidationContext.getValidationReportId(),
          new HashMap<>()
        );
    var commonDayTypeActiveDates =
      this.dayTypeActiveDates.getOrDefault(
          jaxbValidationContext.getValidationReportId(),
          new HashMap<>()
        );
    Map<String, LocalDateTime> lineOperatingDaysToCalendarDate =
      getOperatingDaysToCalendarDate(jaxbValidationContext);
    var lineDayTypesToActiveDates = getDayTypesToActiveDates(
      jaxbValidationContext,
      lineOperatingDaysToCalendarDate
    );

    Map<ServiceJourneyId, List<LocalDateTime>> serviceJourneyToDates =
      jaxbValidationContext
        .serviceJourneys()
        .stream()
        // daytyperefstructure can be null if serviceJourney is referred to by a datedServiceJourney
        .filter(serviceJourney -> serviceJourney.getDayTypes() != null)
        .map(serviceJourney -> {
          DayTypeRefs_RelStructure dayTypeRefsRelStructure =
            serviceJourney.getDayTypes();
          List<JAXBElement<? extends DayTypeRefStructure>> dayTypeRefs =
            dayTypeRefsRelStructure.getDayTypeRef();
          List<LocalDateTime> serviceJourneyDates = new ArrayList<>();
          dayTypeRefs.forEach(dt -> {
            var dtId = dt.getValue().getRef();
            if (commonDayTypeActiveDates.containsKey(dtId)) {
              serviceJourneyDates.addAll(commonDayTypeActiveDates.get(dtId));
            }
            if (lineDayTypesToActiveDates.containsKey(dtId)) {
              serviceJourneyDates.addAll(lineDayTypesToActiveDates.get(dtId));
            }
          });
          return Map.entry(
            ServiceJourneyId.ofNullable(serviceJourney.getId()),
            serviceJourneyDates
          );
        })
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    jaxbValidationContext
      .datedServiceJourneys()
      .forEach(dsj -> {
        var serviceJourneyRef = dsj.getJourneyRef().get(0).getValue().getRef();
        var operatingDayRef = dsj.getOperatingDayRef().getRef();
        var serviceJourneyDates = serviceJourneyToDates.getOrDefault(
          serviceJourneyRef,
          new ArrayList<>()
        );
        if (commonOperatingDaysToCalendarDate.containsKey(operatingDayRef)) {
          serviceJourneyDates.add(
            commonOperatingDaysToCalendarDate.get(operatingDayRef)
          );
        } else if (
          lineOperatingDaysToCalendarDate.containsKey(operatingDayRef)
        ) {
          serviceJourneyDates.add(
            lineOperatingDaysToCalendarDate.get(operatingDayRef)
          );
        }
        serviceJourneyToDates.put(
          ServiceJourneyId.ofNullable(serviceJourneyRef),
          serviceJourneyDates
        );
      });
    addServiceJourneyActiveDates(
      jaxbValidationContext.getValidationReportId(),
      serviceJourneyToDates
    );
  }

  @Override
  protected void collectDataFromCommonFile(
    JAXBValidationContext jaxbValidationContext
  ) {
    Map<String, LocalDateTime> operatingDaysToCalendarDate =
      getOperatingDaysToCalendarDate(jaxbValidationContext);
    Map<String, List<LocalDateTime>> dayTypesToActiveDates =
      getDayTypesToActiveDates(
        jaxbValidationContext,
        operatingDaysToCalendarDate
      );

    addActiveDates(
      jaxbValidationContext.getValidationReportId(),
      dayTypesToActiveDates
    );
    addOperatingDays(
      jaxbValidationContext.getValidationReportId(),
      operatingDaysToCalendarDate
    );
  }

  private Map<String, LocalDateTime> getOperatingDaysFromServiceCalendar(
    JAXBValidationContext jaxbValidationContext
  ) {
    var serviceCalendar = jaxbValidationContext
      .getNetexEntitiesIndex()
      .getServiceCalendarFrames()
      .stream()
      .map(ServiceCalendarFrame_VersionFrameStructure::getServiceCalendar)
      .filter(Objects::nonNull)
      .findFirst();

    if (serviceCalendar.isEmpty()) {
      return Collections.emptyMap();
    }

    // If no service calendar is found, return an empty map
    return serviceCalendar
      .filter(calendar -> calendar.getOperatingDays() != null)
      .map(calendar ->
        calendar
          .getOperatingDays()
          .getOperatingDayRefOrOperatingDay()
          .stream()
          .filter(operatingDayRefOrOperatingDay ->
            operatingDayRefOrOperatingDay instanceof OperatingDay
          )
          .map(operatingDayRefOrOperatingDay ->
            (OperatingDay) operatingDayRefOrOperatingDay
          )
          .map(operatingDay ->
            Map.entry(operatingDay.getId(), operatingDay.getCalendarDate())
          )
          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
      )
      .orElse(Collections.emptyMap());
  }

  private Map<String, LocalDateTime> getOperatingDaysFromOperatingDaysIndex(
    JAXBValidationContext jaxbValidationContext
  ) {
    return jaxbValidationContext
      .getNetexEntitiesIndex()
      .getOperatingDayIndex()
      .getAll()
      .stream()
      .map(operatingDay ->
        Map.entry(operatingDay.getId(), operatingDay.getCalendarDate())
      )
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private Map<String, LocalDateTime> getOperatingDaysToCalendarDate(
    JAXBValidationContext jaxbValidationContext
  ) {
    Map<String, LocalDateTime> operatingDaysFromServiceCalendar =
      getOperatingDaysFromServiceCalendar(jaxbValidationContext);
    Map<String, LocalDateTime> operatingDaysFromIndex =
      getOperatingDaysFromOperatingDaysIndex(jaxbValidationContext);
    Map<String, LocalDateTime> allOperatingDaysToCalendarDate = new HashMap<>();
    allOperatingDaysToCalendarDate.putAll(operatingDaysFromServiceCalendar);
    allOperatingDaysToCalendarDate.putAll(operatingDaysFromIndex);
    return allOperatingDaysToCalendarDate;
  }

  private Map<String, List<LocalDateTime>> getDayTypesToActiveDates(
    JAXBValidationContext jaxbValidationContext,
    Map<String, LocalDateTime> operatingDaysToCalendarDate
  ) {
    return jaxbValidationContext
      .getNetexEntitiesIndex()
      .getDayTypeIndex()
      .getAll()
      .stream()
      .map(dayType -> {
        List<LocalDateTime> dates = new ArrayList<>();
        jaxbValidationContext
          .getNetexEntitiesIndex()
          .getDayTypeAssignmentsByDayTypeIdIndex()
          .get(dayType.getId())
          .forEach(dayTypeAssignment -> {
            if (dayTypeAssignment.getDate() != null) {
              dates.add(dayTypeAssignment.getDate());
            }
            if (dayTypeAssignment.getOperatingDayRef() != null) {
              dates.add(
                operatingDaysToCalendarDate.get(
                  dayTypeAssignment.getOperatingDayRef().getRef()
                )
              );
            }
            if (dayTypeAssignment.getOperatingPeriodRef() != null) {
              List<PropertyOfDay> propertyOfDayElements = dayType
                .getProperties()
                .getPropertyOfDay();
              OperatingPeriod period = jaxbValidationContext
                .getNetexEntitiesIndex()
                .getOperatingPeriodIndex()
                .get(
                  dayTypeAssignment.getOperatingPeriodRef().getValue().getRef()
                );
              Set<DayOfWeekEnumeration> daysOfWeek = new HashSet<>(
                propertyOfDayElements
                  .stream()
                  .flatMap(propertyOfDay ->
                    propertyOfDay.getDaysOfWeek().stream()
                  )
                  .toList()
              );
              dates.addAll(computeDatesForPeriodAndWeekday(period, daysOfWeek));
            }
          });
        return Map.entry(dayType.getId(), dates);
      })
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private void addServiceJourneyActiveDates(
    String validationReportId,
    Map<ServiceJourneyId, List<LocalDateTime>> newServiceJourneyActiveDates
  ) {
    RLock lock = redissonClient.getLock(validationReportId);
    try {
      lock.lock();
      var reportServiceJourneyActiveDates =
        this.serviceJourneyActiveDates.getOrDefault(
            validationReportId,
            new HashMap<>()
          );

      newServiceJourneyActiveDates.forEach((serviceJourneyId, activeDates) -> {
        reportServiceJourneyActiveDates
          .computeIfAbsent(serviceJourneyId, k -> new ArrayList<>())
          .addAll(activeDates);
      });
      this.serviceJourneyActiveDates.put(
          validationReportId,
          reportServiceJourneyActiveDates
        );
    } finally {
      if (lock.isHeldByCurrentThread()) {
        lock.unlock();
      }
    }
  }

  private void addActiveDates(
    String validationReportId,
    Map<String, List<LocalDateTime>> dayTypeActiveDates
  ) {
    RLock lock = redissonClient.getLock(validationReportId);
    try {
      lock.lock();
      var reportDayTypeActiveDates =
        this.dayTypeActiveDates.getOrDefault(
            validationReportId,
            new HashMap<>()
          );

      dayTypeActiveDates.forEach((dayTypeRef, activeDates) -> {
        reportDayTypeActiveDates
          .computeIfAbsent(dayTypeRef, k -> new ArrayList<>())
          .addAll(activeDates);
      });
      this.dayTypeActiveDates.put(validationReportId, reportDayTypeActiveDates);
    } finally {
      if (lock.isHeldByCurrentThread()) {
        lock.unlock();
      }
    }
  }

  private void addOperatingDays(
    String validationReportId,
    Map<String, LocalDateTime> operatingDays
  ) {
    RLock lock = redissonClient.getLock(validationReportId);
    try {
      lock.lock();
      var reportOperatingDaysToCalendarDate =
        this.operatingDaysToCalendarDate.getOrDefault(
            validationReportId,
            new HashMap<>()
          );

      reportOperatingDaysToCalendarDate.putAll(operatingDays);

      this.operatingDaysToCalendarDate.put(
          validationReportId,
          reportOperatingDaysToCalendarDate
        );
    } finally {
      if (lock.isHeldByCurrentThread()) {
        lock.unlock();
      }
    }
  }

  private Set<LocalDateTime> computeDatesForPeriodAndWeekday(
    OperatingPeriod period,
    Set<DayOfWeekEnumeration> daysOfWeekEnumeration
  ) {
    Set<DayOfWeek> daysOfWeek = mapDayOfWeeks(daysOfWeekEnumeration);
    Set<LocalDateTime> dates = new HashSet<>();
    LocalDateTime fromDate = period.getFromDate();
    LocalDateTime toDate = period.getToDate();
    for (LocalDateTime d = fromDate; d.isBefore(toDate); d = d.plusDays(1)) {
      if (daysOfWeek.contains(d.getDayOfWeek())) {
        dates.add(d);
      }
    }

    return dates;
  }

  static Set<DayOfWeek> mapDayOfWeeks(Collection<DayOfWeekEnumeration> values) {
    EnumSet<DayOfWeek> result = EnumSet.noneOf(DayOfWeek.class);
    for (DayOfWeekEnumeration it : values) {
      result.addAll(mapDayOfWeek(it));
    }
    return result;
  }

  static Set<DayOfWeek> mapDayOfWeek(DayOfWeekEnumeration value) {
    switch (value) {
      case MONDAY:
        return EnumSet.of(DayOfWeek.MONDAY);
      case TUESDAY:
        return EnumSet.of(DayOfWeek.TUESDAY);
      case WEDNESDAY:
        return EnumSet.of(DayOfWeek.WEDNESDAY);
      case THURSDAY:
        return EnumSet.of(DayOfWeek.THURSDAY);
      case FRIDAY:
        return EnumSet.of(DayOfWeek.FRIDAY);
      case SATURDAY:
        return EnumSet.of(DayOfWeek.SATURDAY);
      case SUNDAY:
        return EnumSet.of(DayOfWeek.SUNDAY);
      case WEEKDAYS:
        return EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);
      case WEEKEND:
        return EnumSet.range(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
      case EVERYDAY:
        return EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.SUNDAY);
      case NONE:
        return EnumSet.noneOf(DayOfWeek.class);
    }
    throw new IllegalArgumentException(
      "Day of week enum mapping missing: " + value
    );
  }
}
