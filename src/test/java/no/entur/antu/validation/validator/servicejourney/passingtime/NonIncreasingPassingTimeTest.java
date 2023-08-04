package no.entur.antu.validation.validator.servicejourney.passingtime;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalTime;
import no.entur.antu.netextestdata.NetexTestFragment;
import no.entur.antu.validation.ValidationContextWithNetexEntitiesIndex;
import no.entur.antu.validation.validator.servicejourney.passingtime.NonIncreasingPassingTimeError.RuleCode;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.entur.netex.validation.validator.ValidationReportEntrySeverity;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.JourneyPattern_VersionStructure;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.StopPointInJourneyPattern;
import org.rutebanken.netex.model.TimetabledPassingTime;

class NonIncreasingPassingTimeTest {

  @Test
  void testValidateServiceJourneyWithRegularStop() {
    NetexTestFragment testData = new NetexTestFragment();
    JourneyPattern journeyPattern = testData.journeyPattern().create();
    ServiceJourney serviceJourney = testData
      .serviceJourney(journeyPattern)
      .create();

    ValidationReport validationReport = setupAndRunValidation(
      testData.netexEntitiesIndex(journeyPattern, serviceJourney).create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void testValidateServiceJourneyWithRegularStopMissingTime() {
    NetexTestFragment testData = new NetexTestFragment();
    JourneyPattern journeyPattern = testData.journeyPattern().create();
    ServiceJourney serviceJourney = testData
      .serviceJourney(journeyPattern)
      .create();

    // remove arrival time and departure time
    TimetabledPassingTime timetabledPassingTime = getFirstPassingTime(
      serviceJourney
    );
    timetabledPassingTime.withArrivalTime(null).withDepartureTime(null);

    ValidationReport validationReport = setupAndRunValidation(
      testData.netexEntitiesIndex(journeyPattern, serviceJourney).create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
    assertThat(
      validationReport
        .getNumberOfValidationEntriesPerRule()
        .containsKey(
          RuleCode.TIMETABLED_PASSING_TIME_INCOMPLETE_TIME.toString()
        ),
      is(true)
    );
  }

  @Test
  void testValidateServiceJourneyWithRegularStopInconsistentTime() {
    NetexTestFragment testData = new NetexTestFragment();
    JourneyPattern journeyPattern = testData.journeyPattern().create();
    ServiceJourney serviceJourney = testData
      .serviceJourney(journeyPattern)
      .create();

    // set arrival time after departure time
    TimetabledPassingTime timetabledPassingTime = getFirstPassingTime(
      serviceJourney
    );
    timetabledPassingTime.withArrivalTime(
      timetabledPassingTime.getDepartureTime().plusMinutes(1)
    );

    ValidationReport validationReport = setupAndRunValidation(
      testData.netexEntitiesIndex(journeyPattern, serviceJourney).create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
    assertThat(
      validationReport
        .getNumberOfValidationEntriesPerRule()
        .containsKey(
          RuleCode.TIMETABLED_PASSING_TIME_INCONSISTENT_TIME.toString()
        ),
      is(true)
    );
  }

  @Test
  void testValidateServiceJourneyWithAreaStop() {
    NetexTestFragment testData = new NetexTestFragment();
    JourneyPattern journeyPattern = testData.journeyPattern().create();
    ServiceJourney serviceJourney = testData
      .serviceJourney(journeyPattern)
      .create();

    // remove arrival time and departure time and add flex window
    getFirstPassingTime(serviceJourney)
      .withArrivalTime(null)
      .withDepartureTime(null)
      .withEarliestDepartureTime(LocalTime.MIDNIGHT)
      .withLatestArrivalTime(LocalTime.MIDNIGHT.plusMinutes(1));

    NetexEntitiesIndex netexEntitiesIndex = testData
      .netexEntitiesIndex(journeyPattern, serviceJourney)
      .create();

    netexEntitiesIndex
      .getFlexibleStopPlaceIdByStopPointRefIndex()
      .put(getFirstScheduledStopPointRef(journeyPattern), "");

    ValidationReport validationReport = setupAndRunValidation(
      netexEntitiesIndex
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void testValidateServiceJourneyWithAreaStopMissingTimeWindow() {
    NetexTestFragment testData = new NetexTestFragment();
    JourneyPattern journeyPattern = testData.journeyPattern().create();
    ServiceJourney serviceJourney = testData
      .serviceJourney(journeyPattern)
      .create();

    // remove arrival time and departure time and add flex window
    getFirstPassingTime(serviceJourney)
      .withArrivalTime(null)
      .withDepartureTime(null);

    NetexEntitiesIndex netexEntitiesIndex = testData
      .netexEntitiesIndex(journeyPattern, serviceJourney)
      .create();

    netexEntitiesIndex
      .getFlexibleStopPlaceIdByStopPointRefIndex()
      .put(getFirstScheduledStopPointRef(journeyPattern), "");

    ValidationReport validationReport = setupAndRunValidation(
      netexEntitiesIndex
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
    assertThat(
      validationReport
        .getNumberOfValidationEntriesPerRule()
        .containsKey(
          RuleCode.TIMETABLED_PASSING_TIME_INCOMPLETE_TIME.toString()
        ),
      is(true)
    );
  }

  @Test
  void testValidateServiceJourneyWithAreaStopInconsistentTimeWindow() {
    NetexTestFragment testData = new NetexTestFragment();
    JourneyPattern journeyPattern = testData.journeyPattern().create();
    ServiceJourney serviceJourney = testData
      .serviceJourney(journeyPattern)
      .create();

    // remove arrival time and departure time and add flex window
    getFirstPassingTime(serviceJourney)
      .withArrivalTime(null)
      .withDepartureTime(null)
      .withEarliestDepartureTime(LocalTime.MIDNIGHT.plusMinutes(1))
      .withLatestArrivalTime(LocalTime.MIDNIGHT);

    NetexEntitiesIndex netexEntitiesIndex = testData
      .netexEntitiesIndex(journeyPattern, serviceJourney)
      .create();

    netexEntitiesIndex
      .getFlexibleStopPlaceIdByStopPointRefIndex()
      .put(getFirstScheduledStopPointRef(journeyPattern), "");

    ValidationReport validationReport = setupAndRunValidation(
      netexEntitiesIndex
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
    assertThat(
      validationReport
        .getNumberOfValidationEntriesPerRule()
        .containsKey(
          RuleCode.TIMETABLED_PASSING_TIME_INCONSISTENT_TIME.toString()
        ),
      is(true)
    );
  }

  @Test
  void testValidateServiceJourneyWithRegularStopFollowedByRegularStopNonIncreasingTime() {
    NetexTestFragment testData = new NetexTestFragment();
    JourneyPattern journeyPattern = testData.journeyPattern().create();
    ServiceJourney serviceJourney = testData
      .serviceJourney(journeyPattern)
      .create();

    // remove arrival time and departure time and add flex window on second stop
    TimetabledPassingTime firstPassingTime = getFirstPassingTime(
      serviceJourney
    );
    TimetabledPassingTime secondPassingTime = getSecondPassingTime(
      serviceJourney
    );
    secondPassingTime.withArrivalTime(
      firstPassingTime.getDepartureTime().minusMinutes(1)
    );

    NetexEntitiesIndex netexEntitiesIndex = testData
      .netexEntitiesIndex(journeyPattern, serviceJourney)
      .create();

    ValidationReport validationReport = setupAndRunValidation(
      netexEntitiesIndex
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
    assertThat(
      validationReport
        .getNumberOfValidationEntriesPerRule()
        .containsKey(
          RuleCode.TIMETABLED_PASSING_TIME_NON_INCREASING_TIME.toString()
        ),
      is(true)
    );
  }

  /**
   * This test makes sure all passing times are complete and consistent, before it checks for
   * increasing times.
   */
  @Test
  void testValidateWithRegularStopFollowedByRegularStopWithMissingTime() {
    NetexTestFragment testData = new NetexTestFragment();
    JourneyPattern journeyPattern = testData.journeyPattern().create();
    ServiceJourney serviceJourney = testData
      .serviceJourney(journeyPattern)
      .create();

    // Set arrivalTime AFTER departure time (not valid)
    getSecondPassingTime(serviceJourney)
      .withDepartureTime(null)
      .withArrivalTime(null);

    NetexEntitiesIndex netexEntitiesIndex = testData
      .netexEntitiesIndex(journeyPattern, serviceJourney)
      .create();

    ValidationReport validationReport = setupAndRunValidation(
      netexEntitiesIndex
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
    assertThat(
      validationReport
        .getNumberOfValidationEntriesPerRule()
        .containsKey(
          RuleCode.TIMETABLED_PASSING_TIME_INCOMPLETE_TIME.toString()
        ),
      is(true)
    );
  }

  @Test
  void testValidateServiceJourneyWithRegularStopFollowedByStopArea() {
    NetexTestFragment testData = new NetexTestFragment();
    JourneyPattern journeyPattern = testData.journeyPattern().create();
    ServiceJourney serviceJourney = testData
      .serviceJourney(journeyPattern)
      .create();

    // remove arrival time and departure time and add flex window on second stop
    TimetabledPassingTime timetabledPassingTime = getSecondPassingTime(
      serviceJourney
    );
    timetabledPassingTime
      .withEarliestDepartureTime(timetabledPassingTime.getDepartureTime())
      .withLatestArrivalTime(
        timetabledPassingTime.getDepartureTime().plusMinutes(1)
      )
      .withArrivalTime(null)
      .withDepartureTime(null);

    NetexEntitiesIndex netexEntitiesIndex = testData
      .netexEntitiesIndex(journeyPattern, serviceJourney)
      .create();

    netexEntitiesIndex
      .getFlexibleStopPlaceIdByStopPointRefIndex()
      .put(getSecondScheduledStopPointRef(journeyPattern), "");

    ValidationReport validationReport = setupAndRunValidation(
      netexEntitiesIndex
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void testValidateServiceJourneyWithRegularStopFollowedByStopAreaNonIncreasingTime() {
    NetexTestFragment testData = new NetexTestFragment();
    JourneyPattern journeyPattern = testData.journeyPattern().create();
    ServiceJourney serviceJourney = testData
      .serviceJourney(journeyPattern)
      .create();

    // remove arrival time and departure time and add flex window with decreasing time on second stop
    TimetabledPassingTime firstPassingTime = getFirstPassingTime(
      serviceJourney
    );
    TimetabledPassingTime secondPassingTime = getSecondPassingTime(
      serviceJourney
    );
    secondPassingTime
      .withEarliestDepartureTime(
        firstPassingTime.getDepartureTime().minusMinutes(1)
      )
      .withArrivalTime(null)
      .withDepartureTime(null);

    NetexEntitiesIndex netexEntitiesIndex = testData
      .netexEntitiesIndex(journeyPattern, serviceJourney)
      .create();

    netexEntitiesIndex
      .getFlexibleStopPlaceIdByStopPointRefIndex()
      .put(getFirstScheduledStopPointRef(journeyPattern), "");

    ValidationReport validationReport = setupAndRunValidation(
      netexEntitiesIndex
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
    assertThat(
      validationReport
        .getNumberOfValidationEntriesPerRule()
        .containsKey(
          RuleCode.TIMETABLED_PASSING_TIME_INCOMPLETE_TIME.toString()
        ),
      is(true)
    );
  }

  @Test
  void testValidateServiceJourneyWithStopAreaFollowedByRegularStop() {
    NetexTestFragment testData = new NetexTestFragment();
    JourneyPattern journeyPattern = testData.journeyPattern().create();
    ServiceJourney serviceJourney = testData
      .serviceJourney(journeyPattern)
      .create();

    // remove arrival time and departure time and add flex window on first stop
    TimetabledPassingTime firstPassingTime = getFirstPassingTime(
      serviceJourney
    );
    firstPassingTime
      .withEarliestDepartureTime(firstPassingTime.getDepartureTime())
      .withLatestArrivalTime(firstPassingTime.getDepartureTime())
      .withArrivalTime(null)
      .withDepartureTime(null);

    NetexEntitiesIndex netexEntitiesIndex = testData
      .netexEntitiesIndex(journeyPattern, serviceJourney)
      .create();

    netexEntitiesIndex
      .getFlexibleStopPlaceIdByStopPointRefIndex()
      .put(getFirstScheduledStopPointRef(journeyPattern), "");

    ValidationReport validationReport = setupAndRunValidation(
      netexEntitiesIndex
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void testValidateServiceJourneyWithStopAreaFollowedByStopArea() {
    NetexTestFragment testData = new NetexTestFragment();
    JourneyPattern journeyPattern = testData.journeyPattern().create();
    ServiceJourney serviceJourney = testData
      .serviceJourney(journeyPattern)
      .create();

    // remove arrival time and departure time and add flex window on first stop
    TimetabledPassingTime firstPassingTime = getFirstPassingTime(
      serviceJourney
    );
    firstPassingTime
      .withEarliestDepartureTime(firstPassingTime.getDepartureTime())
      .withLatestArrivalTime(firstPassingTime.getDepartureTime())
      .withArrivalTime(null)
      .withDepartureTime(null);

    TimetabledPassingTime secondPassingTime = getSecondPassingTime(
      serviceJourney
    );
    secondPassingTime
      .withEarliestDepartureTime(secondPassingTime.getDepartureTime())
      .withLatestArrivalTime(
        secondPassingTime.getDepartureTime().plusMinutes(1)
      )
      .withArrivalTime(null)
      .withDepartureTime(null);

    NetexEntitiesIndex netexEntitiesIndex = testData
      .netexEntitiesIndex(journeyPattern, serviceJourney)
      .create();

    netexEntitiesIndex
      .getFlexibleStopPlaceIdByStopPointRefIndex()
      .put(getFirstScheduledStopPointRef(journeyPattern), "");

    netexEntitiesIndex
      .getFlexibleStopPlaceIdByStopPointRefIndex()
      .put(getSecondScheduledStopPointRef(journeyPattern), "");

    ValidationReport validationReport = setupAndRunValidation(
      netexEntitiesIndex
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void testValidateServiceJourneyWithStopAreaFollowedByStopAreaNonIncreasingTime() {
    NetexTestFragment testData = new NetexTestFragment();
    JourneyPattern journeyPattern = testData.journeyPattern().create();
    ServiceJourney serviceJourney = testData
      .serviceJourney(journeyPattern)
      .create();

    // remove arrival time and departure time and add flex window on first stop and second stop
    // and add decreasing time on second stop
    TimetabledPassingTime firstPassingTime = getFirstPassingTime(
      serviceJourney
    );
    firstPassingTime
      .withEarliestDepartureTime(firstPassingTime.getDepartureTime())
      .withLatestArrivalTime(firstPassingTime.getDepartureTime())
      .withArrivalTime(null)
      .withDepartureTime(null);

    TimetabledPassingTime secondPassingTime = getSecondPassingTime(
      serviceJourney
    );
    secondPassingTime
      .withEarliestDepartureTime(
        firstPassingTime.getEarliestDepartureTime().minusMinutes(1)
      )
      .withLatestArrivalTime(
        secondPassingTime.getEarliestDepartureTime().plusMinutes(1)
      )
      .withArrivalTime(null)
      .withDepartureTime(null);

    firstPassingTime
      .withEarliestDepartureTime(firstPassingTime.getDepartureTime())
      .withLatestArrivalTime(firstPassingTime.getDepartureTime())
      .withArrivalTime(null)
      .withDepartureTime(null);

    NetexEntitiesIndex netexEntitiesIndex = testData
      .netexEntitiesIndex(journeyPattern, serviceJourney)
      .create();

    netexEntitiesIndex
      .getFlexibleStopPlaceIdByStopPointRefIndex()
      .put(getFirstScheduledStopPointRef(journeyPattern), "");

    ValidationReport validationReport = setupAndRunValidation(
      netexEntitiesIndex
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
    assertThat(
      validationReport
        .getNumberOfValidationEntriesPerRule()
        .containsKey(
          RuleCode.TIMETABLED_PASSING_TIME_INCOMPLETE_TIME.toString()
        ),
      is(true)
    );
  }

  @Test
  void testValidateServiceJourneyWithStopAreaFollowedByRegularStopNonIncreasingTime() {
    NetexTestFragment testData = new NetexTestFragment();
    JourneyPattern journeyPattern = testData.journeyPattern().create();
    ServiceJourney serviceJourney = testData
      .serviceJourney(journeyPattern)
      .create();

    // remove arrival time and departure time and add flex window on first stop
    // and add decreasing time on second stop
    TimetabledPassingTime firstPassingTime = getFirstPassingTime(
      serviceJourney
    );
    firstPassingTime
      .withEarliestDepartureTime(firstPassingTime.getDepartureTime())
      .withLatestArrivalTime(firstPassingTime.getDepartureTime())
      .withArrivalTime(null)
      .withDepartureTime(null);

    TimetabledPassingTime secondPassingTime = getSecondPassingTime(
      serviceJourney
    );
    secondPassingTime
      .withArrivalTime(firstPassingTime.getLatestArrivalTime().minusMinutes(1))
      .withDepartureTime(null);

    NetexEntitiesIndex netexEntitiesIndex = testData
      .netexEntitiesIndex(journeyPattern, serviceJourney)
      .create();

    netexEntitiesIndex
      .getFlexibleStopPlaceIdByStopPointRefIndex()
      .put(getFirstScheduledStopPointRef(journeyPattern), "");

    ValidationReport validationReport = setupAndRunValidation(
      netexEntitiesIndex
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
    assertThat(
      validationReport
        .getNumberOfValidationEntriesPerRule()
        .containsKey(
          RuleCode.TIMETABLED_PASSING_TIME_NON_INCREASING_TIME.toString()
        ),
      is(true)
    );
  }

  private static ValidationReport setupAndRunValidation(
    NetexEntitiesIndex netexEntitiesIndex
  ) {
    NonIncreasingPassingTime nonIncreasingPassingTime =
      new NonIncreasingPassingTime((code, message, dataLocation) ->
        new ValidationReportEntry(
          message,
          code,
          ValidationReportEntrySeverity.ERROR
        )
      );

    ValidationReport testValidationReport = new ValidationReport(
      "TST",
      "Test1122"
    );

    ValidationContextWithNetexEntitiesIndex validationContext = mock(
      ValidationContextWithNetexEntitiesIndex.class
    );
    when(validationContext.getNetexEntitiesIndex())
      .thenReturn(netexEntitiesIndex);

    nonIncreasingPassingTime.validate(testValidationReport, validationContext);

    return testValidationReport;
  }

  private static TimetabledPassingTime getPassingTime(
    ServiceJourney serviceJourney,
    int order
  ) {
    return serviceJourney
      .getPassingTimes()
      .getTimetabledPassingTime()
      .get(order);
  }

  private static TimetabledPassingTime getFirstPassingTime(
    ServiceJourney serviceJourney
  ) {
    return getPassingTime(serviceJourney, 0);
  }

  private static TimetabledPassingTime getSecondPassingTime(
    ServiceJourney serviceJourney
  ) {
    return getPassingTime(serviceJourney, 1);
  }

  private static String getScheduledStopPointRef(
    JourneyPattern_VersionStructure journeyPattern,
    int order
  ) {
    StopPointInJourneyPattern stopPointInJourneyPattern =
      (StopPointInJourneyPattern) journeyPattern
        .getPointsInSequence()
        .getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern()
        .get(order);

    return stopPointInJourneyPattern
      .getScheduledStopPointRef()
      .getValue()
      .getRef();
  }

  private static String getFirstScheduledStopPointRef(
    JourneyPattern_VersionStructure journeyPattern
  ) {
    return getScheduledStopPointRef(journeyPattern, 0);
  }

  private static String getSecondScheduledStopPointRef(
    JourneyPattern_VersionStructure journeyPattern
  ) {
    return getScheduledStopPointRef(journeyPattern, 1);
  }
}
