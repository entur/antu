package no.entur.antu.netexdata.collectors;

import static org.junit.jupiter.api.Assertions.*;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.DayOfWeekEnumeration;
import org.rutebanken.netex.model.OperatingPeriod;

class ServiceJourneyActiveDatesCollectorTest {

  @Test
  void testMapDayOfWeekByWeekDays() {
    Set<DayOfWeek> dayOfWeekSet =
      ServiceJourneyActiveDatesCollector.mapDayOfWeek(
        DayOfWeekEnumeration.WEEKDAYS
      );
    assertEquals(5, dayOfWeekSet.size());
    assertTrue(dayOfWeekSet.contains(DayOfWeek.MONDAY));
    assertTrue(dayOfWeekSet.contains(DayOfWeek.TUESDAY));
    assertTrue(dayOfWeekSet.contains(DayOfWeek.WEDNESDAY));
    assertTrue(dayOfWeekSet.contains(DayOfWeek.THURSDAY));
    assertTrue(dayOfWeekSet.contains(DayOfWeek.FRIDAY));
  }

  @Test
  void testMapDayOfWeekByWeekend() {
    Set<DayOfWeek> dayOfWeekSet =
      ServiceJourneyActiveDatesCollector.mapDayOfWeek(
        DayOfWeekEnumeration.WEEKEND
      );
    assertEquals(2, dayOfWeekSet.size());
    assertTrue(dayOfWeekSet.contains(DayOfWeek.SATURDAY));
    assertTrue(dayOfWeekSet.contains(DayOfWeek.SUNDAY));
  }

  @Test
  void testMapDayOfWeekByEveryday() {
    Set<DayOfWeek> dayOfWeekSet =
      ServiceJourneyActiveDatesCollector.mapDayOfWeek(
        DayOfWeekEnumeration.EVERYDAY
      );
    assertEquals(7, dayOfWeekSet.size());
    assertTrue(dayOfWeekSet.contains(DayOfWeek.MONDAY));
    assertTrue(dayOfWeekSet.contains(DayOfWeek.TUESDAY));
    assertTrue(dayOfWeekSet.contains(DayOfWeek.WEDNESDAY));
    assertTrue(dayOfWeekSet.contains(DayOfWeek.THURSDAY));
    assertTrue(dayOfWeekSet.contains(DayOfWeek.FRIDAY));
    assertTrue(dayOfWeekSet.contains(DayOfWeek.SATURDAY));
    assertTrue(dayOfWeekSet.contains(DayOfWeek.SUNDAY));
  }

  @Test
  void testMapDayOfWeekByNone() {
    Set<DayOfWeek> dayOfWeekSet =
      ServiceJourneyActiveDatesCollector.mapDayOfWeek(
        DayOfWeekEnumeration.NONE
      );
    assertEquals(0, dayOfWeekSet.size());
  }

  @Test
  void testSingleDayOperatingPeriodIncludesTheDay() {
    // 2025-01-15 is a Wednesday — covered by WEEKDAYS
    LocalDateTime singleDay = LocalDateTime.of(2025, 1, 15, 0, 0, 0);
    OperatingPeriod period = new OperatingPeriod()
      .withFromDate(singleDay)
      .withToDate(singleDay);

    Set<LocalDateTime> dates =
      ServiceJourneyActiveDatesCollector.computeDatesForPeriodAndWeekday(
        period,
        Set.of(DayOfWeekEnumeration.WEEKDAYS),
        Map.of()
      );

    assertEquals(
      1,
      dates.size(),
      "A single-day OperatingPeriod (FromDate == ToDate) must include that day"
    );
    assertTrue(dates.contains(singleDay));
  }
}
