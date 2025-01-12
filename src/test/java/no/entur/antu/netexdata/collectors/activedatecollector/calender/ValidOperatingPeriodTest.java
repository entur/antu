package no.entur.antu.netexdata.collectors.activedatecollector.calender;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import no.entur.antu.netextestdata.NetexEntitiesTestFactory;
import org.entur.netex.validation.validator.model.OperatingDayId;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.OperatingDay;
import org.rutebanken.netex.model.OperatingPeriod;
import org.rutebanken.netex.model.ValidBetween;

class ValidOperatingPeriodTest {

  @Test
  void testValidPeriodWithNoValidBetween() {
    OperatingPeriod operatingPeriod =
      new NetexEntitiesTestFactory.CreateOperatingPeriod(
        1,
        LocalDate.of(2023, 1, 1),
        LocalDate.of(2023, 12, 31)
      )
        .create();

    Map<OperatingDayId, OperatingDay> operatingDays = Map.of();

    ValidOperatingPeriod result = ValidOperatingPeriod.of(
      operatingPeriod,
      null,
      operatingDays
    );

    assertEquals(LocalDate.of(2023, 1, 1), result.startDate());
    assertEquals(LocalDate.of(2023, 12, 31), result.endDate());
  }

  @Test
  void testPeriodAdjustedToValidBetween() {
    OperatingPeriod operatingPeriod =
      new NetexEntitiesTestFactory.CreateOperatingPeriod(
        1,
        LocalDate.of(2023, 1, 1),
        LocalDate.of(2023, 12, 31)
      )
        .create();
    ValidBetween validBetween = new ValidBetween()
      .withFromDate(LocalDateTime.of(2023, 6, 1, 0, 0))
      .withToDate(LocalDateTime.of(2023, 10, 31, 0, 0));

    Map<OperatingDayId, OperatingDay> operatingDays = Map.of();

    ValidOperatingPeriod result = ValidOperatingPeriod.of(
      operatingPeriod,
      validBetween,
      operatingDays
    );

    assertEquals(LocalDate.of(2023, 6, 1), result.startDate());
    assertEquals(LocalDate.of(2023, 10, 31), result.endDate());
  }

  @Test
  void testPeriodCompletelyOutsideValidBetween() {
    OperatingPeriod operatingPeriod =
      new NetexEntitiesTestFactory.CreateOperatingPeriod(
        1,
        LocalDate.of(2023, 1, 1),
        LocalDate.of(2023, 3, 31)
      )
        .create();

    ValidBetween validBetween = new ValidBetween()
      .withFromDate(LocalDateTime.of(2023, 6, 1, 0, 0))
      .withToDate(LocalDateTime.of(2023, 10, 31, 0, 0));
    Map<OperatingDayId, OperatingDay> operatingDays = Map.of();

    ValidOperatingPeriod result = ValidOperatingPeriod.of(
      operatingPeriod,
      validBetween,
      operatingDays
    );

    assertNull(result.startDate());
    assertNull(result.endDate());
  }

  @Test
  void testValidDatesOnWeekDays() {
    ValidOperatingPeriod period = new ValidOperatingPeriod(
      LocalDate.of(2023, 6, 1),
      LocalDate.of(2023, 6, 10)
    );

    int intDayTypes = 0b0011111; // All weekdays

    List<LocalDate> result = period.toDates(Set.of(), intDayTypes);

    List<LocalDate> expectedDates = List.of(
      LocalDate.of(2023, 6, 1),
      LocalDate.of(2023, 6, 2),
      LocalDate.of(2023, 6, 5),
      LocalDate.of(2023, 6, 6),
      LocalDate.of(2023, 6, 7),
      LocalDate.of(2023, 6, 8),
      LocalDate.of(2023, 6, 9)
    );
    assertEquals(expectedDates, result);
  }

  @Test
  void testValidDatesOnWeekDaysAndExcludedDates() {
    ValidOperatingPeriod period = new ValidOperatingPeriod(
      LocalDate.of(2023, 6, 1),
      LocalDate.of(2023, 6, 10)
    );

    Set<LocalDate> excludedDates = Set.of(LocalDate.of(2023, 6, 5));
    int intDayTypes = 0b0011111; // All weekdays

    List<LocalDate> result = period.toDates(excludedDates, intDayTypes);

    List<LocalDate> expectedDates = List.of(
      LocalDate.of(2023, 6, 1),
      LocalDate.of(2023, 6, 2),
      LocalDate.of(2023, 6, 6),
      LocalDate.of(2023, 6, 7),
      LocalDate.of(2023, 6, 8),
      LocalDate.of(2023, 6, 9)
    );
    assertEquals(expectedDates, result);
  }

  @Test
  void testToDatesOnWeekends() {
    ValidOperatingPeriod period = new ValidOperatingPeriod(
      LocalDate.of(2023, 6, 1),
      LocalDate.of(2023, 6, 10)
    );

    int intDayTypes = 0b1100000; // Weekend

    List<LocalDate> result = period.toDates(null, intDayTypes);

    List<LocalDate> expectedDates = List.of(
      LocalDate.of(2023, 6, 3),
      LocalDate.of(2023, 6, 4),
      LocalDate.of(2023, 6, 10)
    );
    assertEquals(expectedDates, result);
  }

  @Test
  void testToDates_InvalidPeriod() {
    ValidOperatingPeriod period = new ValidOperatingPeriod(null, null);
    Set<LocalDate> excludedDates = Set.of();
    int intDayTypes = 0b0111111;

    List<LocalDate> result = period.toDates(excludedDates, intDayTypes);

    assertTrue(result.isEmpty());
  }

  @Test
  void testIsValid() {
    ValidOperatingPeriod validPeriod = new ValidOperatingPeriod(
      LocalDate.of(2023, 1, 1),
      LocalDate.of(2023, 12, 31)
    );
    ValidOperatingPeriod invalidPeriod = new ValidOperatingPeriod(null, null);

    assertTrue(validPeriod.isValid());
    assertFalse(invalidPeriod.isValid());
  }
}
