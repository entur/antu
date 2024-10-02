package no.entur.antu.netexdata.collectors;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import jakarta.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.List;
import org.rutebanken.netex.model.DayType;
import org.rutebanken.netex.model.DayTypeAssignment;
import org.rutebanken.netex.model.DayTypeAssignmentsInFrame_RelStructure;
import org.rutebanken.netex.model.DayTypeAssignments_RelStructure;
import org.rutebanken.netex.model.DayTypesInFrame_RelStructure;
import org.rutebanken.netex.model.DayTypes_RelStructure;
import org.rutebanken.netex.model.OperatingDay;
import org.rutebanken.netex.model.OperatingDaysInFrame_RelStructure;
import org.rutebanken.netex.model.OperatingPeriod_VersionStructure;
import org.rutebanken.netex.model.OperatingPeriodsInFrame_RelStructure;
import org.rutebanken.netex.model.OperatingPeriods_RelStructure;
import org.rutebanken.netex.model.ServiceCalendar;
import org.rutebanken.netex.model.ServiceCalendarFrame;
import org.rutebanken.netex.model.ServiceCalendarFrame_VersionFrameStructure;

class ServiceCalendarFrameParser {

  private final List<DayType> dayTypes = new ArrayList<>();
  private final List<OperatingPeriod_VersionStructure> operatingPeriods =
    new ArrayList<>();
  private final List<OperatingDay> operatingDays = new ArrayList<>();
  private final Multimap<String, DayTypeAssignment> dayTypeAssignmentByDayTypeId =
    ArrayListMultimap.create();

  void parse(ServiceCalendarFrame_VersionFrameStructure frame) {
    parseServiceCalendar(frame.getServiceCalendar());
    parseDayTypes(frame.getDayTypes());
    parseOperatingPeriods(frame.getOperatingPeriods());
    parseOperatingDays(frame.getOperatingDays());
    parseDayTypeAssignments(frame.getDayTypeAssignments());
  }

  void parse(ServiceCalendarFrame frame) {
    parseServiceCalendar(frame.getServiceCalendar());
    parseDayTypes(frame.getDayTypes());
    parseOperatingPeriods(frame.getOperatingPeriods());
    parseOperatingDays(frame.getOperatingDays());
    parseDayTypeAssignments(frame.getDayTypeAssignments());
  }

  private void parseServiceCalendar(ServiceCalendar serviceCalendar) {
    if (serviceCalendar == null) return;

    parseDayTypes(serviceCalendar.getDayTypes());
    parseOperatingPeriods(serviceCalendar.getOperatingPeriods());
    parseDayTypeAssignments(serviceCalendar.getDayTypeAssignments());
  }

  private void parseDayTypes(DayTypesInFrame_RelStructure element) {
    if (element == null) return;
    for (JAXBElement<?> dt : element.getDayType_()) {
      parseDayType(dt);
    }
  }

  private void parseDayTypes(DayTypes_RelStructure dayTypes) {
    if (dayTypes == null) return;
    for (JAXBElement<?> dt : dayTypes.getDayTypeRefOrDayType_()) {
      parseDayType(dt);
    }
  }

  private void parseDayType(JAXBElement<?> dt) {
    if (dt.getValue() instanceof DayType) {
      dayTypes.add((DayType) dt.getValue());
    }
  }

  private void parseOperatingPeriods(
    OperatingPeriodsInFrame_RelStructure operatingPeriods
  ) {
    if (operatingPeriods == null) {
      return;
    }

    for (OperatingPeriod_VersionStructure p : operatingPeriods.getOperatingPeriodOrUicOperatingPeriod()) {
      parseOperatingPeriod(p);
    }
  }

  private void parseOperatingPeriods(
    OperatingPeriods_RelStructure operatingPeriods
  ) {
    if (operatingPeriods == null) {
      return;
    }

    for (Object p : operatingPeriods.getOperatingPeriodRefOrOperatingPeriodOrUicOperatingPeriod()) {
      parseOperatingPeriod(p);
    }
  }

  private void parseOperatingPeriod(Object operatingPeriod) {
    if (operatingPeriod instanceof OperatingPeriod_VersionStructure op) {
      operatingPeriods.add(op);
    }
  }

  private void parseOperatingDays(OperatingDaysInFrame_RelStructure element) {
    if (element == null) {
      return;
    }
    operatingDays.addAll(element.getOperatingDay());
  }

  private void parseDayTypeAssignments(
    DayTypeAssignments_RelStructure element
  ) {
    if (element == null) {
      return;
    }
    parseDayTypeAssignments(element.getDayTypeAssignment());
  }

  private void parseDayTypeAssignments(
    DayTypeAssignmentsInFrame_RelStructure element
  ) {
    if (element == null) {
      return;
    }
    parseDayTypeAssignments(element.getDayTypeAssignment());
  }

  private void parseDayTypeAssignments(List<DayTypeAssignment> elements) {
    for (DayTypeAssignment it : elements) {
      String ref = it.getDayTypeRef().getValue().getRef();
      dayTypeAssignmentByDayTypeId.put(ref, it);
    }
  }
}
