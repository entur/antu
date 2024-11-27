package no.entur.antu.netexdata.collectors.activedatecollector;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.Map;
import no.entur.antu.netexdata.collectors.activedatecollector.calender.ServiceCalendarFrameObject;
import no.entur.antu.netextestdata.NetexEntitiesTestFactory;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.DayOfWeekEnumeration;

class ActiveDatesCollectorTest {

  @Test
  void testParseServiceCalendarFrameInCompositeFrame() {
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

    // Create a composite frame with a service calendar frame
    netexEntitiesTestFactory
      .createCompositeFrame()
      .createServiceCalendarFrame();

    List<ServiceCalendarFrameObject> serviceCalendarFrameObjects =
      ActiveDatesCollector.parseServiceCalendarFrame(
        createContext(netexEntitiesTestFactory.create())
      );

    assertEquals(1, serviceCalendarFrameObjects.size());
  }

  @Test
  void testParseServiceCalendarFrame() {
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

    // Create a service calendar frame
    netexEntitiesTestFactory.createServiceCalendarFrame();

    List<ServiceCalendarFrameObject> serviceCalendarFrameObjects =
      ActiveDatesCollector.parseServiceCalendarFrame(
        createContext(netexEntitiesTestFactory.create())
      );

    assertEquals(1, serviceCalendarFrameObjects.size());
  }

  @Test
  void testParseMultipleServiceCalendarFrames() {
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

    // Create a service calendar frame
    netexEntitiesTestFactory.createServiceCalendarFrame(1);
    netexEntitiesTestFactory.createServiceCalendarFrame(2);
    netexEntitiesTestFactory.createServiceCalendarFrame(3);

    List<ServiceCalendarFrameObject> serviceCalendarFrameObjects =
      ActiveDatesCollector.parseServiceCalendarFrame(
        createContext(netexEntitiesTestFactory.create())
      );

    assertEquals(3, serviceCalendarFrameObjects.size());
  }

  @Test
  void testParseMultipleServiceCalendarFramesInCompositeFrame() {
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

    NetexEntitiesTestFactory.CreateCompositeFrame compositeFrame =
      netexEntitiesTestFactory.createCompositeFrame();

    // Create a service calendar frame
    compositeFrame.createServiceCalendarFrame(1);
    compositeFrame.createServiceCalendarFrame(2);
    compositeFrame.createServiceCalendarFrame(3);

    List<ServiceCalendarFrameObject> serviceCalendarFrameObjects =
      ActiveDatesCollector.parseServiceCalendarFrame(
        createContext(netexEntitiesTestFactory.create())
      );

    assertEquals(3, serviceCalendarFrameObjects.size());
  }

  /**
   * Test that a service calendar frame in a composite frame is returned
   * when there are both a service calendar frame outside composite frame also exists.
   * TODO: or do we need to parse both and combine the result?
   */
  @Test
  void testParseServiceCalendarFrameInCompositeFrameIsPrioritized() {
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

    // Create a composite frame with a service calendar frame
    netexEntitiesTestFactory
      .createCompositeFrame()
      .createServiceCalendarFrame(1)
      .createValidBetween(
        LocalDateTime.of(2024, 11, 1, 0, 0),
        LocalDateTime.of(2024, 11, 2, 0, 0)
      );

    // Create a service calendar frame
    netexEntitiesTestFactory
      .createServiceCalendarFrame(2)
      .createValidBetween(
        LocalDateTime.of(2024, 12, 1, 0, 0),
        LocalDateTime.of(2024, 12, 2, 0, 0)
      );

    List<ServiceCalendarFrameObject> serviceCalendarFrameObjects =
      ActiveDatesCollector.parseServiceCalendarFrame(
        createContext(netexEntitiesTestFactory.create())
      );

    assertEquals(1, serviceCalendarFrameObjects.size());
    assertEquals(
      Month.NOVEMBER,
      serviceCalendarFrameObjects.get(0).validBetween().getFromDate().getMonth()
    );
  }

  @Test
  void testActiveDatesForDayTypeAssignmentsAndOperatingDays() {
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

    NetexEntitiesTestFactory.CreateServiceCalendarFrame serviceCalendarFrame =
      netexEntitiesTestFactory.createServiceCalendarFrame();

    // Validity on service calendar frame
    serviceCalendarFrame.createValidBetween(
      LocalDateTime.of(2024, 11, 20, 0, 0),
      LocalDateTime.of(2024, 11, 30, 0, 0)
    );

    NetexEntitiesTestFactory.CreateDayType dayType1 = serviceCalendarFrame
      .createDayType(1)
      .withDaysOfWeek(
        DayOfWeekEnumeration.MONDAY,
        DayOfWeekEnumeration.WEDNESDAY,
        DayOfWeekEnumeration.FRIDAY
      );

    NetexEntitiesTestFactory.CreateDayType dayType2 =
      serviceCalendarFrame.createDayType(2);
    NetexEntitiesTestFactory.CreateDayType dayType3 =
      serviceCalendarFrame.createDayType(3);

    serviceCalendarFrame
      .createDayTypeAssignment(1, dayType1)
      .withOperatingPeriodRef(
        serviceCalendarFrame.createOperatingPeriod(
          LocalDate.of(2024, 11, 25),
          LocalDate.of(2024, 11, 30)
        )
      );

    serviceCalendarFrame
      .createDayTypeAssignment(2, dayType2)
      .withDate(LocalDate.of(2024, 11, 19));

    NetexEntitiesTestFactory.CreateOperatingDay operatingDay =
      serviceCalendarFrame.createOperatingDay(LocalDate.of(2024, 11, 23));
    serviceCalendarFrame
      .createDayTypeAssignment(3, dayType3)
      .withOperatingDayRef(operatingDay);

    ServiceCalendarFrameObject serviceCalendarFrameObject =
      ServiceCalendarFrameObject.ofNullable(serviceCalendarFrame.create());

    Map<String, String> activeDatesPerId =
      ActiveDatesCollector.getActiveDatesPerIdAsMapOfStrings(
        List.of(serviceCalendarFrameObject)
      );

    assertEquals(
      "2024-11-25,2024-11-27,2024-11-29",
      activeDatesPerId.get(dayType1.ref())
    );

    assertEquals("2024-11-23", activeDatesPerId.get(operatingDay.ref()));

    assertEquals("2024-11-23", activeDatesPerId.get(dayType3.ref()));
  }

  @Test
  void testActiveDatesForOperatingDaysOnly() {
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

    NetexEntitiesTestFactory.CreateServiceCalendarFrame serviceCalendarFrame =
      netexEntitiesTestFactory.createServiceCalendarFrame();

    // Validity on service calendar frame
    serviceCalendarFrame.createValidBetween(
      LocalDateTime.of(2024, 11, 20, 0, 0),
      LocalDateTime.of(2024, 11, 30, 0, 0)
    );

    List<NetexEntitiesTestFactory.CreateOperatingDay> operatingDays =
      serviceCalendarFrame.createOperatingDays(3, LocalDate.of(2024, 11, 25));

    ServiceCalendarFrameObject serviceCalendarFrameObject =
      ServiceCalendarFrameObject.ofNullable(serviceCalendarFrame.create());

    Map<String, String> activeDatesPerId =
      ActiveDatesCollector.getActiveDatesPerIdAsMapOfStrings(
        List.of(serviceCalendarFrameObject)
      );

    assertEquals(3, activeDatesPerId.size());
    assertEquals(
      "2024-11-25",
      activeDatesPerId.get(operatingDays.get(0).ref())
    );

    assertEquals(
      "2024-11-26",
      activeDatesPerId.get(operatingDays.get(1).ref())
    );

    assertEquals(
      "2024-11-27",
      activeDatesPerId.get(operatingDays.get(2).ref())
    );
  }

  @Test
  void testActiveDatesWithoutAnyValidityProvided() {
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

    NetexEntitiesTestFactory.CreateServiceCalendarFrame serviceCalendarFrame =
      netexEntitiesTestFactory.createServiceCalendarFrame();

    List<NetexEntitiesTestFactory.CreateOperatingDay> operatingDays =
      serviceCalendarFrame.createOperatingDays(3, LocalDate.of(2024, 11, 25));

    ServiceCalendarFrameObject serviceCalendarFrameObject =
      ServiceCalendarFrameObject.ofNullable(serviceCalendarFrame.create());

    Map<String, String> activeDatesPerId =
      ActiveDatesCollector.getActiveDatesPerIdAsMapOfStrings(
        List.of(serviceCalendarFrameObject)
      );

    assertEquals(3, activeDatesPerId.size());
    assertEquals(
      "2024-11-25",
      activeDatesPerId.get(operatingDays.get(0).ref())
    );

    assertEquals(
      "2024-11-26",
      activeDatesPerId.get(operatingDays.get(1).ref())
    );

    assertEquals(
      "2024-11-27",
      activeDatesPerId.get(operatingDays.get(2).ref())
    );
  }

  private static JAXBValidationContext createContext(
    NetexEntitiesIndex netexEntitiesIndex
  ) {
    return new JAXBValidationContext(
      "test123",
      netexEntitiesIndex,
      null,
      null,
      "TST",
      "fileSchemaVersion",
      null
    );
  }
}
