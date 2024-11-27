package no.entur.antu.netexdata.collectors.activedatecollector.calender;

import java.time.LocalDate;
import no.entur.antu.netextestdata.NetexEntitiesTestFactory;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.DayOfWeekEnumeration;

class ServiceCalendarFrameObjectTest {

  @Test
  void ofServiceCalendarFrame() {
    NetexEntitiesTestFactory testFactory = new NetexEntitiesTestFactory();
    NetexEntitiesTestFactory.CreateDayType createDayType = testFactory
      .dayType(1)
      .withDaysOfWeek(
        DayOfWeekEnumeration.MONDAY,
        DayOfWeekEnumeration.TUESDAY
      );

    NetexEntitiesTestFactory.CreateOperatingDay createOperatingDay =
      testFactory.operatingDay(1, LocalDate.of(2014, 11, 20));

    NetexEntitiesTestFactory.CreateOperatingPeriod createOperatingPeriod =
      testFactory.operatingPeriod(
        1,
        LocalDate.of(2014, 11, 21),
        LocalDate.of(2014, 11, 22)
      );

    testFactory
      .serviceCalendarFrame(1)
      .withDayTypes(createDayType)
      .withOperatingDays(createOperatingDay)
      .withOperatingPeriods(createOperatingPeriod);

  }
}
