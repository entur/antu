package no.entur.antu;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.time.LocalDate;
import java.util.List;
import no.entur.antu.netextestdata.NetexTestFragment;
import no.entur.antu.validation.OperatingDayCalculator;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.OperatingDay;

public class OperatingDaysCalculatorTest {

  @Test
  void testNoServiceCalendarFrame() {
    NetexTestFragment netexTestFragment = new NetexTestFragment();
    netexTestFragment.netexEntitiesIndex().create();

    List<LocalDate> localDates = OperatingDayCalculator.operatingDays(
      netexTestFragment.netexEntitiesIndex().create()
    );

    assertThat(localDates.size(), is(0));
  }

  @Test
  void testOperatingDaysInServiceCalendarFrame() {
    NetexTestFragment netexTestFragment = new NetexTestFragment();

    List<LocalDate> localDates = OperatingDayCalculator.operatingDays(
      netexTestFragment
        .netexEntitiesIndex()
        .addOperatingDays(
          netexTestFragment.operatingDay(LocalDate.of(2024, 1, 1)).create()
        )
        .create()
    );

    assertThat(localDates.size(), is(1));
  }

  @Test
  void testMultipleOperatingDaysInServiceCalendarFrame() {
    NetexTestFragment netexTestFragment = new NetexTestFragment();

    List<LocalDate> localDates = OperatingDayCalculator.operatingDays(
      netexTestFragment
        .netexEntitiesIndex()
        .addOperatingDays(
          netexTestFragment.operatingDay(LocalDate.of(2024, 1, 1)).create(),
          netexTestFragment.operatingDay(LocalDate.of(2024, 1, 2)).create()
        )
        .create()
    );

    assertThat(localDates.size(), is(2));
  }

  @Test
  void testDayTypeAssignmentsWithDates() {
    NetexTestFragment netexTestFragment = new NetexTestFragment();

    List<LocalDate> localDates = OperatingDayCalculator.operatingDays(
      netexTestFragment
        .netexEntitiesIndex()
        .addDayTypeAssignments(
          netexTestFragment
            .dayTypeAssignment()
            .withDate(LocalDate.of(2024, 1, 1))
            .create()
        )
        .create()
    );

    assertThat(localDates.size(), is(1));
  }

  @Test
  void testMultipleDayTypeAssignmentsWithDates() {
    NetexTestFragment netexTestFragment = new NetexTestFragment();

    List<LocalDate> localDates = OperatingDayCalculator.operatingDays(
      netexTestFragment
        .netexEntitiesIndex()
        .addDayTypeAssignments(
          netexTestFragment
            .dayTypeAssignment()
            .withDate(LocalDate.of(2024, 1, 1))
            .create(),
          netexTestFragment
            .dayTypeAssignment()
            .withDate(LocalDate.of(2024, 1, 2))
            .create()
        )
        .create()
    );

    assertThat(localDates.size(), is(2));
  }

  @Test
  void testDayTypeAssignmentsWithOperatingDayRefs() {
    NetexTestFragment netexTestFragment = new NetexTestFragment();

    OperatingDay operatingDay = netexTestFragment
      .operatingDay(LocalDate.of(2024, 1, 1))
      .create();

    List<LocalDate> localDates = OperatingDayCalculator.operatingDays(
      netexTestFragment
        .netexEntitiesIndex()
        .addDayTypeAssignments(
          netexTestFragment
            .dayTypeAssignment()
            .withOperatingDayRef(operatingDay.getId())
            .create()
        )
        .create()
    );

    assertThat(localDates.size(), is(1));
  }
}
