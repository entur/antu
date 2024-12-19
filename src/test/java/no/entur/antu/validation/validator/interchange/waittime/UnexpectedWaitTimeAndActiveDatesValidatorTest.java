package no.entur.antu.validation.validator.interchange.waittime;

import static java.util.stream.Collectors.toMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import no.entur.antu.netextestdata.NetexEntitiesTestFactory;
import no.entur.antu.validation.ValidationTest;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.entur.netex.validation.validator.model.ActiveDates;
import org.entur.netex.validation.validator.model.ActiveDatesId;
import org.entur.netex.validation.validator.model.DayTypeId;
import org.entur.netex.validation.validator.model.OperatingDayId;
import org.entur.netex.validation.validator.model.ScheduledStopPointId;
import org.entur.netex.validation.validator.model.ServiceJourneyId;
import org.entur.netex.validation.validator.model.ServiceJourneyInterchangeInfo;
import org.entur.netex.validation.validator.model.ServiceJourneyStop;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.ServiceJourneyInterchange;

class UnexpectedWaitTimeAndActiveDatesValidatorTest extends ValidationTest {

  private class TestInterchange {

    static class TestServiceJourney {

      private TestServiceJourney(int serviceJourneyId) {
        this.serviceJourneyId = serviceJourneyId;
      }

      static class TestActiveDates {

        String activeDatesRef;
        List<LocalDate> activeDates = new ArrayList<>();

        public ActiveDatesId getActiveDatesId() {
          return ActiveDatesId.of(activeDatesRef);
        }

        public ActiveDates getActiveDates() {
          return new ActiveDates(activeDates);
        }

        TestActiveDates withActiveDatesRef(String activeDatesRef) {
          this.activeDatesRef = activeDatesRef;
          return this;
        }

        TestActiveDates addActiveDate(LocalDate activeDate) {
          this.activeDates.add(activeDate);
          return this;
        }
      }

      int serviceJourneyId;
      int scheduledStopPointId;
      List<TestActiveDates> dayTypes = new ArrayList<>();
      List<TestActiveDates> operatingDays = new ArrayList<>();
      LocalTime arrivalTime;
      LocalTime departureTime;
      int arrivalDayOffset;
      int departureDayOffset;

      public ServiceJourneyId serviceJourneyId() {
        return new ServiceJourneyId("TST:ServiceJourney:" + serviceJourneyId);
      }

      public ScheduledStopPointId scheduledStopPointId() {
        return new ScheduledStopPointId(
          "TST:ScheduledStopPoint:" + scheduledStopPointId
        );
      }

      public ServiceJourneyStop serviceJourneyStop() {
        return new ServiceJourneyStop(
          scheduledStopPointId(),
          arrivalTime,
          departureTime,
          arrivalDayOffset,
          departureDayOffset
        );
      }

      public List<DayTypeId> dayTypeRefs() {
        return dayTypes
          .stream()
          .map(activeDate -> activeDate.activeDatesRef)
          .map(DayTypeId::new)
          .toList();
      }

      public List<OperatingDayId> operatingDayRefs() {
        return operatingDays
          .stream()
          .map(activeDate -> activeDate.activeDatesRef)
          .map(OperatingDayId::new)
          .toList();
      }

      public Map<ActiveDatesId, ActiveDates> activeDatesMap() {
        return Stream
          .of(dayTypes, operatingDays)
          .flatMap(List::stream)
          .collect(
            toMap(
              TestActiveDates::getActiveDatesId,
              TestActiveDates::getActiveDates
            )
          );
      }

      TestServiceJourney withScheduledStopPointId(int scheduledStopPointId) {
        this.scheduledStopPointId = scheduledStopPointId;
        return this;
      }

      TestServiceJourney addTestDayType(
        int dayTypeId,
        LocalDate... activeDates
      ) {
        TestActiveDates testDayTypes = new TestActiveDates()
          .withActiveDatesRef("TST:DayType:" + dayTypeId);
        Stream.of(activeDates).forEach(testDayTypes::addActiveDate);
        this.dayTypes.add(testDayTypes);
        return this;
      }

      TestServiceJourney addTestOperatingDays(
        int operatingDayId,
        LocalDate... activeDates
      ) {
        TestActiveDates testOperatingDays = new TestActiveDates()
          .withActiveDatesRef("TST:OperatingDay:" + operatingDayId);
        Stream.of(activeDates).forEach(testOperatingDays::addActiveDate);
        this.operatingDays.add(testOperatingDays);
        return this;
      }

      TestServiceJourney withArrivalTime(LocalTime arrivalTime) {
        this.arrivalTime = arrivalTime;
        return this;
      }

      TestServiceJourney withDepartureTime(LocalTime departureTime) {
        this.departureTime = departureTime;
        return this;
      }

      TestServiceJourney withArrivalDayOffset(int arrivalDayOffset) {
        this.arrivalDayOffset = arrivalDayOffset;
        return this;
      }

      TestServiceJourney withDepartureDayOffset(int departureDayOffset) {
        this.departureDayOffset = departureDayOffset;
        return this;
      }
    }

    private TestServiceJourney fromServiceJourney;
    private TestServiceJourney toServiceJourney;

    TestServiceJourney newTestServiceJourney(int serviceJourneyId) {
      return new TestServiceJourney(serviceJourneyId);
    }

    TestInterchange withFromServiceJourney(
      TestServiceJourney fromServiceJourney
    ) {
      this.fromServiceJourney = fromServiceJourney;
      return this;
    }

    TestInterchange withToServiceJourney(TestServiceJourney toServiceJourney) {
      this.toServiceJourney = toServiceJourney;
      return this;
    }

    public void doMock() {
      mockGetServiceJourneyStops(
        Map.of(
          fromServiceJourney.serviceJourneyId(),
          List.of(fromServiceJourney.serviceJourneyStop()),
          toServiceJourney.serviceJourneyId(),
          List.of(toServiceJourney.serviceJourneyStop())
        )
      );

      mockGetServiceJourneyDayTypes(
        Map.of(
          fromServiceJourney.serviceJourneyId(),
          fromServiceJourney.dayTypeRefs(),
          toServiceJourney.serviceJourneyId(),
          toServiceJourney.dayTypeRefs()
        )
      );

      mockGetServiceJourneyOperatingDays(
        Map.of(
          fromServiceJourney.serviceJourneyId(),
          fromServiceJourney.operatingDayRefs(),
          toServiceJourney.serviceJourneyId(),
          toServiceJourney.operatingDayRefs()
        )
      );

      mockGetActiveDays(
        Stream
          .of(
            fromServiceJourney.activeDatesMap(),
            toServiceJourney.activeDatesMap()
          )
          .flatMap(map -> map.entrySet().stream())
          .collect(toMap(Map.Entry::getKey, Map.Entry::getValue))
      );
    }

    private ValidationReport runTest() {
      NetexEntitiesTestFactory factory = new NetexEntitiesTestFactory();

      ServiceJourneyInterchange serviceJourneyInterchange = factory
        .createServiceJourneyInterchange(1)
        .withFromPointRef(
          NetexEntitiesTestFactory.createScheduledStopPointRef(
            fromServiceJourney.scheduledStopPointId
          )
        )
        .withToPointRef(
          NetexEntitiesTestFactory.createScheduledStopPointRef(
            toServiceJourney.scheduledStopPointId
          )
        )
        .withFromJourneyRef(
          NetexEntitiesTestFactory.createServiceJourneyRef(
            fromServiceJourney.serviceJourneyId
          )
        )
        .withToJourneyRef(
          NetexEntitiesTestFactory.createServiceJourneyRef(
            toServiceJourney.serviceJourneyId
          )
        )
        .create();

      mockGetServiceJourneyInterchangeInfo(
        List.of(
          ServiceJourneyInterchangeInfo.of(
            "test.xml",
            serviceJourneyInterchange
          )
        )
      );

      return runDatasetValidation(
        UnexpectedWaitTimeAndActiveDatesValidator.class
      );
    }
  }

  @Test
  void testValidWaitTimeAndActiveDays() {
    TestInterchange testInterchange = new TestInterchange();
    testInterchange
      .withFromServiceJourney(
        testInterchange
          .newTestServiceJourney(1)
          .withScheduledStopPointId(1)
          .addTestDayType(1, LocalDate.of(2024, 11, 1))
          .withDepartureTime(LocalTime.of(9, 53, 0))
      )
      .withToServiceJourney(
        testInterchange
          .newTestServiceJourney(2)
          .withScheduledStopPointId(2)
          .addTestDayType(2, LocalDate.of(2024, 11, 1))
          .withArrivalTime(LocalTime.of(9, 56, 0))
      );

    testInterchange.doMock();

    ValidationReport validationReport = testInterchange.runTest();

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  /*
   * Interchange with feeder service journey with days types and consumer service journey with dated service journey.
   * Is this a real world scenario?
   */
  @Test
  void testValidWaitTimeAndActiveDays_MixOfDaysTypesAndDatedServiceJourneys() {
    TestInterchange testInterchange = new TestInterchange();
    testInterchange
      .withFromServiceJourney(
        testInterchange
          .newTestServiceJourney(1)
          .withScheduledStopPointId(1)
          .addTestDayType(1, LocalDate.of(2024, 11, 1))
          .withDepartureTime(LocalTime.of(9, 53, 0))
      )
      .withToServiceJourney(
        testInterchange
          .newTestServiceJourney(2)
          .withScheduledStopPointId(2)
          .addTestOperatingDays(2, LocalDate.of(2024, 11, 1))
          .withArrivalTime(LocalTime.of(9, 56, 0))
      );

    testInterchange.doMock();

    ValidationReport validationReport = testInterchange.runTest();

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void testValidWaitTimeAndActiveDaysWithDaysOffSet() {
    TestInterchange testInterchange = new TestInterchange();
    testInterchange
      .withFromServiceJourney(
        testInterchange
          .newTestServiceJourney(1)
          .withScheduledStopPointId(1)
          .addTestDayType(1, LocalDate.of(2024, 11, 1))
          .withDepartureTime(LocalTime.of(9, 53, 0))
          .withDepartureDayOffset(3)
      )
      .withToServiceJourney(
        testInterchange
          .newTestServiceJourney(2)
          .withScheduledStopPointId(2)
          .addTestDayType(2, LocalDate.of(2024, 11, 1))
          .withArrivalTime(LocalTime.of(9, 56, 0))
          .withArrivalDayOffset(3)
      );

    testInterchange.doMock();
    ValidationReport validationReport = testInterchange.runTest();

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void testWaitTimeEqualsWarningLimit() {
    TestInterchange testInterchange = new TestInterchange();
    testInterchange
      .withFromServiceJourney(
        testInterchange
          .newTestServiceJourney(1)
          .withScheduledStopPointId(1)
          .addTestDayType(1, LocalDate.of(2024, 11, 1))
          .withDepartureTime(LocalTime.of(9, 53, 0))
      )
      .withToServiceJourney(
        testInterchange
          .newTestServiceJourney(2)
          .withScheduledStopPointId(2)
          .addTestDayType(2, LocalDate.of(2024, 11, 1))
          .withArrivalTime(LocalTime.of(9, 53, 0))
      );

    testInterchange.doMock();
    ValidationReport validationReport = testInterchange.runTest();

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  /*
   * Test that the wait time between two service journeys is more than the warning limit.
   * Waiting Limit is 1 hour
   */
  @Test
  void testWaitTimeExceedingWarningLimit() {
    TestInterchange testInterchange = new TestInterchange();
    testInterchange
      .withFromServiceJourney(
        testInterchange
          .newTestServiceJourney(1)
          .withScheduledStopPointId(1)
          .addTestDayType(1, LocalDate.of(2024, 11, 1))
          .withDepartureTime(LocalTime.of(9, 53, 0))
      )
      .withToServiceJourney(
        testInterchange
          .newTestServiceJourney(2)
          .withScheduledStopPointId(2)
          .addTestDayType(2, LocalDate.of(2024, 11, 1))
          .withArrivalTime(LocalTime.of(10, 55, 0)) // waiting time 1 hour and 2 minutes
      );

    testInterchange.doMock();
    ValidationReport validationReport = testInterchange.runTest();

    assertThat(validationReport.getValidationReportEntries().size(), is(1));

    assertThat(
      validationReport
        .getValidationReportEntries()
        .stream()
        .findFirst()
        .map(ValidationReportEntry::getName)
        .orElse(null),
      is("Wait time in interchange exceeds warning limit")
    );
  }

  /*
   * Test that the wait time between two service journeys is less than the warning limit.
   * Waiting Limit is 1 hour
   */
  @Test
  void testWaitTimeExceedingWarningLimitWithDayOffset() {
    TestInterchange testInterchange = new TestInterchange();
    testInterchange
      .withFromServiceJourney(
        testInterchange
          .newTestServiceJourney(1)
          .withScheduledStopPointId(1)
          .addTestDayType(1, LocalDate.of(2024, 11, 2))
          .withDepartureTime(LocalTime.of(9, 53, 0))
      )
      .withToServiceJourney(
        testInterchange
          .newTestServiceJourney(2)
          .withScheduledStopPointId(2)
          .addTestDayType(2, LocalDate.of(2024, 11, 1))
          .withArrivalTime(LocalTime.of(10, 55, 0)) // waiting time 1 hour and 2 minutes
          .withArrivalDayOffset(1)
      );

    testInterchange.doMock();
    ValidationReport validationReport = testInterchange.runTest();

    assertThat(validationReport.getValidationReportEntries().size(), is(1));

    assertThat(
      validationReport
        .getValidationReportEntries()
        .stream()
        .findFirst()
        .map(ValidationReportEntry::getName)
        .orElse(null),
      is("Wait time in interchange exceeds warning limit")
    );
  }

  /*
   * Test that the wait time between two service journeys is less than the maximum limit.
   * Maximum Limit is 3 hours
   */
  @Test
  void testWaitTimeExceedingErrorLimit() {
    TestInterchange testInterchange = new TestInterchange();
    testInterchange
      .withFromServiceJourney(
        testInterchange
          .newTestServiceJourney(1)
          .withScheduledStopPointId(1)
          .addTestDayType(1, LocalDate.of(2024, 11, 1))
          .withDepartureTime(LocalTime.of(9, 53, 0))
      )
      .withToServiceJourney(
        testInterchange
          .newTestServiceJourney(2)
          .withScheduledStopPointId(2)
          .addTestDayType(2, LocalDate.of(2024, 11, 1))
          .withArrivalTime(LocalTime.of(12, 55, 0)) // waiting time 3 hours 2 minutes
      );

    testInterchange.doMock();
    ValidationReport validationReport = testInterchange.runTest();

    assertThat(validationReport.getValidationReportEntries().size(), is(1));

    assertThat(
      validationReport
        .getValidationReportEntries()
        .stream()
        .findFirst()
        .map(ValidationReportEntry::getName)
        .orElse(null),
      is("Wait time in interchange exceeds maximum limit")
    );
  }

  /*
   * Test that the wait time between two service journeys is more than the maximum limit.
   * Maximum Limit is 3 hours
   */
  @Test
  void testWaitTimeExceedingErrorLimitWithDayOffset() {
    TestInterchange testInterchange = new TestInterchange();

    testInterchange
      .withFromServiceJourney(
        testInterchange
          .newTestServiceJourney(1)
          .withScheduledStopPointId(1)
          .addTestDayType(1, LocalDate.of(2024, 11, 1))
          .withDepartureTime(LocalTime.of(9, 53, 0))
          .withDepartureDayOffset(1)
      )
      .withToServiceJourney(
        testInterchange
          .newTestServiceJourney(2)
          .withScheduledStopPointId(2)
          .addTestDayType(2, LocalDate.of(2024, 11, 2))
          .withArrivalTime(LocalTime.of(12, 55, 0)) // waiting time 3 hours 2 minutes
      );

    testInterchange.doMock();
    ValidationReport validationReport = testInterchange.runTest();

    assertThat(validationReport.getValidationReportEntries().size(), is(1));

    assertThat(
      validationReport
        .getValidationReportEntries()
        .stream()
        .findFirst()
        .map(ValidationReportEntry::getName)
        .orElse(null),
      is("Wait time in interchange exceeds maximum limit")
    );
  }

  @Test
  void testNoSharedActiveDays_oneDayTypePerServiceJourney() {
    TestInterchange testInterchange = new TestInterchange();
    testInterchange
      .withFromServiceJourney(
        testInterchange
          .newTestServiceJourney(1)
          .withScheduledStopPointId(1)
          .addTestDayType(1, LocalDate.of(2024, 11, 1))
          .withDepartureTime(LocalTime.of(9, 53, 0))
      )
      .withToServiceJourney(
        testInterchange
          .newTestServiceJourney(2)
          .withScheduledStopPointId(2)
          .addTestDayType(2, LocalDate.of(2024, 11, 2))
          .withArrivalTime(LocalTime.of(9, 56, 0))
      );

    testInterchange.doMock();

    ValidationReport validationReport = testInterchange.runTest();

    assertThat(validationReport.getValidationReportEntries().size(), is(1));

    assertThat(
      validationReport
        .getValidationReportEntries()
        .stream()
        .findFirst()
        .map(ValidationReportEntry::getName)
        .orElse(null),
      is("No shared active date found in interchange")
    );
  }

  @Test
  void testNoSharedActiveDays_multipleDayTypePerServiceJourney() {
    TestInterchange testInterchange = new TestInterchange();
    testInterchange
      .withFromServiceJourney(
        testInterchange
          .newTestServiceJourney(1)
          .withScheduledStopPointId(1)
          .addTestDayType(1, LocalDate.of(2024, 11, 1))
          .addTestDayType(2, LocalDate.of(2024, 11, 3))
          .addTestDayType(3, LocalDate.of(2024, 11, 5))
          .addTestDayType(4, LocalDate.of(2024, 11, 7))
          .withDepartureTime(LocalTime.of(9, 53, 0))
      )
      .withToServiceJourney(
        testInterchange
          .newTestServiceJourney(2)
          .withScheduledStopPointId(2)
          .addTestDayType(5, LocalDate.of(2024, 11, 2))
          .addTestDayType(6, LocalDate.of(2024, 11, 4))
          .addTestDayType(7, LocalDate.of(2024, 11, 6))
          .addTestDayType(8, LocalDate.of(2024, 11, 8))
          .addTestDayType(9, LocalDate.of(2024, 11, 10))
          .withArrivalTime(LocalTime.of(9, 56, 0))
      );

    testInterchange.doMock();

    ValidationReport validationReport = testInterchange.runTest();

    assertThat(validationReport.getValidationReportEntries().size(), is(1));

    assertThat(
      validationReport
        .getValidationReportEntries()
        .stream()
        .findFirst()
        .map(ValidationReportEntry::getName)
        .orElse(null),
      is("No shared active date found in interchange")
    );
  }

  @Test
  void testHasSharedActiveDay_multipleDayTypePerServiceJourney() {
    TestInterchange testInterchange = new TestInterchange();
    testInterchange
      .withFromServiceJourney(
        testInterchange
          .newTestServiceJourney(1)
          .withScheduledStopPointId(1)
          .addTestDayType(1, LocalDate.of(2024, 11, 1))
          .addTestDayType(2, LocalDate.of(2024, 11, 3)) // shared active date
          .addTestDayType(3, LocalDate.of(2024, 11, 5))
          .addTestDayType(4, LocalDate.of(2024, 11, 7))
          .withDepartureTime(LocalTime.of(9, 53, 0))
      )
      .withToServiceJourney(
        testInterchange
          .newTestServiceJourney(2)
          .withScheduledStopPointId(2)
          .addTestDayType(5, LocalDate.of(2024, 11, 2))
          .addTestDayType(6, LocalDate.of(2024, 11, 3)) // shared active date
          .addTestDayType(7, LocalDate.of(2024, 11, 6))
          .addTestDayType(8, LocalDate.of(2024, 11, 8))
          .addTestDayType(9, LocalDate.of(2024, 11, 10))
          .withArrivalTime(LocalTime.of(9, 56, 0))
      );

    testInterchange.doMock();

    ValidationReport validationReport = testInterchange.runTest();

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void testValidWaitTimeAndActiveDays_datedServiceJourneys() {
    TestInterchange testInterchange = new TestInterchange();
    testInterchange
      .withFromServiceJourney(
        testInterchange
          .newTestServiceJourney(1)
          .withScheduledStopPointId(1)
          .addTestOperatingDays(1, LocalDate.of(2024, 11, 1))
          .withDepartureTime(LocalTime.of(9, 53, 0))
      )
      .withToServiceJourney(
        testInterchange
          .newTestServiceJourney(2)
          .withScheduledStopPointId(2)
          .addTestOperatingDays(2, LocalDate.of(2024, 11, 1))
          .withArrivalTime(LocalTime.of(9, 56, 0))
      );

    testInterchange.doMock();

    ValidationReport validationReport = testInterchange.runTest();

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void testValidWaitTimeAndActiveDaysWithDaysOffSet_datedServiceJourneys() {
    TestInterchange testInterchange = new TestInterchange();
    testInterchange
      .withFromServiceJourney(
        testInterchange
          .newTestServiceJourney(1)
          .withScheduledStopPointId(1)
          .addTestOperatingDays(1, LocalDate.of(2024, 11, 1))
          .withDepartureTime(LocalTime.of(9, 53, 0))
          .withDepartureDayOffset(3)
      )
      .withToServiceJourney(
        testInterchange
          .newTestServiceJourney(2)
          .withScheduledStopPointId(2)
          .addTestOperatingDays(2, LocalDate.of(2024, 11, 1))
          .withArrivalTime(LocalTime.of(9, 56, 0))
          .withArrivalDayOffset(3)
      );

    testInterchange.doMock();
    ValidationReport validationReport = testInterchange.runTest();

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void testWaitTimeEqualsWarningLimit_datedServiceJourneys() {
    TestInterchange testInterchange = new TestInterchange();
    testInterchange
      .withFromServiceJourney(
        testInterchange
          .newTestServiceJourney(1)
          .withScheduledStopPointId(1)
          .addTestOperatingDays(1, LocalDate.of(2024, 11, 1))
          .withDepartureTime(LocalTime.of(9, 53, 0))
      )
      .withToServiceJourney(
        testInterchange
          .newTestServiceJourney(2)
          .withScheduledStopPointId(2)
          .addTestOperatingDays(2, LocalDate.of(2024, 11, 1))
          .withArrivalTime(LocalTime.of(9, 53, 0))
      );

    testInterchange.doMock();
    ValidationReport validationReport = testInterchange.runTest();

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  /*
   * Test that the wait time between two service journeys is more than the warning limit.
   * Waiting Limit is 1 hour
   */
  @Test
  void testWaitTimeExceedingWarningLimit_datedServiceJourneys() {
    TestInterchange testInterchange = new TestInterchange();
    testInterchange
      .withFromServiceJourney(
        testInterchange
          .newTestServiceJourney(1)
          .withScheduledStopPointId(1)
          .addTestOperatingDays(1, LocalDate.of(2024, 11, 1))
          .withDepartureTime(LocalTime.of(9, 53, 0))
      )
      .withToServiceJourney(
        testInterchange
          .newTestServiceJourney(2)
          .withScheduledStopPointId(2)
          .addTestOperatingDays(2, LocalDate.of(2024, 11, 1))
          .withArrivalTime(LocalTime.of(10, 55, 0)) // waiting time 1 hour and 2 minutes
      );

    testInterchange.doMock();
    ValidationReport validationReport = testInterchange.runTest();

    assertThat(validationReport.getValidationReportEntries().size(), is(1));

    assertThat(
      validationReport
        .getValidationReportEntries()
        .stream()
        .findFirst()
        .map(ValidationReportEntry::getName)
        .orElse(null),
      is("Wait time in interchange exceeds warning limit")
    );
  }

  /*
   * Test that the wait time between two service journeys is more than the warning limit.
   * Waiting Limit is 1 hour
   */
  @Test
  void testWaitTimeExceedingWarningLimitWithDayOffset_datedServiceJourneys() {
    TestInterchange testInterchange = new TestInterchange();
    testInterchange
      .withFromServiceJourney(
        testInterchange
          .newTestServiceJourney(1)
          .withScheduledStopPointId(1)
          .addTestOperatingDays(1, LocalDate.of(2024, 11, 2))
          .withDepartureTime(LocalTime.of(9, 53, 0))
      )
      .withToServiceJourney(
        testInterchange
          .newTestServiceJourney(2)
          .withScheduledStopPointId(2)
          .addTestOperatingDays(2, LocalDate.of(2024, 11, 1))
          .withArrivalTime(LocalTime.of(10, 55, 0)) // waiting time 1 hour and 2 minutes
          .withArrivalDayOffset(1)
      );

    testInterchange.doMock();
    ValidationReport validationReport = testInterchange.runTest();

    assertThat(validationReport.getValidationReportEntries().size(), is(1));

    assertThat(
      validationReport
        .getValidationReportEntries()
        .stream()
        .findFirst()
        .map(ValidationReportEntry::getName)
        .orElse(null),
      is("Wait time in interchange exceeds warning limit")
    );
  }

  /*
   * Test that the wait time between two service journeys is more than the maximum limit.
   * Maximum Limit is 3 hours
   */
  @Test
  void testWaitTimeExceedingErrorLimit_datedServiceJourneys() {
    TestInterchange testInterchange = new TestInterchange();
    testInterchange
      .withFromServiceJourney(
        testInterchange
          .newTestServiceJourney(1)
          .withScheduledStopPointId(1)
          .addTestOperatingDays(1, LocalDate.of(2024, 11, 1))
          .withDepartureTime(LocalTime.of(9, 53, 0))
      )
      .withToServiceJourney(
        testInterchange
          .newTestServiceJourney(2)
          .withScheduledStopPointId(2)
          .addTestOperatingDays(2, LocalDate.of(2024, 11, 1))
          .withArrivalTime(LocalTime.of(12, 55, 0)) // waiting time 3 hours 2 minutes
      );

    testInterchange.doMock();
    ValidationReport validationReport = testInterchange.runTest();

    assertThat(validationReport.getValidationReportEntries().size(), is(1));

    assertThat(
      validationReport
        .getValidationReportEntries()
        .stream()
        .findFirst()
        .map(ValidationReportEntry::getName)
        .orElse(null),
      is("Wait time in interchange exceeds maximum limit")
    );
  }

  /*
   * Test that the wait time between two service journeys is less than the maximum limit.
   * Maximum Limit is 3 hours
   */
  @Test
  void testWaitTimeExceedingErrorLimitWithDayOffset_datedServiceJourneys() {
    TestInterchange testInterchange = new TestInterchange();

    testInterchange
      .withFromServiceJourney(
        testInterchange
          .newTestServiceJourney(1)
          .withScheduledStopPointId(1)
          .addTestOperatingDays(1, LocalDate.of(2024, 11, 1))
          .withDepartureTime(LocalTime.of(9, 53, 0))
          .withDepartureDayOffset(1)
      )
      .withToServiceJourney(
        testInterchange
          .newTestServiceJourney(2)
          .withScheduledStopPointId(2)
          .addTestOperatingDays(2, LocalDate.of(2024, 11, 2))
          .withArrivalTime(LocalTime.of(12, 55, 0)) // waiting time 3 hours 2 minutes
      );

    testInterchange.doMock();
    ValidationReport validationReport = testInterchange.runTest();

    assertThat(validationReport.getValidationReportEntries().size(), is(1));

    assertThat(
      validationReport
        .getValidationReportEntries()
        .stream()
        .findFirst()
        .map(ValidationReportEntry::getName)
        .orElse(null),
      is("Wait time in interchange exceeds maximum limit")
    );
  }

  @Test
  void testNoSharedActiveDays_datedServiceJourneys() {
    TestInterchange testInterchange = new TestInterchange();
    testInterchange
      .withFromServiceJourney(
        testInterchange
          .newTestServiceJourney(1)
          .withScheduledStopPointId(1)
          .addTestOperatingDays(1, LocalDate.of(2024, 11, 1))
          .withDepartureTime(LocalTime.of(9, 53, 0))
      )
      .withToServiceJourney(
        testInterchange
          .newTestServiceJourney(2)
          .withScheduledStopPointId(2)
          .addTestOperatingDays(2, LocalDate.of(2024, 11, 2))
          .withArrivalTime(LocalTime.of(9, 56, 0))
      );

    testInterchange.doMock();

    ValidationReport validationReport = testInterchange.runTest();

    assertThat(validationReport.getValidationReportEntries().size(), is(1));

    assertThat(
      validationReport
        .getValidationReportEntries()
        .stream()
        .findFirst()
        .map(ValidationReportEntry::getName)
        .orElse(null),
      is("No shared active date found in interchange")
    );
  }

  @Test
  void testNoDatedServiceJourneyOrDayTypeExistsForConsumerServiceJourney() {
    TestInterchange testInterchange = new TestInterchange();
    testInterchange
      .withFromServiceJourney(
        testInterchange
          .newTestServiceJourney(1)
          .withScheduledStopPointId(1)
          .addTestDayType(1, LocalDate.of(2024, 11, 1))
          .withDepartureTime(LocalTime.of(9, 53, 0))
      )
      .withToServiceJourney(
        testInterchange
          .newTestServiceJourney(2)
          .withScheduledStopPointId(2)
          .withArrivalTime(LocalTime.of(9, 56, 0))
      );

    testInterchange.doMock();

    ValidationReport validationReport = testInterchange.runTest();

    assertThat(validationReport.getValidationReportEntries().size(), is(1));

    assertThat(
      validationReport
        .getValidationReportEntries()
        .stream()
        .findFirst()
        .map(ValidationReportEntry::getName)
        .orElse(null),
      is("No shared active date found in interchange")
    );
  }

  @Test
  void testNoDatedServiceJourneyOrDayTypeExistsForFeederServiceJourney() {
    TestInterchange testInterchange = new TestInterchange();
    testInterchange
      .withFromServiceJourney(
        testInterchange
          .newTestServiceJourney(5)
          .withScheduledStopPointId(1)
          .withDepartureTime(LocalTime.of(9, 53, 0))
      )
      .withToServiceJourney(
        testInterchange
          .newTestServiceJourney(6)
          .withScheduledStopPointId(2)
          .addTestOperatingDays(2, LocalDate.of(2024, 11, 1))
          .withArrivalTime(LocalTime.of(9, 56, 0))
      );

    testInterchange.doMock();

    ValidationReport validationReport = testInterchange.runTest();

    assertThat(validationReport.getValidationReportEntries().size(), is(1));

    assertThat(
      validationReport
        .getValidationReportEntries()
        .stream()
        .findFirst()
        .map(ValidationReportEntry::getName)
        .orElse(null),
      is("No shared active date found in interchange")
    );
  }

  @Test
  void testNoDatedServiceJourneyOrDayTypeExistsForBothServiceJourneys() {
    TestInterchange testInterchange = new TestInterchange();
    testInterchange
      .withFromServiceJourney(
        testInterchange
          .newTestServiceJourney(5)
          .withScheduledStopPointId(1)
          .withDepartureTime(LocalTime.of(9, 53, 0))
      )
      .withToServiceJourney(
        testInterchange
          .newTestServiceJourney(6)
          .withScheduledStopPointId(2)
          .withArrivalTime(LocalTime.of(9, 56, 0))
      );

    testInterchange.doMock();

    ValidationReport validationReport = testInterchange.runTest();

    assertThat(validationReport.getValidationReportEntries().size(), is(1));

    assertThat(
      validationReport
        .getValidationReportEntries()
        .stream()
        .findFirst()
        .map(ValidationReportEntry::getName)
        .orElse(null),
      is("No shared active date found in interchange")
    );
  }
}
