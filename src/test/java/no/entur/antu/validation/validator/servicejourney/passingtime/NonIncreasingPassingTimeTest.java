package no.entur.antu.validation.validator.servicejourney.passingtime;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.time.LocalTime;
import no.entur.antu.model.ScheduledStopPointId;
import no.entur.antu.netextestdata.NetexTestFragment;
import no.entur.antu.validation.ValidationTest;
import no.entur.antu.validation.validator.servicejourney.passingtime.NonIncreasingPassingTimeError.RuleCode;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.JourneyPattern_VersionStructure;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.StopPointInJourneyPattern;
import org.rutebanken.netex.model.TimetabledPassingTime;

class NonIncreasingPassingTimeTest extends ValidationTest {

  private ValidationReport runValidation(
    NetexEntitiesIndex netexEntitiesIndex
  ) {
    return runValidationOnLineFile(
      netexEntitiesIndex,
      NonIncreasingPassingTime.class
    );
  }

  @Test
  void testValidateServiceJourneyWithRegularStop() {
    NetexTestFragment testData = new NetexTestFragment();
    JourneyPattern journeyPattern = testData.journeyPattern().create();
    ServiceJourney serviceJourney = testData
      .serviceJourney(journeyPattern)
      .create();

    ValidationReport validationReport = runValidation(
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

    mockGetStopName(getFirstScheduledStopPointId(journeyPattern));
    ValidationReport validationReport = runValidation(
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

    mockGetStopName(getFirstScheduledStopPointId(journeyPattern));
    ValidationReport validationReport = runValidation(
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
      .put(getFirstScheduledStopPointId(journeyPattern).id(), "");

    ValidationReport validationReport = runValidation(netexEntitiesIndex);

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
      .put(getFirstScheduledStopPointId(journeyPattern).id(), "");

    mockGetStopName(getFirstScheduledStopPointId(journeyPattern));
    ValidationReport validationReport = runValidation(netexEntitiesIndex);

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

    ScheduledStopPointId scheduledStopPointId = getFirstScheduledStopPointId(
      journeyPattern
    );
    netexEntitiesIndex
      .getFlexibleStopPlaceIdByStopPointRefIndex()
      .put(scheduledStopPointId.id(), "");

    mockGetStopName(scheduledStopPointId);
    ValidationReport validationReport = runValidation(netexEntitiesIndex);

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

    mockGetStopName(getFirstScheduledStopPointId(journeyPattern));
    ValidationReport validationReport = runValidation(netexEntitiesIndex);

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

    mockGetStopName(getSecondScheduledStopPointId(journeyPattern));
    ValidationReport validationReport = runValidation(netexEntitiesIndex);

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
      .put(getSecondScheduledStopPointId(journeyPattern).id(), "");

    ValidationReport validationReport = runValidation(netexEntitiesIndex);

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

    ScheduledStopPointId scheduledStopPointId = getFirstScheduledStopPointId(
      journeyPattern
    );
    netexEntitiesIndex
      .getFlexibleStopPlaceIdByStopPointRefIndex()
      .put(scheduledStopPointId.id(), "");

    mockGetStopName(scheduledStopPointId);
    ValidationReport validationReport = runValidation(netexEntitiesIndex);

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
      .put(getFirstScheduledStopPointId(journeyPattern).id(), "");

    ValidationReport validationReport = runValidation(netexEntitiesIndex);

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
      .put(getFirstScheduledStopPointId(journeyPattern).id(), "");

    netexEntitiesIndex
      .getFlexibleStopPlaceIdByStopPointRefIndex()
      .put(getSecondScheduledStopPointId(journeyPattern).id(), "");

    ValidationReport validationReport = runValidation(netexEntitiesIndex);

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

    ScheduledStopPointId scheduledStopPointId = getFirstScheduledStopPointId(
      journeyPattern
    );
    netexEntitiesIndex
      .getFlexibleStopPlaceIdByStopPointRefIndex()
      .put(scheduledStopPointId.id(), "");

    mockGetStopName(scheduledStopPointId);
    ValidationReport validationReport = runValidation(netexEntitiesIndex);

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

    ScheduledStopPointId scheduledStopPointId = getFirstScheduledStopPointId(
      journeyPattern
    );
    netexEntitiesIndex
      .getFlexibleStopPlaceIdByStopPointRefIndex()
      .put(scheduledStopPointId.id(), "");

    mockGetStopName(scheduledStopPointId);
    ValidationReport validationReport = runValidation(netexEntitiesIndex);

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

  private static ScheduledStopPointId getScheduledStopPointId(
    JourneyPattern_VersionStructure journeyPattern,
    int order
  ) {
    StopPointInJourneyPattern stopPointInJourneyPattern =
      (StopPointInJourneyPattern) journeyPattern
        .getPointsInSequence()
        .getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern()
        .get(order);

    return ScheduledStopPointId.of(stopPointInJourneyPattern);
  }

  private static ScheduledStopPointId getFirstScheduledStopPointId(
    JourneyPattern_VersionStructure journeyPattern
  ) {
    return getScheduledStopPointId(journeyPattern, 0);
  }

  private static ScheduledStopPointId getSecondScheduledStopPointId(
    JourneyPattern_VersionStructure journeyPattern
  ) {
    return getScheduledStopPointId(journeyPattern, 1);
  }
}
