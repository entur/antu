package no.entur.antu.netexdata.collectors.activedatecollector.calender;

import static no.entur.antu.netexdata.collectors.activedatecollector.calender.CalendarUtilities.getValidityForFrameOrDefault;
import static no.entur.antu.netexdata.collectors.activedatecollector.calender.CalendarUtilities.toMultimap;

import com.google.common.collect.Multimap;
import jakarta.xml.bind.JAXBElement;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import no.entur.antu.exception.AntuException;
import org.entur.netex.validation.validator.model.DayTypeId;
import org.entur.netex.validation.validator.model.OperatingDayId;
import org.rutebanken.netex.model.DayType;
import org.rutebanken.netex.model.DayTypeAssignment;
import org.rutebanken.netex.model.DayTypeAssignmentsInFrame_RelStructure;
import org.rutebanken.netex.model.DayTypesInFrame_RelStructure;
import org.rutebanken.netex.model.EntityStructure;
import org.rutebanken.netex.model.OperatingDay;
import org.rutebanken.netex.model.OperatingDaysInFrame_RelStructure;
import org.rutebanken.netex.model.OperatingPeriod;
import org.rutebanken.netex.model.OperatingPeriodsInFrame_RelStructure;
import org.rutebanken.netex.model.ServiceCalendarFrame;
import org.rutebanken.netex.model.ValidBetween;

public record ServiceCalendarFrameObject(
  ValidBetween validBetween,
  CalendarData calendarData,
  ServiceCalendarObject serviceCalendar
) {
  public static ServiceCalendarFrameObject ofNullable(
    ServiceCalendarFrame serviceCalendarFrame
  ) {
    return ofNullable(serviceCalendarFrame, null);
  }

  public static ServiceCalendarFrameObject ofNullable(
    ServiceCalendarFrame serviceCalendarFrame,
    ValidBetween compositeFrameValidity
  ) {
    if (serviceCalendarFrame == null) {
      throw new AntuException(
        "ServiceCalendarFrame_VersionFrameStructure is null"
      );
    }
    ValidBetween serviceCalendarFrameValidity = getValidityForFrameOrDefault(
      serviceCalendarFrame,
      compositeFrameValidity
    );
    return new ServiceCalendarFrameObject(
      serviceCalendarFrameValidity,
      new CalendarData(
        getDayTypes(serviceCalendarFrame),
        getOperatingPeriods(serviceCalendarFrame),
        getOperatingDays(serviceCalendarFrame),
        getDayTypeAssignmentByDayTypeId(serviceCalendarFrame)
      ),
      ServiceCalendarObject.ofNullable(
        serviceCalendarFrame.getServiceCalendar(),
        serviceCalendarFrameValidity
      )
    );
  }

  private static Multimap<DayTypeId, DayTypeAssignment> getDayTypeAssignmentByDayTypeId(
    ServiceCalendarFrame serviceCalendarFrame
  ) {
    return Optional
      .ofNullable(serviceCalendarFrame.getDayTypeAssignments())
      .map(DayTypeAssignmentsInFrame_RelStructure::getDayTypeAssignment)
      .stream()
      .flatMap(Collection::stream)
      .collect(toMultimap(DayTypeId::of, Function.identity()));
  }

  private static Map<OperatingDayId, OperatingDay> getOperatingDays(
    ServiceCalendarFrame serviceCalendarFrame
  ) {
    return Optional
      .ofNullable(serviceCalendarFrame.getOperatingDays())
      .map(OperatingDaysInFrame_RelStructure::getOperatingDay)
      .stream()
      .flatMap(Collection::stream)
      .collect(Collectors.toMap(OperatingDayId::of, Function.identity()));
  }

  private static Map<String, OperatingPeriod> getOperatingPeriods(
    ServiceCalendarFrame serviceCalendarFrame
  ) {
    return Optional
      .ofNullable(serviceCalendarFrame.getOperatingPeriods())
      .map(
        OperatingPeriodsInFrame_RelStructure::getOperatingPeriodOrUicOperatingPeriod
      )
      .stream()
      .flatMap(List::stream)
      .filter(OperatingPeriod.class::isInstance)
      .map(OperatingPeriod.class::cast)
      .collect(Collectors.toMap(EntityStructure::getId, Function.identity()));
  }

  private static Map<DayTypeId, DayType> getDayTypes(
    ServiceCalendarFrame serviceCalendarFrame
  ) {
    return Optional
      .ofNullable(serviceCalendarFrame.getDayTypes())
      .map(ServiceCalendarFrameObject::parseDayTypes)
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

  private static List<DayType> parseDayTypes(
    DayTypesInFrame_RelStructure element
  ) {
    return element
      .getDayType_()
      .stream()
      .map(JAXBElement::getValue)
      .filter(DayType.class::isInstance)
      .map(DayType.class::cast)
      .toList();
  }
}
