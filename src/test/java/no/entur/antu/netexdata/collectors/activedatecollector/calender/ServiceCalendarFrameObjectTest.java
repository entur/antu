package no.entur.antu.netexdata.collectors.activedatecollector.calender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import no.entur.antu.netextestdata.NetexEntitiesTestFactory;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.DayOfWeekEnumeration;

class ServiceCalendarFrameObjectTest {

  /**
   * Test Validity is set on service calendar inside Service calendar frame.
   */
  @Test
  void testValidityOnServiceCalendar() {
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

    NetexEntitiesTestFactory.CreateServiceCalendarFrame serviceCalendarFrame =
      netexEntitiesTestFactory.createServiceCalendarFrame();

    NetexEntitiesTestFactory.CreateServiceCalendar serviceCalendar =
      serviceCalendarFrame.createServiceCalendar();
    LocalDateTime fromDateTime = LocalDateTime.of(2024, 11, 20, 0, 0);
    LocalDateTime toDateTime = LocalDateTime.of(2024, 11, 22, 0, 0);

    // Validity on service calendar frame
    serviceCalendar.createValidBetween(fromDateTime, toDateTime);

    ServiceCalendarFrameObject serviceCalendarFrameObject =
      ServiceCalendarFrameObject.ofNullable(serviceCalendarFrame.create());

    assertEquals(
      fromDateTime,
      serviceCalendarFrameObject.serviceCalendar().validBetween().getFromDate()
    );
    assertEquals(
      toDateTime,
      serviceCalendarFrameObject.serviceCalendar().validBetween().getToDate()
    );
  }

  /**
   * Test Validity is set on service calendar frame.
   */
  @Test
  void testValidityOnServiceCalendarFrame() {
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

    NetexEntitiesTestFactory.CreateServiceCalendarFrame serviceCalendarFrame =
      netexEntitiesTestFactory.createServiceCalendarFrame();
    LocalDateTime fromDateTime = LocalDateTime.of(2024, 11, 20, 0, 0);
    LocalDateTime toDateTime = LocalDateTime.of(2024, 11, 22, 0, 0);

    // Validity on service calendar frame
    serviceCalendarFrame.createValidBetween(fromDateTime, toDateTime);

    ServiceCalendarFrameObject serviceCalendarFrameObject =
      ServiceCalendarFrameObject.ofNullable(serviceCalendarFrame.create());

    assertNull(serviceCalendarFrameObject.serviceCalendar());
    assertEquals(
      fromDateTime,
      serviceCalendarFrameObject.validBetween().getFromDate()
    );
    assertEquals(
      toDateTime,
      serviceCalendarFrameObject.validBetween().getToDate()
    );
  }

  /**
   * Validity is set on service calendar frame only.
   * Service calendar uses validity from service calendar frame.
   */
  @Test
  void testServiceCalendarUsesValidityFromServiceCalendarFrame() {
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

    NetexEntitiesTestFactory.CreateServiceCalendarFrame serviceCalendarFrame =
      netexEntitiesTestFactory.createServiceCalendarFrame();

    // Validity on service calendar frame
    LocalDateTime fromDateTimeServiceCalendarFrame = LocalDateTime.of(
      2024,
      11,
      20,
      0,
      0
    );
    LocalDateTime toDateTimeServiceCalendarFrame = LocalDateTime.of(
      2024,
      11,
      22,
      0,
      0
    );
    serviceCalendarFrame.createValidBetween(
      fromDateTimeServiceCalendarFrame,
      toDateTimeServiceCalendarFrame
    );

    // Creating service calendar inside service calendar frame
    serviceCalendarFrame.createServiceCalendar();

    ServiceCalendarFrameObject serviceCalendarFrameObject =
      ServiceCalendarFrameObject.ofNullable(serviceCalendarFrame.create());

    assertNotNull(serviceCalendarFrameObject.serviceCalendar());

    // validity for service calendar frame should be set on service calendar frame object
    assertEquals(
      fromDateTimeServiceCalendarFrame,
      serviceCalendarFrameObject.validBetween().getFromDate()
    );
    assertEquals(
      toDateTimeServiceCalendarFrame,
      serviceCalendarFrameObject.validBetween().getToDate()
    );

    // validity for service calendar frame should be set on service calendar object
    assertEquals(
      fromDateTimeServiceCalendarFrame,
      serviceCalendarFrameObject.serviceCalendar().validBetween().getFromDate()
    );
    assertEquals(
      toDateTimeServiceCalendarFrame,
      serviceCalendarFrameObject.serviceCalendar().validBetween().getToDate()
    );
  }

  /**
   * Validity is set on both service calendar frame and service calendar.
   * Both uses their own validity.
   */
  @Test
  void testBothServiceCalendarFrameAndServiceCalendarHasValidity() {
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

    NetexEntitiesTestFactory.CreateServiceCalendarFrame serviceCalendarFrame =
      netexEntitiesTestFactory.createServiceCalendarFrame();

    // Validity on service calendar frame
    LocalDateTime fromDateTimeServiceCalendarFrame = LocalDateTime.of(
      2024,
      11,
      20,
      0,
      0
    );
    LocalDateTime toDateTimeServiceCalendarFrame = LocalDateTime.of(
      2024,
      11,
      22,
      0,
      0
    );
    serviceCalendarFrame.createValidBetween(
      fromDateTimeServiceCalendarFrame,
      toDateTimeServiceCalendarFrame
    );

    NetexEntitiesTestFactory.CreateServiceCalendar serviceCalendar =
      serviceCalendarFrame.createServiceCalendar();

    // Validity on service calendar
    LocalDateTime fromDateTimeServiceCalendar = LocalDateTime.of(
      2024,
      12,
      20,
      0,
      0
    );
    LocalDateTime toDateTimeServiceCalendar = LocalDateTime.of(
      2024,
      12,
      22,
      0,
      0
    );
    serviceCalendar.createValidBetween(
      fromDateTimeServiceCalendar,
      toDateTimeServiceCalendar
    );

    ServiceCalendarFrameObject serviceCalendarFrameObject =
      ServiceCalendarFrameObject.ofNullable(serviceCalendarFrame.create());

    // validity for service calendar frame should be set on service calendar frame object
    assertEquals(
      fromDateTimeServiceCalendarFrame,
      serviceCalendarFrameObject.validBetween().getFromDate()
    );
    assertEquals(
      toDateTimeServiceCalendarFrame,
      serviceCalendarFrameObject.validBetween().getToDate()
    );

    // validity for service calendar should be set on service calendar object
    assertEquals(
      fromDateTimeServiceCalendar,
      serviceCalendarFrameObject.serviceCalendar().validBetween().getFromDate()
    );
    assertEquals(
      toDateTimeServiceCalendar,
      serviceCalendarFrameObject.serviceCalendar().validBetween().getToDate()
    );
  }

  /**
   * Validity is set on composite frame, service calendar frame and service calendar.
   * Both Service calendar frame and service calendar uses their own validity.
   */
  @Test
  void testCompositeFrameAndBothServiceCalendarFrameAndServiceCalendarHasValidity() {
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

    LocalDateTime fromDateTimeCompositeFrame = LocalDateTime.of(
      2024,
      10,
      20,
      0,
      0
    );
    LocalDateTime toDateTimeCompositeFrame = LocalDateTime.of(
      2024,
      10,
      22,
      0,
      0
    );
    NetexEntitiesTestFactory.CreateValidBetween validBetweenCompositeFrame =
      new NetexEntitiesTestFactory.CreateValidBetween(1)
        .withFromDate(fromDateTimeCompositeFrame)
        .withToDate(toDateTimeCompositeFrame);

    NetexEntitiesTestFactory.CreateServiceCalendarFrame serviceCalendarFrameInsideCompositeFrame =
      netexEntitiesTestFactory
        .createCompositeFrame()
        .createServiceCalendarFrame();

    // Validity on service calendar frame
    LocalDateTime fromDateTimeServiceCalendarFrame = LocalDateTime.of(
      2024,
      11,
      20,
      0,
      0
    );
    LocalDateTime toDateTimeServiceCalendarFrame = LocalDateTime.of(
      2024,
      11,
      22,
      0,
      0
    );
    serviceCalendarFrameInsideCompositeFrame.createValidBetween(
      fromDateTimeServiceCalendarFrame,
      toDateTimeServiceCalendarFrame
    );

    NetexEntitiesTestFactory.CreateServiceCalendar serviceCalendar =
      serviceCalendarFrameInsideCompositeFrame.createServiceCalendar();

    // Validity on service calendar
    LocalDateTime fromDateTimeServiceCalendar = LocalDateTime.of(
      2024,
      12,
      20,
      0,
      0
    );
    LocalDateTime toDateTimeServiceCalendar = LocalDateTime.of(
      2024,
      12,
      22,
      0,
      0
    );
    serviceCalendar.createValidBetween(
      fromDateTimeServiceCalendar,
      toDateTimeServiceCalendar
    );

    ServiceCalendarFrameObject serviceCalendarFrameObject =
      ServiceCalendarFrameObject.ofNullable(
        serviceCalendarFrameInsideCompositeFrame.create(),
        validBetweenCompositeFrame.create()
      );

    // validity for service calendar frame should be set on service calendar frame object
    assertEquals(
      fromDateTimeServiceCalendarFrame,
      serviceCalendarFrameObject.validBetween().getFromDate()
    );
    assertEquals(
      toDateTimeServiceCalendarFrame,
      serviceCalendarFrameObject.validBetween().getToDate()
    );

    // validity for service calendar should be set on service calendar object
    assertEquals(
      fromDateTimeServiceCalendar,
      serviceCalendarFrameObject.serviceCalendar().validBetween().getFromDate()
    );
    assertEquals(
      toDateTimeServiceCalendar,
      serviceCalendarFrameObject.serviceCalendar().validBetween().getToDate()
    );
  }

  /**
   * Validity is set on composite frame only.
   * Both Service calendar frame and service calendar uses validity from composite frame.
   */
  @Test
  void testDayTypesInServiceCalendarFrameInCompositeFrame() {
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

    NetexEntitiesTestFactory.CreateServiceCalendarFrame serviceCalendarFrame =
      netexEntitiesTestFactory
        .createCompositeFrame()
        .createServiceCalendarFrame();

    // creating service calendar inside service calendar frame
    serviceCalendarFrame.createServiceCalendar();

    // Validity on composite frame
    LocalDateTime fromDateTimeCompositeFrame = LocalDateTime.of(
      2024,
      11,
      20,
      0,
      0
    );
    LocalDateTime toDateTimeCompositeFrame = LocalDateTime.of(
      2024,
      11,
      22,
      0,
      0
    );
    NetexEntitiesTestFactory.CreateValidBetween validBetween =
      new NetexEntitiesTestFactory.CreateValidBetween(1)
        .withFromDate(fromDateTimeCompositeFrame)
        .withToDate(toDateTimeCompositeFrame);

    ServiceCalendarFrameObject serviceCalendarFrameObject =
      ServiceCalendarFrameObject.ofNullable(
        serviceCalendarFrame.create(),
        validBetween.create()
      );

    // validity for composite frame should be set on service calendar frame object
    assertEquals(
      fromDateTimeCompositeFrame,
      serviceCalendarFrameObject.validBetween().getFromDate()
    );
    assertEquals(
      toDateTimeCompositeFrame,
      serviceCalendarFrameObject.validBetween().getToDate()
    );

    // validity for composite frame should be set on service calendar object
    assertEquals(
      fromDateTimeCompositeFrame,
      serviceCalendarFrameObject.serviceCalendar().validBetween().getFromDate()
    );
    assertEquals(
      toDateTimeCompositeFrame,
      serviceCalendarFrameObject.serviceCalendar().validBetween().getToDate()
    );
  }

  /**
   * Validity is set on composite frame and service calendar frame.
   * Service calendar should use the Validity from service calendar frame.
   */
  @Test
  void testDayTypesWithServiceCalendarFrameOverridingValidity() {
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

    NetexEntitiesTestFactory.CreateServiceCalendarFrame serviceCalendarFrame =
      netexEntitiesTestFactory
        .createCompositeFrame()
        .createServiceCalendarFrame();

    // Validity on service calendar frame
    LocalDateTime fromDateTimeServiceCalendarFrame = LocalDateTime.of(
      2024,
      12,
      20,
      0,
      0
    );
    LocalDateTime toDateTimeServiceCalendarFrame = LocalDateTime.of(
      2024,
      12,
      22,
      0,
      0
    );
    serviceCalendarFrame.createValidBetween(
      fromDateTimeServiceCalendarFrame,
      toDateTimeServiceCalendarFrame
    );

    // creating service calendar inside service calendar frame
    serviceCalendarFrame.createServiceCalendar();

    // Validity on composite frame
    LocalDateTime fromDateTimeCompositeFrame = LocalDateTime.of(
      2024,
      11,
      20,
      0,
      0
    );
    LocalDateTime toDateTimeCompositeFrame = LocalDateTime.of(
      2024,
      11,
      22,
      0,
      0
    );
    NetexEntitiesTestFactory.CreateValidBetween validBetween =
      new NetexEntitiesTestFactory.CreateValidBetween(1)
        .withFromDate(fromDateTimeCompositeFrame)
        .withToDate(toDateTimeCompositeFrame);

    ServiceCalendarFrameObject serviceCalendarFrameObject =
      ServiceCalendarFrameObject.ofNullable(
        serviceCalendarFrame.create(),
        validBetween.create()
      );

    // validity for ServiceCalendarFrame should be set on service calendar frame object
    assertEquals(
      fromDateTimeServiceCalendarFrame,
      serviceCalendarFrameObject.validBetween().getFromDate()
    );
    assertEquals(
      toDateTimeServiceCalendarFrame,
      serviceCalendarFrameObject.validBetween().getToDate()
    );

    // validity for ServiceCalendarFrame should be set on service calendar object
    assertEquals(
      fromDateTimeServiceCalendarFrame,
      serviceCalendarFrameObject.serviceCalendar().validBetween().getFromDate()
    );
    assertEquals(
      toDateTimeServiceCalendarFrame,
      serviceCalendarFrameObject.serviceCalendar().validBetween().getToDate()
    );
  }

  @Test
  void testCalendarDataIsCorrectlyParsedInServiceCalenderFrame() {
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

    NetexEntitiesTestFactory.CreateServiceCalendarFrame serviceCalendarFrame =
      netexEntitiesTestFactory.createServiceCalendarFrame();
    serviceCalendarFrame.createValidBetween(
      LocalDateTime.of(2024, 11, 20, 0, 0),
      LocalDateTime.of(2024, 11, 22, 0, 0)
    );

    List<NetexEntitiesTestFactory.CreateDayType> dayTypes =
      serviceCalendarFrame.createDayTypes(
        3,
        DayOfWeekEnumeration.MONDAY,
        DayOfWeekEnumeration.TUESDAY
      );

    List<NetexEntitiesTestFactory.CreateOperatingDay> operatingDays =
      serviceCalendarFrame.createOperatingDays(2, LocalDate.of(2024, 11, 1));

    List<NetexEntitiesTestFactory.CreateOperatingPeriod> operatingPeriods =
      serviceCalendarFrame.createOperatingPeriods(
        4,
        LocalDate.of(2024, 12, 1),
        LocalDate.of(2024, 12, 6)
      );

    serviceCalendarFrame.createDayTypeAssignmentsWithDates(
      dayTypes,
      LocalDate.of(2024, 11, 20)
    );
    serviceCalendarFrame.createDayTypeAssignmentsWithOperatingDays(
      dayTypes,
      operatingDays
    );
    serviceCalendarFrame.createDayTypeAssignmentsWithOperatingPeriods(
      dayTypes,
      operatingPeriods
    );

    ServiceCalendarFrameObject serviceCalendarFrameObject =
      ServiceCalendarFrameObject.ofNullable(serviceCalendarFrame.create());

    assertEquals(
      3,
      serviceCalendarFrameObject.calendarData().dayTypes().size()
    );
    assertEquals(
      2,
      serviceCalendarFrameObject.calendarData().operatingDays().size()
    );
    assertEquals(
      4,
      serviceCalendarFrameObject.calendarData().operatingPeriods().size()
    );
    assertEquals(
      9,
      serviceCalendarFrameObject.calendarData().dayTypeAssignments().size()
    );
  }

  @Test
  void testCalendarDataIsCorrectlyParsedInServiceCalender() {
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

    NetexEntitiesTestFactory.CreateServiceCalendarFrame serviceCalendarFrame =
      netexEntitiesTestFactory.createServiceCalendarFrame();
    serviceCalendarFrame.createValidBetween(
      LocalDateTime.of(2024, 11, 20, 0, 0),
      LocalDateTime.of(2024, 11, 22, 0, 0)
    );

    NetexEntitiesTestFactory.CreateServiceCalendar serviceCalendar =
      serviceCalendarFrame.createServiceCalendar();

    List<NetexEntitiesTestFactory.CreateDayType> dayTypes =
      serviceCalendar.createDayTypes(
        3,
        DayOfWeekEnumeration.MONDAY,
        DayOfWeekEnumeration.TUESDAY
      );

    List<NetexEntitiesTestFactory.CreateOperatingDay> operatingDays =
      serviceCalendar.createOperatingDays(2, LocalDate.of(2024, 11, 1));

    List<NetexEntitiesTestFactory.CreateOperatingPeriod> operatingPeriods =
      serviceCalendar.createOperatingPeriods(
        4,
        LocalDate.of(2024, 12, 1),
        LocalDate.of(2024, 12, 6)
      );

    serviceCalendar.createDayTypeAssignmentsWithDates(
      dayTypes,
      LocalDate.of(2024, 11, 20)
    );
    serviceCalendar.createDayTypeAssignmentsWithOperatingDays(
      dayTypes,
      operatingDays
    );
    serviceCalendar.createDayTypeAssignmentsWithOperatingPeriods(
      dayTypes,
      operatingPeriods
    );

    ServiceCalendarFrameObject serviceCalendarFrameObject =
      ServiceCalendarFrameObject.ofNullable(serviceCalendarFrame.create());

    assertEquals(
      3,
      serviceCalendarFrameObject
        .serviceCalendar()
        .calendarData()
        .dayTypes()
        .size()
    );
    assertEquals(
      2,
      serviceCalendarFrameObject
        .serviceCalendar()
        .calendarData()
        .operatingDays()
        .size()
    );
    assertEquals(
      4,
      serviceCalendarFrameObject
        .serviceCalendar()
        .calendarData()
        .operatingPeriods()
        .size()
    );
    assertEquals(
      9,
      serviceCalendarFrameObject
        .serviceCalendar()
        .calendarData()
        .dayTypeAssignments()
        .size()
    );
  }

  @Test
  void testNoValidityPresent() {
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

    NetexEntitiesTestFactory.CreateServiceCalendarFrame serviceCalendarFrame =
      netexEntitiesTestFactory.createServiceCalendarFrame();

    ServiceCalendarFrameObject serviceCalendarFrameObject =
      ServiceCalendarFrameObject.ofNullable(serviceCalendarFrame.create());

    assertNotNull(serviceCalendarFrameObject);
  }
}
