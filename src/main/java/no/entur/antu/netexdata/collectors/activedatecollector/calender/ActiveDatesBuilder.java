package no.entur.antu.netexdata.collectors.activedatecollector.calender;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static no.entur.antu.netexdata.collectors.activedatecollector.calender.CalendarUtilities.getOrDefault;
import static no.entur.antu.netexdata.collectors.activedatecollector.calender.CalendarUtilities.isWithinValidRange;

import jakarta.xml.bind.JAXBElement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import org.entur.netex.validation.validator.model.ActiveDates;
import org.entur.netex.validation.validator.model.DayTypeId;
import org.entur.netex.validation.validator.model.OperatingDayId;
import org.rutebanken.netex.model.DayOfWeekEnumeration;
import org.rutebanken.netex.model.DayType;
import org.rutebanken.netex.model.DayTypeAssignment;
import org.rutebanken.netex.model.OperatingDay;
import org.rutebanken.netex.model.OperatingDay_VersionStructure;
import org.rutebanken.netex.model.PropertyOfDay;
import org.rutebanken.netex.model.ValidBetween;
import org.rutebanken.netex.model.VersionOfObjectRefStructure;

public class ActiveDatesBuilder {

  private final Map<DayTypeId, ActiveDates> activeDatesForDayTypeRef =
    new HashMap<>();
  private final Map<DayTypeId, Set<LocalDate>> excludedDates = new HashMap<>();
  private final Map<DayTypeId, Integer> daysForDayTypeId = new HashMap<>();

  public Map<DayTypeId, ActiveDates> buildPerDayType(
    ServiceCalendarFrameObject serviceCalendarFrameObject
  ) {
    // Creating the DayTypes for ServiceCalendarFrame
    serviceCalendarFrameObject
      .calendarData()
      .dayTypes()
      .forEach((dayTypeRef, dayType) -> {
        activeDatesForDayTypeRef.put(
          dayTypeRef,
          new ActiveDates(new ArrayList<>())
        );
        addDayType(dayTypeRef, dayType);
      });

    if (serviceCalendarFrameObject.serviceCalendar() != null) {
      // Creating the DayTypes for ServiceCalendar
      serviceCalendarFrameObject
        .serviceCalendar()
        .calendarData()
        .dayTypes()
        .forEach((dayTypeRef, dayType) -> {
          activeDatesForDayTypeRef.put(
            dayTypeRef,
            new ActiveDates(new ArrayList<>())
          );
          addDayType(dayTypeRef, dayType);
        });
    }

    // Creating ActiveDates form DayTypeAssignments for Dates in ServiceCalendarFrame
    activeDatesForDates(
      serviceCalendarFrameObject.calendarData(),
      serviceCalendarFrameObject.validBetween()
    );

    if (serviceCalendarFrameObject.serviceCalendar() != null) {
      // Creating ActiveDates form DayTypeAssignments for Dates in ServiceCalendar
      activeDatesForDates(
        serviceCalendarFrameObject.serviceCalendar().calendarData(),
        serviceCalendarFrameObject.serviceCalendar().validBetween()
      );
    }

    // Creating ActiveDates form DayTypeAssignments for OperatingDays for ServiceCalendarFrame
    activeDatesForOperatingDays(
      serviceCalendarFrameObject.calendarData(),
      serviceCalendarFrameObject.validBetween()
    );

    if (serviceCalendarFrameObject.serviceCalendar() != null) {
      // Creating ActiveDates form DayTypeAssignments for OperatingDays for ServiceCalendar
      activeDatesForOperatingDays(
        serviceCalendarFrameObject.serviceCalendar().calendarData(),
        serviceCalendarFrameObject.serviceCalendar().validBetween()
      );
    }

    // Creating ActiveDates form DayTypeAssignments for OperatingPeriods for ServiceCalendarFrame
    activeDatesForOperatingPeriods(
      serviceCalendarFrameObject.calendarData(),
      serviceCalendarFrameObject.validBetween()
    );

    if (serviceCalendarFrameObject.serviceCalendar() != null) {
      // Creating ActiveDates form DayTypeAssignments for OperatingPeriods for ServiceCalendar
      activeDatesForOperatingPeriods(
        serviceCalendarFrameObject.serviceCalendar().calendarData(),
        serviceCalendarFrameObject.serviceCalendar().validBetween()
      );
    }

    return Map.copyOf(activeDatesForDayTypeRef);
  }

  public Map<OperatingDayId, ActiveDates> buildPerOperatingDay(
    ServiceCalendarFrameObject serviceCalendarFrameObject
  ) {
    Map<OperatingDayId, ActiveDates> activeDaysPerOperatingDayId =
      activeDaysPerOperatingDayId(
        serviceCalendarFrameObject.calendarData(),
        serviceCalendarFrameObject.validBetween()
      );

    if (serviceCalendarFrameObject.serviceCalendar() != null) {
      activeDaysPerOperatingDayId(
        serviceCalendarFrameObject.serviceCalendar().calendarData(),
        serviceCalendarFrameObject.serviceCalendar().validBetween()
      )
        .forEach(activeDaysPerOperatingDayId::putIfAbsent);
    }

    return activeDaysPerOperatingDayId;
  }

  private Map<OperatingDayId, ActiveDates> activeDaysPerOperatingDayId(
    CalendarData calendarData,
    ValidBetween validBetween
  ) {
    return calendarData
      .operatingDays()
      .entrySet()
      .stream()
      .filter(entry ->
        isWithinValidRange(entry.getValue().getCalendarDate(), validBetween)
      )
      .collect(
        toMap(
          Map.Entry::getKey,
          entry ->
            new ActiveDates(
              List.of(entry.getValue().getCalendarDate().toLocalDate())
            )
        )
      );
  }

  private void addDayType(DayTypeId dayTypeRef, DayType dayType) {
    if (dayType.getProperties() != null) {
      for (PropertyOfDay propertyOfDay : dayType
        .getProperties()
        .getPropertyOfDay()) {
        List<DayOfWeekEnumeration> daysOfWeeks = propertyOfDay.getDaysOfWeek();

        int intDayTypes = 0;
        for (DayOfWeekEnumeration dayOfWeek : daysOfWeeks) {
          List<DayOfWeekEnumeration> dayTypeEnums = convertDayOfWeek(dayOfWeek);

          for (DayOfWeekEnumeration dayTypeEnum : dayTypeEnums) {
            int mask = 1 << dayTypeEnum.ordinal();
            intDayTypes |= mask;
          }
        }
        daysForDayTypeId.put(dayTypeRef, intDayTypes);
      }
    }
  }

  private void activeDatesForDates(
    CalendarData calendarData,
    ValidBetween validBetween
  ) {
    // Dates
    for (DayTypeId dayTypeId : calendarData.dayTypeAssignments().keySet()) {
      Collection<DayTypeAssignment> dayTypeAssignments = calendarData
        .dayTypeAssignments()
        .get(dayTypeId);
      Map<Boolean, List<LocalDate>> includedAndExcludedDates =
        findIncludedAndExcludedDates(dayTypeAssignments, validBetween);

      Optional
        .ofNullable(includedAndExcludedDates.get(Boolean.FALSE))
        .filter(Predicate.not(List::isEmpty))
        .ifPresent(
          excludedDates.computeIfAbsent(dayTypeId, d -> new HashSet<>())::addAll
        );

      // It should be true here, otherwise error for missing reference should already be reported.
      // If it's false, it means that the DayTypeAssignment is referring to a dayType that is not defined in the dayTypes.
      if (activeDatesForDayTypeRef.containsKey(dayTypeId)) {
        Optional
          .ofNullable(includedAndExcludedDates.get(Boolean.TRUE))
          .ifPresent(activeDatesForDayTypeRef.get(dayTypeId).dates()::addAll);
      }
    }
  }

  private void activeDatesForOperatingDays(
    CalendarData calendarData,
    ValidBetween validBetween
  ) {
    // Operating days
    for (DayTypeId dayTypeId : calendarData.dayTypeAssignments().keySet()) {
      Collection<DayTypeAssignment> dayTypeAssignments = calendarData
        .dayTypeAssignments()
        .get(dayTypeId);
      Map<Boolean, List<LocalDate>> includedAndExcludedDates =
        findIncludedAndExcludedOperatingDays(
          dayTypeAssignments,
          validBetween,
          calendarData.operatingDays()
        );

      Optional
        .ofNullable(includedAndExcludedDates.get(Boolean.FALSE))
        .filter(Predicate.not(List::isEmpty))
        .ifPresent(
          excludedDates.computeIfAbsent(dayTypeId, d -> new HashSet<>())::addAll
        );

      // It should be true here, otherwise error for missing reference should already be reported.
      // If it's false, it means that the DayTypeAssignment is referring to a dayType that is not defined in the dayTypes.
      if (activeDatesForDayTypeRef.containsKey(dayTypeId)) {
        Optional
          .ofNullable(includedAndExcludedDates.get(Boolean.TRUE))
          .ifPresent(activeDatesForDayTypeRef.get(dayTypeId).dates()::addAll);
      }
    }
  }

  private void activeDatesForOperatingPeriods(
    CalendarData calendarData,
    ValidBetween validBetween
  ) {
    for (DayTypeId dayTypeId : calendarData.dayTypeAssignments().keys()) {
      Collection<DayTypeAssignment> dayTypeAssignments = calendarData
        .dayTypeAssignments()
        .get(dayTypeId);
      dayTypeAssignments.forEach(dayTypeAssignment ->
        Optional
          .ofNullable(dayTypeAssignment.getOperatingPeriodRef())
          .map(JAXBElement::getValue)
          .map(VersionOfObjectRefStructure::getRef)
          .map(calendarData.operatingPeriods()::get)
          .map(operatingPeriod ->
            ValidOperatingPeriod.of(
              operatingPeriod,
              validBetween,
              calendarData.operatingDays()
            )
          )
          .filter(ValidOperatingPeriod::isValid)
          .ifPresent(validOperatingPeriod ->
            activeDatesForDayTypeRef
              .get(dayTypeId)
              .dates()
              .addAll(
                validOperatingPeriod.toDates(
                  excludedDates.getOrDefault(dayTypeId, Set.of()),
                  daysForDayTypeId.getOrDefault(dayTypeId, 127) // 127 means all days of weak. i.e. 0b01111111
                )
              )
          )
      );
    }
  }

  private static Map<Boolean, List<LocalDate>> findIncludedAndExcludedDates(
    Collection<DayTypeAssignment> dayTypeAssignments,
    ValidBetween validBetween
  ) {
    return dayTypeAssignments
      .stream()
      .filter(dayTypeAssignment ->
        Optional
          .ofNullable(dayTypeAssignment.getDate())
          .filter(date -> isWithinValidRange(date, validBetween))
          .isPresent()
      )
      .collect(
        groupingBy(
          dayTypeAssignment ->
            getOrDefault(dayTypeAssignment.isIsAvailable(), Boolean.TRUE),
          mapping(
            dayTypeAssignment -> dayTypeAssignment.getDate().toLocalDate(),
            toList()
          )
        )
      );
  }

  private Map<Boolean, List<LocalDate>> findIncludedAndExcludedOperatingDays(
    Collection<DayTypeAssignment> dayTypeAssignments,
    ValidBetween validBetween,
    Map<OperatingDayId, OperatingDay> operatingDays
  ) {
    return dayTypeAssignments
      .stream()
      .filter(dayTypeAssignment ->
        Optional
          .ofNullable(OperatingDayId.of(dayTypeAssignment))
          .map(operatingDays::get)
          .map(OperatingDay::getCalendarDate)
          .filter(dateOfOperation ->
            isWithinValidRange(dateOfOperation, validBetween)
          )
          .isPresent()
      )
      .collect(
        groupingBy(
          dta -> getOrDefault(dta.isIsAvailable(), Boolean.TRUE),
          mapping(
            dta ->
              Optional
                .ofNullable(OperatingDayId.of(dta))
                .map(operatingDays::get)
                .map(OperatingDay_VersionStructure::getCalendarDate)
                .map(LocalDateTime::toLocalDate)
                .orElse(null),
            toList()
          )
        )
      );
  }

  private static List<DayOfWeekEnumeration> convertDayOfWeek(
    DayOfWeekEnumeration dayOfWeek
  ) {
    List<DayOfWeekEnumeration> days = new ArrayList<>();

    switch (dayOfWeek) {
      case MONDAY:
        days.add(DayOfWeekEnumeration.MONDAY);
        break;
      case TUESDAY:
        days.add(DayOfWeekEnumeration.TUESDAY);
        break;
      case WEDNESDAY:
        days.add(DayOfWeekEnumeration.WEDNESDAY);
        break;
      case THURSDAY:
        days.add(DayOfWeekEnumeration.THURSDAY);
        break;
      case FRIDAY:
        days.add(DayOfWeekEnumeration.FRIDAY);
        break;
      case SATURDAY:
        days.add(DayOfWeekEnumeration.SATURDAY);
        break;
      case SUNDAY:
        days.add(DayOfWeekEnumeration.SUNDAY);
        break;
      case EVERYDAY:
        days.add(DayOfWeekEnumeration.MONDAY);
        days.add(DayOfWeekEnumeration.TUESDAY);
        days.add(DayOfWeekEnumeration.WEDNESDAY);
        days.add(DayOfWeekEnumeration.THURSDAY);
        days.add(DayOfWeekEnumeration.FRIDAY);
        days.add(DayOfWeekEnumeration.SATURDAY);
        days.add(DayOfWeekEnumeration.SUNDAY);
        break;
      case WEEKDAYS:
        days.add(DayOfWeekEnumeration.MONDAY);
        days.add(DayOfWeekEnumeration.TUESDAY);
        days.add(DayOfWeekEnumeration.WEDNESDAY);
        days.add(DayOfWeekEnumeration.THURSDAY);
        days.add(DayOfWeekEnumeration.FRIDAY);
        break;
      case WEEKEND:
        days.add(DayOfWeekEnumeration.SATURDAY);
        days.add(DayOfWeekEnumeration.SUNDAY);
        break;
      case NONE:
        // None
        break;
    }
    return days;
  }
}
