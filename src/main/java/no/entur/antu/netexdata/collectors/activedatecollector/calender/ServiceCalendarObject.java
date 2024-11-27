package no.entur.antu.netexdata.collectors.activedatecollector.calender;

import static no.entur.antu.netexdata.collectors.activedatecollector.calender.CalendarUtilities.getValidBetween;
import static no.entur.antu.netexdata.collectors.activedatecollector.calender.CalendarUtilities.toMultimap;

import com.google.common.collect.Multimap;
import jakarta.xml.bind.JAXBElement;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.entur.netex.validation.validator.model.DayTypeId;
import org.entur.netex.validation.validator.model.OperatingDayId;
import org.rutebanken.netex.model.DayType;
import org.rutebanken.netex.model.DayTypeAssignment;
import org.rutebanken.netex.model.DayTypeAssignments_RelStructure;
import org.rutebanken.netex.model.DayTypes_RelStructure;
import org.rutebanken.netex.model.EntityStructure;
import org.rutebanken.netex.model.OperatingDay;
import org.rutebanken.netex.model.OperatingDays_RelStructure;
import org.rutebanken.netex.model.OperatingPeriod;
import org.rutebanken.netex.model.OperatingPeriod_VersionStructure;
import org.rutebanken.netex.model.OperatingPeriods_RelStructure;
import org.rutebanken.netex.model.ServiceCalendar;
import org.rutebanken.netex.model.ValidBetween;

public record ServiceCalendarObject(
  ValidBetween validBetween,
  CalendarData calendarData
) {
  static ServiceCalendarObject ofNullable(
    ServiceCalendar serviceCalendar,
    ValidBetween serviceCalendarFrameValidity
  ) {
    if (serviceCalendar == null) {
      return null;
    }
    return new ServiceCalendarObject(
      getServiceCalendarValidity(serviceCalendar, serviceCalendarFrameValidity),
      new CalendarData(
        getDayTypes(serviceCalendar),
        getOperatingPeriods(serviceCalendar),
        getOperatingDays(serviceCalendar),
        getDayTypeAssignmentByDayTypeId(serviceCalendar)
      )
    );
  }

  private static Multimap<DayTypeId, DayTypeAssignment> getDayTypeAssignmentByDayTypeId(
    ServiceCalendar serviceCalendar
  ) {
    return Optional
      .ofNullable(serviceCalendar.getDayTypeAssignments())
      .map(DayTypeAssignments_RelStructure::getDayTypeAssignment)
      .stream()
      .flatMap(Collection::stream)
      .collect(toMultimap(DayTypeId::of, Function.identity()));
  }

  private static Map<OperatingDayId, OperatingDay> getOperatingDays(
    ServiceCalendar serviceCalendar
  ) {
    return Optional
      .ofNullable(serviceCalendar.getOperatingDays())
      .map(OperatingDays_RelStructure::getOperatingDayRefOrOperatingDay)
      .stream()
      .flatMap(Collection::stream)
      .filter(OperatingDay.class::isInstance)
      .map(OperatingDay.class::cast)
      .collect(Collectors.toMap(OperatingDayId::of, Function.identity()));
  }

  private static Map<String, OperatingPeriod> getOperatingPeriods(
    ServiceCalendar serviceCalendar
  ) {
    return Optional
      .ofNullable(serviceCalendar.getOperatingPeriods())
      .map(ServiceCalendarObject::parseOperatingPeriods)
      .stream()
      .flatMap(List::stream)
      .filter(OperatingPeriod.class::isInstance)
      .map(OperatingPeriod.class::cast)
      .collect(Collectors.toMap(EntityStructure::getId, Function.identity()));
  }

  private static Map<DayTypeId, DayType> getDayTypes(
    ServiceCalendar serviceCalendar
  ) {
    return Optional
      .ofNullable(serviceCalendar.getDayTypes())
      .map(ServiceCalendarObject::parseDayTypes)
      .stream()
      .flatMap(List::stream)
      .filter(dayType -> DayTypeId.isValid(dayType.getId()))
      .collect(
        Collectors.toMap(
          dayType -> new DayTypeId(dayType.getId()),
          Function.identity()
        )
      );
  }

  private static ValidBetween getServiceCalendarValidity(
    ServiceCalendar serviceCalendar,
    ValidBetween serviceCalendarFrameValidity
  ) {
    if (
      serviceCalendar.getFromDate() != null &&
      serviceCalendar.getToDate() != null
    ) {
      LocalDateTime fromDateTime = serviceCalendar.getFromDate();
      LocalDateTime toDateTime = serviceCalendar.getToDate();
      return new ValidBetween()
        .withFromDate(fromDateTime)
        .withToDate(toDateTime);
    } else {
      ValidBetween entityValidity = getValidBetweenForServiceCalendar(
        serviceCalendar
      );
      if (entityValidity != null) {
        return entityValidity;
      }
      return serviceCalendarFrameValidity;
    }
  }

  static ValidBetween getValidBetweenForServiceCalendar(
    ServiceCalendar entityStruct
  ) {
    ValidBetween validBetween = null;

    if (entityStruct.getValidityConditions() != null) {
      validBetween = getValidBetween(entityStruct.getValidityConditions());
    } else if (
      entityStruct.getValidBetween() != null &&
      !entityStruct.getValidBetween().isEmpty()
    ) {
      validBetween = entityStruct.getValidBetween().get(0);
    }

    return validBetween;
  }

  private static List<DayType> parseDayTypes(DayTypes_RelStructure dayTypes) {
    return dayTypes
      .getDayTypeRefOrDayType_()
      .stream()
      .map(JAXBElement::getValue)
      .filter(DayType.class::isInstance)
      .map(DayType.class::cast)
      .toList();
  }

  private static List<OperatingPeriod_VersionStructure> parseOperatingPeriods(
    OperatingPeriods_RelStructure operatingPeriods
  ) {
    return operatingPeriods
      .getOperatingPeriodRefOrOperatingPeriodOrUicOperatingPeriod()
      .stream()
      .map(JAXBElement::getValue)
      .filter(OperatingPeriod_VersionStructure.class::isInstance)
      .map(OperatingPeriod_VersionStructure.class::cast)
      .toList();
  }
}
