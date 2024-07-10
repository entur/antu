package no.entur.antu.validation;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.rutebanken.netex.model.DayTypeAssignmentsInFrame_RelStructure;
import org.rutebanken.netex.model.OperatingDaysInFrame_RelStructure;
import org.rutebanken.netex.model.ServiceCalendarFrame_VersionFrameStructure;

public class OperatingDayCalculator {

  public static List<LocalDate> operatingDays(
    NetexEntitiesIndex netexEntitiesIndex
  ) {
    if (netexEntitiesIndex.getServiceCalendarFrames().isEmpty()) {
      return List.of();
    }

    return Stream.concat(
      operatingDatesOfOperatingDays(
        netexEntitiesIndex
      ),
      operatingDatesOfDatesInDayTypeAssignments(
        netexEntitiesIndex
      )
    ).toList();
  }

  private static Stream<LocalDate> operatingDatesOfOperatingDayRefsInDayTypeAssignments(NetexEntitiesIndex netexEntitiesIndex) {
    return netexEntitiesIndex
      .getServiceCalendarFrames()
      .stream()
      .map(ServiceCalendarFrame_VersionFrameStructure::getDayTypeAssignments)
      .filter(Objects::nonNull)
      .map(DayTypeAssignmentsInFrame_RelStructure::getDayTypeAssignment)
      .filter(Objects::nonNull)
      .flatMap(List::stream)
      .map(dayTypeAssignment -> dayTypeAssignment
        .getOperatingDayRef()
        .toLocalDate()
      );
  }

  private static Stream<LocalDate> operatingDatesOfDatesInDayTypeAssignments(NetexEntitiesIndex netexEntitiesIndex) {
    return netexEntitiesIndex
      .getServiceCalendarFrames()
      .stream()
      .map(ServiceCalendarFrame_VersionFrameStructure::getDayTypeAssignments)
      .filter(Objects::nonNull)
      .map(DayTypeAssignmentsInFrame_RelStructure::getDayTypeAssignment)
      .filter(Objects::nonNull)
      .flatMap(List::stream)
      .map(dayTypeAssignment -> dayTypeAssignment
        .getDate()
        .toLocalDate());
  }

  private static Stream<LocalDate> operatingDatesOfOperatingDays(NetexEntitiesIndex netexEntitiesIndex) {
    return netexEntitiesIndex
      .getServiceCalendarFrames()
      .stream()
      .map(ServiceCalendarFrame_VersionFrameStructure::getOperatingDays)
      .filter(Objects::nonNull)
      .map(OperatingDaysInFrame_RelStructure::getOperatingDay)
      .filter(Objects::nonNull)
      .flatMap(List::stream)
      .map(operatingDay -> operatingDay
        .getCalendarDate()
        .toLocalDate());
  }
}
