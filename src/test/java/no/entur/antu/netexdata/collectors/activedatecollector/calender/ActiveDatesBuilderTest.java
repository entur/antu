package no.entur.antu.netexdata.collectors.activedatecollector.calender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import no.entur.antu.netextestdata.NetexEntitiesTestFactory;
import org.entur.netex.validation.validator.model.ActiveDates;
import org.entur.netex.validation.validator.model.DayTypeId;
import org.entur.netex.validation.validator.model.OperatingDayId;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.DayOfWeekEnumeration;

class ActiveDatesBuilderTest {

  @Test
  void testActiveDatesForDayTypeAssignmentsWithDates() {
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

    NetexEntitiesTestFactory.CreateServiceCalendarFrame serviceCalendarFrame =
      netexEntitiesTestFactory.createServiceCalendarFrame();

    // Validity on service calendar frame
    serviceCalendarFrame.createValidBetween(
      LocalDateTime.of(2024, 11, 20, 0, 0),
      LocalDateTime.of(2024, 11, 22, 0, 0)
    );

    List<NetexEntitiesTestFactory.CreateDayType> dayTypes =
      serviceCalendarFrame.createDayTypes(3);

    serviceCalendarFrame.createDayTypeAssignmentsWithDates(
      dayTypes,
      LocalDate.of(2024, 11, 21)
    );

    ServiceCalendarFrameObject serviceCalendarFrameObject =
      ServiceCalendarFrameObject.ofNullable(serviceCalendarFrame.create());

    ActiveDatesBuilder activeDatesBuilder = new ActiveDatesBuilder();

    Map<DayTypeId, ActiveDates> dayTypeIdActiveDatesMap =
      activeDatesBuilder.buildPerDayType(serviceCalendarFrameObject);

    // Date outside the validity of the service calendar frame is not included in the active dates
    assertEquals(
      2,
      dayTypeIdActiveDatesMap
        .values()
        .stream()
        .filter(ActiveDates::isValid)
        .count()
    );
  }

  @Test
  void testActiveDatesForDayTypeAssignmentsWithOperatingDays() {
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

    NetexEntitiesTestFactory.CreateServiceCalendarFrame serviceCalendarFrame =
      netexEntitiesTestFactory.createServiceCalendarFrame();

    // Validity on service calendar frame
    serviceCalendarFrame.createValidBetween(
      LocalDateTime.of(2024, 11, 20, 0, 0),
      LocalDateTime.of(2024, 11, 22, 0, 0)
    );

    List<NetexEntitiesTestFactory.CreateDayType> dayTypes =
      serviceCalendarFrame.createDayTypes(3);

    List<NetexEntitiesTestFactory.CreateOperatingDay> operatingDays =
      serviceCalendarFrame.createOperatingDays(3, LocalDate.of(2024, 11, 21));

    serviceCalendarFrame.createDayTypeAssignmentsWithOperatingDays(
      dayTypes,
      operatingDays
    );

    ServiceCalendarFrameObject serviceCalendarFrameObject =
      ServiceCalendarFrameObject.ofNullable(serviceCalendarFrame.create());

    ActiveDatesBuilder activeDatesBuilder = new ActiveDatesBuilder();

    Map<DayTypeId, ActiveDates> dayTypeIdActiveDatesMap =
      activeDatesBuilder.buildPerDayType(serviceCalendarFrameObject);

    // Dates outside the validity of the service calendar frame is not included in the active dates
    assertEquals(
      2,
      dayTypeIdActiveDatesMap
        .values()
        .stream()
        .filter(ActiveDates::isValid)
        .count()
    );
  }

  @Test
  void testOperatingDaysAndDatesAreNotEffectedByDayTypes() {
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

    NetexEntitiesTestFactory.CreateServiceCalendarFrame serviceCalendarFrame =
      netexEntitiesTestFactory.createServiceCalendarFrame();

    // Validity on service calendar frame
    serviceCalendarFrame.createValidBetween(
      LocalDateTime.of(2024, 11, 20, 0, 0),
      LocalDateTime.of(2024, 11, 25, 0, 0)
    );

    NetexEntitiesTestFactory.CreateDayType dayType1 = serviceCalendarFrame
      .createDayType(1)
      .withDaysOfWeek(DayOfWeekEnumeration.THURSDAY);

    NetexEntitiesTestFactory.CreateDayType dayType2 = serviceCalendarFrame
      .createDayType(2)
      .withDaysOfWeek(DayOfWeekEnumeration.FRIDAY);

    serviceCalendarFrame
      .createDayTypeAssignment(2, dayType1)
      .withDate(
        LocalDate.of(2024, 11, 21) // Thursday
      );

    serviceCalendarFrame
      .createDayTypeAssignment(3, dayType2)
      .withOperatingDayRef(
        serviceCalendarFrame.createOperatingDay(LocalDate.of(2024, 11, 22)) // Friday
      );

    ServiceCalendarFrameObject serviceCalendarFrameObject =
      ServiceCalendarFrameObject.ofNullable(serviceCalendarFrame.create());

    ActiveDatesBuilder activeDatesBuilder = new ActiveDatesBuilder();

    Map<DayTypeId, ActiveDates> dayTypeIdActiveDatesMap =
      activeDatesBuilder.buildPerDayType(serviceCalendarFrameObject);

    // Date outside the validity of the service calendar frame is not included in the active dates
    assertEquals(
      "2024-11-21",
      dayTypeIdActiveDatesMap.get(new DayTypeId(dayType1.ref())).toString()
    );
    assertEquals(
      "2024-11-22",
      dayTypeIdActiveDatesMap.get(new DayTypeId(dayType2.ref())).toString()
    );
  }

  @Test
  void testActiveDatesForDayTypeAssignmentsWithDatesDaysAndPeriods() {
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

    serviceCalendarFrame
      .createDayTypeAssignment(3, dayType3)
      .withOperatingDayRef(
        serviceCalendarFrame.createOperatingDay(LocalDate.of(2024, 11, 23))
      );

    ServiceCalendarFrameObject serviceCalendarFrameObject =
      ServiceCalendarFrameObject.ofNullable(serviceCalendarFrame.create());

    ActiveDatesBuilder activeDatesBuilder = new ActiveDatesBuilder();

    Map<DayTypeId, ActiveDates> dayTypeIdActiveDatesMap =
      activeDatesBuilder.buildPerDayType(serviceCalendarFrameObject);

    // Date outside the validity of the service calendar frame is not included in the active dates
    assertEquals(
      "2024-11-25,2024-11-27,2024-11-29",
      dayTypeIdActiveDatesMap.get(new DayTypeId(dayType1.ref())).toString()
    );
    assertEquals(
      "",
      dayTypeIdActiveDatesMap.get(new DayTypeId(dayType2.ref())).toString()
    );
    assertEquals(
      "2024-11-23",
      dayTypeIdActiveDatesMap.get(new DayTypeId(dayType3.ref())).toString()
    );
  }

  @Test
  void testActiveDatesForDayTypeAssignmentsWithOperatingPeriodsOutsideValidPeriod() {
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

    NetexEntitiesTestFactory.CreateServiceCalendarFrame serviceCalendarFrame =
      netexEntitiesTestFactory.createServiceCalendarFrame();

    // Validity on service calendar frame
    serviceCalendarFrame.createValidBetween(
      LocalDateTime.of(2024, 11, 23, 0, 0),
      LocalDateTime.of(2024, 12, 3, 0, 0)
    );

    List<NetexEntitiesTestFactory.CreateDayType> dayTypes =
      serviceCalendarFrame.createDayTypes(3, DayOfWeekEnumeration.EVERYDAY);

    /*
      TST:DayType:1 = 2024-11-20,2024-11-21,2024-11-22,2024-11-23,2024-11-24,2024-11-25
      TST:DayType:2 = 2024-11-25,2024-11-26,2024-11-27,2024-11-28,2024-11-29,2024-11-30
      TST:DayType:3 = 2024-11-30,2024-12-01,2024-12-02,2024-12-03,2024-12-04,2024-12-05
     */
    List<NetexEntitiesTestFactory.CreateOperatingPeriod> operatingPeriods =
      serviceCalendarFrame.createOperatingPeriods(
        3,
        LocalDate.of(2024, 11, 20),
        LocalDate.of(2024, 11, 25),
        5
      );

    serviceCalendarFrame.createDayTypeAssignmentsWithOperatingPeriods(
      dayTypes,
      operatingPeriods
    );

    ServiceCalendarFrameObject serviceCalendarFrameObject =
      ServiceCalendarFrameObject.ofNullable(serviceCalendarFrame.create());

    ActiveDatesBuilder activeDatesBuilder = new ActiveDatesBuilder();

    Map<DayTypeId, ActiveDates> dayTypeIdActiveDatesMap =
      activeDatesBuilder.buildPerDayType(serviceCalendarFrameObject);

    // Date outside the validity of the service calendar frame is not included in the active dates
    assertEquals(
      "2024-11-23,2024-11-24,2024-11-25",
      dayTypeIdActiveDatesMap
        .get(new DayTypeId(dayTypes.get(0).ref()))
        .toString()
    );
    assertEquals(
      "2024-11-25,2024-11-26,2024-11-27,2024-11-28,2024-11-29,2024-11-30",
      dayTypeIdActiveDatesMap
        .get(new DayTypeId(dayTypes.get(1).ref()))
        .toString()
    );
    assertEquals(
      "2024-11-30,2024-12-01,2024-12-02,2024-12-03",
      dayTypeIdActiveDatesMap
        .get(new DayTypeId(dayTypes.get(2).ref()))
        .toString()
    );
  }

  @Test
  void testActiveDatesForDayTypeAssignmentsWithOperatingPeriodsEveryDay() {
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

    NetexEntitiesTestFactory.CreateServiceCalendarFrame serviceCalendarFrame =
      netexEntitiesTestFactory.createServiceCalendarFrame();

    // Validity on service calendar frame
    serviceCalendarFrame.createValidBetween(
      LocalDateTime.of(2024, 11, 20, 0, 0),
      LocalDateTime.of(2024, 12, 5, 0, 0)
    );

    List<NetexEntitiesTestFactory.CreateDayType> dayTypes =
      serviceCalendarFrame.createDayTypes(3, DayOfWeekEnumeration.EVERYDAY);

    /*
      TST:DayType:1 = 2024-11-20,2024-11-21,2024-11-22,2024-11-23,2024-11-24,2024-11-25
      TST:DayType:2 = 2024-11-25,2024-11-26,2024-11-27,2024-11-28,2024-11-29,2024-11-30
      TST:DayType:3 = 2024-11-30,2024-12-01,2024-12-02,2024-12-03,2024-12-04,2024-12-05
     */
    List<NetexEntitiesTestFactory.CreateOperatingPeriod> operatingPeriods =
      serviceCalendarFrame.createOperatingPeriods(
        3,
        LocalDate.of(2024, 11, 20),
        LocalDate.of(2024, 11, 25),
        5
      );

    serviceCalendarFrame.createDayTypeAssignmentsWithOperatingPeriods(
      dayTypes,
      operatingPeriods
    );

    ServiceCalendarFrameObject serviceCalendarFrameObject =
      ServiceCalendarFrameObject.ofNullable(serviceCalendarFrame.create());

    ActiveDatesBuilder activeDatesBuilder = new ActiveDatesBuilder();

    Map<DayTypeId, ActiveDates> dayTypeIdActiveDatesMap =
      activeDatesBuilder.buildPerDayType(serviceCalendarFrameObject);

    // No Dates are outside the validity of the service calendar frame and
    // DayTypes are set to 'Every Day' so all the DayTypeAssignments, so all the days are active.
    assertEquals(
      "2024-11-20,2024-11-21,2024-11-22,2024-11-23,2024-11-24,2024-11-25",
      dayTypeIdActiveDatesMap
        .get(new DayTypeId(dayTypes.get(0).ref()))
        .toString()
    );
    assertEquals(
      "2024-11-25,2024-11-26,2024-11-27,2024-11-28,2024-11-29,2024-11-30",
      dayTypeIdActiveDatesMap
        .get(new DayTypeId(dayTypes.get(1).ref()))
        .toString()
    );
    assertEquals(
      "2024-11-30,2024-12-01,2024-12-02,2024-12-03,2024-12-04,2024-12-05",
      dayTypeIdActiveDatesMap
        .get(new DayTypeId(dayTypes.get(2).ref()))
        .toString()
    );
  }

  @Test
  void testActiveDatesForDayTypeAssignmentsWithOperatingPeriodsOnWeekDays() {
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

    NetexEntitiesTestFactory.CreateServiceCalendarFrame serviceCalendarFrame =
      netexEntitiesTestFactory.createServiceCalendarFrame();

    // Validity on service calendar frame
    serviceCalendarFrame.createValidBetween(
      LocalDateTime.of(2024, 11, 20, 0, 0),
      LocalDateTime.of(2024, 12, 5, 0, 0)
    );

    List<NetexEntitiesTestFactory.CreateDayType> dayTypes =
      serviceCalendarFrame.createDayTypes(3, DayOfWeekEnumeration.WEEKDAYS);

    /*
      TST:DayType:1 = 2024-11-20,2024-11-21,2024-11-22,2024-11-23,2024-11-24,2024-11-25
      TST:DayType:2 = 2024-11-25,2024-11-26,2024-11-27,2024-11-28,2024-11-29,2024-11-30
      TST:DayType:3 = 2024-11-30,2024-12-01,2024-12-02,2024-12-03,2024-12-04,2024-12-05
     */
    List<NetexEntitiesTestFactory.CreateOperatingPeriod> operatingPeriods =
      serviceCalendarFrame.createOperatingPeriods(
        3,
        LocalDate.of(2024, 11, 20),
        LocalDate.of(2024, 11, 25),
        5
      );

    serviceCalendarFrame.createDayTypeAssignmentsWithOperatingPeriods(
      dayTypes,
      operatingPeriods
    );

    ServiceCalendarFrameObject serviceCalendarFrameObject =
      ServiceCalendarFrameObject.ofNullable(serviceCalendarFrame.create());

    ActiveDatesBuilder activeDatesBuilder = new ActiveDatesBuilder();

    Map<DayTypeId, ActiveDates> dayTypeIdActiveDatesMap =
      activeDatesBuilder.buildPerDayType(serviceCalendarFrameObject);

    // No Dates are outside the validity of the service calendar frame and
    // DayTypes are set to 'Week days' so the dates on week ends are filtered out.
    assertEquals(
      "2024-11-20,2024-11-21,2024-11-22,2024-11-25",
      dayTypeIdActiveDatesMap
        .get(new DayTypeId(dayTypes.get(0).ref()))
        .toString()
    );
    assertEquals(
      "2024-11-25,2024-11-26,2024-11-27,2024-11-28,2024-11-29",
      dayTypeIdActiveDatesMap
        .get(new DayTypeId(dayTypes.get(1).ref()))
        .toString()
    );
    assertEquals(
      "2024-12-02,2024-12-03,2024-12-04,2024-12-05",
      dayTypeIdActiveDatesMap
        .get(new DayTypeId(dayTypes.get(2).ref()))
        .toString()
    );
  }

  @Test
  void testActiveDatesForDayTypeAssignmentsWithOperatingPeriodsOnWeekends() {
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

    NetexEntitiesTestFactory.CreateServiceCalendarFrame serviceCalendarFrame =
      netexEntitiesTestFactory.createServiceCalendarFrame();

    // Validity on service calendar frame
    serviceCalendarFrame.createValidBetween(
      LocalDateTime.of(2024, 11, 20, 0, 0),
      LocalDateTime.of(2024, 12, 5, 0, 0)
    );

    List<NetexEntitiesTestFactory.CreateDayType> dayTypes =
      serviceCalendarFrame.createDayTypes(3, DayOfWeekEnumeration.WEEKEND);

    /*
      TST:DayType:1 = 2024-11-20,2024-11-21,2024-11-22,2024-11-23,2024-11-24,2024-11-25
      TST:DayType:2 = 2024-11-25,2024-11-26,2024-11-27,2024-11-28,2024-11-29,2024-11-30
      TST:DayType:3 = 2024-11-30,2024-12-01,2024-12-02,2024-12-03,2024-12-04,2024-12-05
     */
    List<NetexEntitiesTestFactory.CreateOperatingPeriod> operatingPeriods =
      serviceCalendarFrame.createOperatingPeriods(
        3,
        LocalDate.of(2024, 11, 20),
        LocalDate.of(2024, 11, 25),
        5
      );

    serviceCalendarFrame.createDayTypeAssignmentsWithOperatingPeriods(
      dayTypes,
      operatingPeriods
    );

    ServiceCalendarFrameObject serviceCalendarFrameObject =
      ServiceCalendarFrameObject.ofNullable(serviceCalendarFrame.create());

    ActiveDatesBuilder activeDatesBuilder = new ActiveDatesBuilder();

    Map<DayTypeId, ActiveDates> dayTypeIdActiveDatesMap =
      activeDatesBuilder.buildPerDayType(serviceCalendarFrameObject);

    // No Dates are outside the validity of the service calendar frame and
    // DayTypes are set to 'Week ends' so the dates on week days are filtered out.
    assertEquals(
      "2024-11-23,2024-11-24",
      dayTypeIdActiveDatesMap
        .get(new DayTypeId(dayTypes.get(0).ref()))
        .toString()
    );
    assertEquals(
      "2024-11-30",
      dayTypeIdActiveDatesMap
        .get(new DayTypeId(dayTypes.get(1).ref()))
        .toString()
    );
    assertEquals(
      "2024-11-30,2024-12-01",
      dayTypeIdActiveDatesMap
        .get(new DayTypeId(dayTypes.get(2).ref()))
        .toString()
    );
  }

  @Test
  void testActiveDatesForDayTypeAssignmentsWithOperatingPeriodsWithSelectedDates() {
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

    NetexEntitiesTestFactory.CreateServiceCalendarFrame serviceCalendarFrame =
      netexEntitiesTestFactory.createServiceCalendarFrame();

    // Validity on service calendar frame
    serviceCalendarFrame.createValidBetween(
      LocalDateTime.of(2024, 11, 20, 0, 0),
      LocalDateTime.of(2024, 12, 5, 0, 0)
    );

    List<NetexEntitiesTestFactory.CreateDayType> dayTypes =
      serviceCalendarFrame.createDayTypes(
        3,
        DayOfWeekEnumeration.MONDAY,
        DayOfWeekEnumeration.WEDNESDAY,
        DayOfWeekEnumeration.FRIDAY
      );

    /*
      TST:DayType:1 = 2024-11-20,2024-11-21,2024-11-22,2024-11-23,2024-11-24,2024-11-25
      TST:DayType:2 = 2024-11-25,2024-11-26,2024-11-27,2024-11-28,2024-11-29,2024-11-30
      TST:DayType:3 = 2024-11-30,2024-12-01,2024-12-02,2024-12-03,2024-12-04,2024-12-05
     */
    List<NetexEntitiesTestFactory.CreateOperatingPeriod> operatingPeriods =
      serviceCalendarFrame.createOperatingPeriods(
        3,
        LocalDate.of(2024, 11, 20),
        LocalDate.of(2024, 11, 25),
        5
      );

    serviceCalendarFrame.createDayTypeAssignmentsWithOperatingPeriods(
      dayTypes,
      operatingPeriods
    );

    ServiceCalendarFrameObject serviceCalendarFrameObject =
      ServiceCalendarFrameObject.ofNullable(serviceCalendarFrame.create());

    ActiveDatesBuilder activeDatesBuilder = new ActiveDatesBuilder();

    Map<DayTypeId, ActiveDates> dayTypeIdActiveDatesMap =
      activeDatesBuilder.buildPerDayType(serviceCalendarFrameObject);

    // No Dates are outside the validity of the service calendar frame and
    // Only the dates on Monday, Wednesday and Friday are included in the active dates, as per day types.
    assertEquals(
      "2024-11-20,2024-11-22,2024-11-25",
      dayTypeIdActiveDatesMap
        .get(new DayTypeId(dayTypes.get(0).ref()))
        .toString()
    );
    assertEquals(
      "2024-11-25,2024-11-27,2024-11-29",
      dayTypeIdActiveDatesMap
        .get(new DayTypeId(dayTypes.get(1).ref()))
        .toString()
    );
    assertEquals(
      "2024-12-02,2024-12-04",
      dayTypeIdActiveDatesMap
        .get(new DayTypeId(dayTypes.get(2).ref()))
        .toString()
    );
  }

  @Test
  void testActiveDatesForDayTypeAssignmentsInServiceCalendar() {
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

    NetexEntitiesTestFactory.CreateServiceCalendarFrame serviceCalendarFrame =
      netexEntitiesTestFactory.createServiceCalendarFrame();

    // Creating Service Calendar inside Service Calendar Frame
    NetexEntitiesTestFactory.CreateServiceCalendar serviceCalendar =
      serviceCalendarFrame.createServiceCalendar();

    // Validity on service calendar
    serviceCalendar.createValidBetween(
      LocalDateTime.of(2024, 11, 20, 0, 0),
      LocalDateTime.of(2024, 11, 30, 0, 0)
    );

    NetexEntitiesTestFactory.CreateDayType dayType1 = serviceCalendar
      .createDayType(1)
      .withDaysOfWeek(
        DayOfWeekEnumeration.MONDAY,
        DayOfWeekEnumeration.WEDNESDAY,
        DayOfWeekEnumeration.FRIDAY
      );

    NetexEntitiesTestFactory.CreateDayType dayType2 =
      serviceCalendar.createDayType(2);
    NetexEntitiesTestFactory.CreateDayType dayType3 =
      serviceCalendar.createDayType(3);

    serviceCalendar
      .createDayTypeAssignment(1, dayType1)
      .withOperatingPeriodRef(
        serviceCalendar.createOperatingPeriod(
          LocalDate.of(2024, 11, 25),
          LocalDate.of(2024, 11, 30)
        )
      );

    serviceCalendar
      .createDayTypeAssignment(2, dayType2)
      .withDate(LocalDate.of(2024, 11, 19));

    serviceCalendar
      .createDayTypeAssignment(3, dayType3)
      .withOperatingDayRef(
        serviceCalendar.createOperatingDay(LocalDate.of(2024, 11, 23))
      );

    ServiceCalendarFrameObject serviceCalendarFrameObject =
      ServiceCalendarFrameObject.ofNullable(serviceCalendarFrame.create());

    ActiveDatesBuilder activeDatesBuilder = new ActiveDatesBuilder();

    Map<DayTypeId, ActiveDates> dayTypeIdActiveDatesMap =
      activeDatesBuilder.buildPerDayType(serviceCalendarFrameObject);

    // Date outside the validity of the service calendar frame is not included in the active dates
    assertEquals(
      "2024-11-25,2024-11-27,2024-11-29",
      dayTypeIdActiveDatesMap.get(new DayTypeId(dayType1.ref())).toString()
    );
    assertEquals(
      "",
      dayTypeIdActiveDatesMap.get(new DayTypeId(dayType2.ref())).toString()
    );
    assertEquals(
      "2024-11-23",
      dayTypeIdActiveDatesMap.get(new DayTypeId(dayType3.ref())).toString()
    );
  }

  @Test
  void testActiveDatesFromBothServiceCalendarFrameAndServiceCalendar() {
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

    NetexEntitiesTestFactory.CreateServiceCalendarFrame serviceCalendarFrame =
      netexEntitiesTestFactory.createServiceCalendarFrame();

    // Validity on service calendar frame
    serviceCalendarFrame.createValidBetween(
      LocalDateTime.of(2024, 11, 20, 0, 0),
      LocalDateTime.of(2024, 11, 30, 0, 0)
    );

    // Creating Service Calendar inside Service Calendar Frame
    NetexEntitiesTestFactory.CreateServiceCalendar serviceCalendar =
      serviceCalendarFrame.createServiceCalendar();

    // Creating data in Service Calendar Frame
    NetexEntitiesTestFactory.CreateDayType dayType1 = serviceCalendarFrame
      .createDayType(1)
      .withDaysOfWeek(
        DayOfWeekEnumeration.MONDAY,
        DayOfWeekEnumeration.WEDNESDAY,
        DayOfWeekEnumeration.FRIDAY
      );

    serviceCalendarFrame
      .createDayTypeAssignment(1, dayType1)
      .withOperatingPeriodRef(
        serviceCalendarFrame.createOperatingPeriod(
          LocalDate.of(2024, 11, 25),
          LocalDate.of(2024, 11, 30)
        )
      );

    // Creating data in service calendar
    NetexEntitiesTestFactory.CreateDayType dayType2 =
      serviceCalendar.createDayType(2);
    NetexEntitiesTestFactory.CreateDayType dayType3 =
      serviceCalendar.createDayType(3);

    serviceCalendar
      .createDayTypeAssignment(2, dayType2)
      .withDate(LocalDate.of(2024, 11, 21));

    serviceCalendar
      .createDayTypeAssignment(3, dayType3)
      .withOperatingDayRef(
        serviceCalendar.createOperatingDay(LocalDate.of(2024, 11, 23))
      );

    ServiceCalendarFrameObject serviceCalendarFrameObject =
      ServiceCalendarFrameObject.ofNullable(serviceCalendarFrame.create());

    ActiveDatesBuilder activeDatesBuilder = new ActiveDatesBuilder();

    Map<DayTypeId, ActiveDates> dayTypeIdActiveDatesMap =
      activeDatesBuilder.buildPerDayType(serviceCalendarFrameObject);

    // Date outside the validity of the service calendar frame is not included in the active dates
    assertEquals(
      "2024-11-25,2024-11-27,2024-11-29",
      dayTypeIdActiveDatesMap.get(new DayTypeId(dayType1.ref())).toString()
    );
    assertEquals(
      "2024-11-21",
      dayTypeIdActiveDatesMap.get(new DayTypeId(dayType2.ref())).toString()
    );
    assertEquals(
      "2024-11-23",
      dayTypeIdActiveDatesMap.get(new DayTypeId(dayType3.ref())).toString()
    );
  }

  @Test
  void testActiveDatesWithBuildPerOperationDays() {
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

    NetexEntitiesTestFactory.CreateServiceCalendarFrame serviceCalendarFrame =
      netexEntitiesTestFactory.createServiceCalendarFrame();

    // Validity on service calendar frame
    serviceCalendarFrame.createValidBetween(
      LocalDateTime.of(2024, 11, 20, 0, 0),
      LocalDateTime.of(2024, 11, 22, 0, 0)
    );

    // Creating 3 Operating days: 2024-11-21, 2024-11-22, 2024-11-23
    List<NetexEntitiesTestFactory.CreateOperatingDay> operatingDays =
      serviceCalendarFrame.createOperatingDays(3, LocalDate.of(2024, 11, 21));

    ServiceCalendarFrameObject serviceCalendarFrameObject =
      ServiceCalendarFrameObject.ofNullable(serviceCalendarFrame.create());

    ActiveDatesBuilder activeDatesBuilder = new ActiveDatesBuilder();

    // Build per operating days
    Map<OperatingDayId, ActiveDates> dayTypeIdActiveDatesMap =
      activeDatesBuilder.buildPerOperatingDay(serviceCalendarFrameObject);

    assertEquals(
      "2024-11-21",
      dayTypeIdActiveDatesMap
        .get(new OperatingDayId(operatingDays.get(0).ref()))
        .toString()
    );
    assertEquals(
      "2024-11-22",
      dayTypeIdActiveDatesMap
        .get(new OperatingDayId(operatingDays.get(1).ref()))
        .toString()
    );
    // the third operating day is outside the validity of the service calendar frame
    assertNull(
      dayTypeIdActiveDatesMap.get(
        new OperatingDayId(operatingDays.get(2).ref())
      )
    );
  }
}
