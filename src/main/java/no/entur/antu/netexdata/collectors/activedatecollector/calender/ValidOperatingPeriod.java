package no.entur.antu.netexdata.collectors.activedatecollector.calender;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.entur.netex.validation.validator.model.OperatingDayId;
import org.rutebanken.netex.model.OperatingDay;
import org.rutebanken.netex.model.OperatingPeriod;
import org.rutebanken.netex.model.ValidBetween;

record ValidOperatingPeriod(LocalDate startDate, LocalDate endDate) {
  static ValidOperatingPeriod of(
    OperatingPeriod operatingPeriod,
    ValidBetween validBetween,
    Map<OperatingDayId, OperatingDay> operatingDays
  ) {
    LocalDate fromDate = getFromDate(operatingPeriod, operatingDays);
    LocalDate toDate = getToDate(operatingPeriod, operatingDays);

    return cutToValidityCondition(fromDate, toDate, validBetween);
  }

  private static LocalDate getFromDate(
    OperatingPeriod operatingPeriod,
    Map<OperatingDayId, OperatingDay> operatingDays
  ) {
    return Optional
      .ofNullable(OperatingDayId.ofFromOperatingDayRef(operatingPeriod))
      .map(operatingDays::get)
      .map(OperatingDay::getCalendarDate)
      .map(LocalDateTime::toLocalDate)
      .orElseGet(() -> operatingPeriod.getFromDate().toLocalDate());
  }

  private static LocalDate getToDate(
    OperatingPeriod operatingPeriod,
    Map<OperatingDayId, OperatingDay> operatingDays
  ) {
    return Optional
      .ofNullable(OperatingDayId.ofToOperatingDayRef(operatingPeriod))
      .map(operatingDays::get)
      .map(OperatingDay::getCalendarDate)
      .map(LocalDateTime::toLocalDate)
      .orElseGet(() -> operatingPeriod.getToDate().toLocalDate());
  }

  // Adjust operating period to validity condition
  private static ValidOperatingPeriod cutToValidityCondition(
    LocalDate startDate,
    LocalDate endDate,
    ValidBetween validBetween
  ) {
    if (validBetween == null) {
      return new ValidOperatingPeriod(startDate, endDate);
    }

    LocalDate validFrom = validBetween.getFromDate() != null
      ? validBetween.getFromDate().toLocalDate()
      : null;
    LocalDate validTo = validBetween.getToDate() != null
      ? validBetween.getToDate().toLocalDate()
      : null;

    // Check if the period is completely outside the valid range
    if (
      (validFrom != null && endDate.isBefore(validFrom)) ||
      (validTo != null && startDate.isAfter(validTo))
    ) {
      return new ValidOperatingPeriod(null, null);
    }

    // Adjust the start and end dates to be within the valid range
    LocalDate adjustedStart = (
        validFrom != null && startDate.isBefore(validFrom)
      )
      ? validFrom
      : startDate;
    LocalDate adjustedEnd = (validTo != null && endDate.isAfter(validTo))
      ? validTo
      : endDate;

    return new ValidOperatingPeriod(adjustedStart, adjustedEnd);
  }

  List<LocalDate> toDates(Set<LocalDate> excludedDates, int intDayTypes) {
    if (!isValid()) {
      return List.of();
    }

    List<LocalDate> dates = new ArrayList<>();

    if (intDayTypes != 0) {
      LocalDate date = startDate;

      while (!date.isAfter(endDate)) {
        int aDayOfWeek = date.getDayOfWeek().getValue() - 1;
        int aDayOfWeekFlag = 1 << aDayOfWeek;
        if ((intDayTypes & aDayOfWeekFlag) == aDayOfWeekFlag) {
          if (excludedDates == null || !excludedDates.contains(date)) {
            dates.add(date);
          }
        }
        date = date.plusDays(1);
      }
    }
    return dates;
  }

  public boolean isValid() {
    return startDate != null && endDate != null;
  }
}
