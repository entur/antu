package no.entur.antu.validation.validator.servicejourney.passingtime;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.time.LocalTime;
import java.util.List;
import java.util.stream.IntStream;
import no.entur.antu.netextestdata.NetexEntitiesTestFactory;
import no.entur.antu.validation.ValidationTest;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.model.ScheduledStopPointId;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.ScheduledStopPointRefStructure;

class NonIncreasingTimetabledPassingTimeValidatorTest extends ValidationTest {

  private ValidationReport runValidation(
    NetexEntitiesIndex netexEntitiesIndex
  ) {
    return runValidationOnLineFile(
      netexEntitiesIndex,
      NonIncreasingPassingTimeValidator.class
    );
  }

  private record Context(
    NetexEntitiesTestFactory netexEntitiesTestFactory,
    List<ScheduledStopPointRefStructure> scheduledStopPointRefs,
    List<NetexEntitiesTestFactory.CreateStopPointInJourneyPattern> stopPointInJourneyPatterns,
    List<NetexEntitiesTestFactory.CreateTimetabledPassingTime> timetabledPassingTimes,
    List<LocalTime> departureTimes
  ) {
    private static final int numberOfStopPointsInJourneyPattern = 4;

    public static Context create() {
      NetexEntitiesTestFactory netexEntitiesTestFactory =
        new NetexEntitiesTestFactory();

      NetexEntitiesTestFactory.CreateJourneyPattern createJourneyPattern =
        netexEntitiesTestFactory.createJourneyPattern();

      NetexEntitiesTestFactory.CreateServiceJourney createServiceJourney =
        netexEntitiesTestFactory.createServiceJourney(createJourneyPattern);

      List<ScheduledStopPointRefStructure> scheduledStopPointRefs = IntStream
        .rangeClosed(1, numberOfStopPointsInJourneyPattern)
        .mapToObj(NetexEntitiesTestFactory::createScheduledStopPointRef)
        .toList();

      List<NetexEntitiesTestFactory.CreateStopPointInJourneyPattern> stopPointInJourneyPatterns =
        IntStream
          .rangeClosed(1, numberOfStopPointsInJourneyPattern)
          .mapToObj(index ->
            createJourneyPattern
              .createStopPointInJourneyPattern(index)
              .withScheduledStopPointRef(scheduledStopPointRefs.get(index - 1))
          )
          .toList();

      List<LocalTime> departureTimes = IntStream
        .rangeClosed(1, numberOfStopPointsInJourneyPattern)
        .mapToObj(index -> LocalTime.of(5, index * 5))
        .toList();

      List<NetexEntitiesTestFactory.CreateTimetabledPassingTime> timetabledPassingTimes =
        IntStream
          .rangeClosed(1, numberOfStopPointsInJourneyPattern)
          .mapToObj(index ->
            createServiceJourney
              .createTimetabledPassingTime(
                index,
                stopPointInJourneyPatterns.get(index - 1)
              )
              .withDepartureTime(departureTimes.get(index - 1))
          )
          .toList();

      return new Context(
        netexEntitiesTestFactory,
        scheduledStopPointRefs,
        stopPointInJourneyPatterns,
        timetabledPassingTimes,
        departureTimes
      );
    }
  }

  @Test
  void testValidateServiceJourneyWithRegularStop() {
    Context context = Context.create();

    ValidationReport validationReport = runValidation(
      context.netexEntitiesTestFactory.create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void testValidateServiceJourneyWithRegularStopMissingTime() {
    Context context = Context.create();

    // remove arrival time and departure time for the first passing time
    context.timetabledPassingTimes
      .get(0)
      .withDepartureTime(null)
      .withArrivalTime(null);
    mockGetStopName(
      ScheduledStopPointId.of(context.scheduledStopPointRefs.get(0))
    );

    ValidationReport validationReport = runValidation(
      context.netexEntitiesTestFactory.create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
    assertThat(
      validationReport
        .getNumberOfValidationEntriesPerRule()
        .containsKey(
          NonIncreasingPassingTimeValidator.RULE_INCOMPLETE_TIME.name()
        ),
      is(true)
    );
  }

  @Test
  void testValidateServiceJourneyWithRegularStopInconsistentTime() {
    Context context = Context.create();

    NetexEntitiesTestFactory.CreateTimetabledPassingTime firstTimetabledPassingTime =
      context.timetabledPassingTimes.get(0);
    // set arrival time after departure time for the first passing time
    firstTimetabledPassingTime.withArrivalTime(
      context.departureTimes.get(0).plusMinutes(1)
    );

    mockGetStopName(
      ScheduledStopPointId.of(context.scheduledStopPointRefs.get(0))
    );

    ValidationReport validationReport = runValidation(
      context.netexEntitiesTestFactory.create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
    assertThat(
      validationReport
        .getNumberOfValidationEntriesPerRule()
        .containsKey(
          NonIncreasingPassingTimeValidator.RULE_INCONSISTENT_TIME.name()
        ),
      is(true)
    );
  }

  @Test
  void testValidateServiceJourneyWithAreaStop() {
    Context context = Context.create();
    // remove arrival time and departure time and add flex window
    context.timetabledPassingTimes
      .get(0)
      .withDepartureTime(null)
      .withArrivalTime(null)
      .withEarliestDepartureTime(LocalTime.MIDNIGHT)
      .withLatestArrivalTime(LocalTime.MIDNIGHT.plusMinutes(1));

    NetexEntitiesIndex netexEntitiesIndex =
      context.netexEntitiesTestFactory.create();

    netexEntitiesIndex
      .getFlexibleStopPlaceIdByStopPointRefIndex()
      .put(context.scheduledStopPointRefs.get(0).getRef(), "");

    ValidationReport validationReport = runValidation(netexEntitiesIndex);

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void testValidateServiceJourneyWithAreaStopMissingTimeWindow() {
    Context context = Context.create();
    // remove arrival time and departure time and add flex window
    context.timetabledPassingTimes
      .get(0)
      .withDepartureTime(null)
      .withArrivalTime(null);

    NetexEntitiesIndex netexEntitiesIndex =
      context.netexEntitiesTestFactory.create();

    netexEntitiesIndex
      .getFlexibleStopPlaceIdByStopPointRefIndex()
      .put(context.scheduledStopPointRefs.get(0).getRef(), "");

    mockGetStopName(
      ScheduledStopPointId.of(context.scheduledStopPointRefs.get(0))
    );
    ValidationReport validationReport = runValidation(netexEntitiesIndex);

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
    assertThat(
      validationReport
        .getNumberOfValidationEntriesPerRule()
        .containsKey(
          NonIncreasingPassingTimeValidator.RULE_INCOMPLETE_TIME.name()
        ),
      is(true)
    );
  }

  @Test
  void testValidateServiceJourneyWithAreaStopInconsistentTimeWindow() {
    Context context = Context.create();
    // remove arrival time and departure time and add flex window
    context.timetabledPassingTimes
      .get(0)
      .withDepartureTime(null)
      .withArrivalTime(null)
      .withEarliestDepartureTime(LocalTime.MIDNIGHT.plusMinutes(1))
      .withLatestArrivalTime(LocalTime.MIDNIGHT);

    NetexEntitiesIndex netexEntitiesIndex =
      context.netexEntitiesTestFactory.create();

    netexEntitiesIndex
      .getFlexibleStopPlaceIdByStopPointRefIndex()
      .put(context.scheduledStopPointRefs.get(0).getRef(), "");

    mockGetStopName(
      ScheduledStopPointId.of(context.scheduledStopPointRefs.get(0))
    );
    ValidationReport validationReport = runValidation(netexEntitiesIndex);

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
    assertThat(
      validationReport
        .getNumberOfValidationEntriesPerRule()
        .containsKey(
          NonIncreasingPassingTimeValidator.RULE_INCONSISTENT_TIME.name()
        ),
      is(true)
    );
  }

  @Test
  void testValidateServiceJourneyWithRegularStopFollowedByRegularStopNonIncreasingTime() {
    Context context = Context.create();
    // remove arrival time and departure time and add flex window on second stop
    context.timetabledPassingTimes
      .get(1)
      .withArrivalTime(context.departureTimes.get(0).minusMinutes(1));

    mockGetStopName(
      ScheduledStopPointId.of(context.scheduledStopPointRefs.get(0))
    );
    ValidationReport validationReport = runValidation(
      context.netexEntitiesTestFactory.create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
    assertThat(
      validationReport
        .getNumberOfValidationEntriesPerRule()
        .containsKey(
          NonIncreasingPassingTimeValidator.RULE_NON_INCREASING_TIME.name()
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
    Context context = Context.create();
    // Set arrivalTime AFTER departure time (not valid)
    context.timetabledPassingTimes
      .get(1)
      .withArrivalTime(null)
      .withDepartureTime(null);
    mockGetStopName(
      ScheduledStopPointId.of(context.scheduledStopPointRefs.get(1))
    );

    ValidationReport validationReport = runValidation(
      context.netexEntitiesTestFactory.create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
    assertThat(
      validationReport
        .getNumberOfValidationEntriesPerRule()
        .containsKey(
          NonIncreasingPassingTimeValidator.RULE_INCOMPLETE_TIME.name()
        ),
      is(true)
    );
  }

  @Test
  void testValidateServiceJourneyWithRegularStopFollowedByStopArea() {
    Context context = Context.create();

    // remove arrival time and departure time and add flex window on second stop
    context.timetabledPassingTimes
      .get(1)
      .withDepartureTime(null)
      .withArrivalTime(null)
      .withEarliestDepartureTime(context.departureTimes().get(1))
      .withLatestArrivalTime(context.departureTimes().get(1).plusMinutes(1));

    NetexEntitiesIndex netexEntitiesIndex =
      context.netexEntitiesTestFactory.create();

    netexEntitiesIndex
      .getFlexibleStopPlaceIdByStopPointRefIndex()
      .put(context.scheduledStopPointRefs.get(1).getRef(), "");

    ValidationReport validationReport = runValidation(netexEntitiesIndex);

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void testValidateServiceJourneyWithRegularStopFollowedByStopAreaNonIncreasingTime() {
    Context context = Context.create();
    // remove arrival time and departure time and add flex window with decreasing time on second stop
    context.timetabledPassingTimes
      .get(1)
      .withEarliestDepartureTime(context.departureTimes.get(0).minusMinutes(1))
      .withArrivalTime(null)
      .withDepartureTime(null);

    NetexEntitiesIndex netexEntitiesIndex =
      context.netexEntitiesTestFactory.create();

    netexEntitiesIndex
      .getFlexibleStopPlaceIdByStopPointRefIndex()
      .put(context.scheduledStopPointRefs.get(0).getRef(), "");

    mockGetStopName(
      ScheduledStopPointId.of(context.scheduledStopPointRefs.get(0))
    );
    ValidationReport validationReport = runValidation(netexEntitiesIndex);

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
    assertThat(
      validationReport
        .getNumberOfValidationEntriesPerRule()
        .containsKey(
          NonIncreasingPassingTimeValidator.RULE_INCOMPLETE_TIME.name()
        ),
      is(true)
    );
  }

  @Test
  void testValidateServiceJourneyWithStopAreaFollowedByRegularStop() {
    Context context = Context.create();

    // remove arrival time and departure time and add flex window on first stop
    context.timetabledPassingTimes
      .get(0)
      .withEarliestDepartureTime(context.departureTimes.get(0))
      .withLatestArrivalTime(context.departureTimes.get(0))
      .withArrivalTime(null)
      .withDepartureTime(null);

    NetexEntitiesIndex netexEntitiesIndex =
      context.netexEntitiesTestFactory.create();

    netexEntitiesIndex
      .getFlexibleStopPlaceIdByStopPointRefIndex()
      .put(context.scheduledStopPointRefs.get(0).getRef(), "");

    ValidationReport validationReport = runValidation(netexEntitiesIndex);

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void testValidateServiceJourneyWithStopAreaFollowedByStopArea() {
    Context context = Context.create();
    context.timetabledPassingTimes
      .get(0)
      .withEarliestDepartureTime(context.departureTimes.get(0))
      .withLatestArrivalTime(context.departureTimes.get(0))
      .withArrivalTime(null)
      .withDepartureTime(null);

    context.timetabledPassingTimes
      .get(1)
      .withEarliestDepartureTime(context.departureTimes.get(1))
      .withLatestArrivalTime(context.departureTimes.get(1).plusMinutes(1))
      .withArrivalTime(null)
      .withDepartureTime(null);

    NetexEntitiesIndex netexEntitiesIndex =
      context.netexEntitiesTestFactory.create();

    netexEntitiesIndex
      .getFlexibleStopPlaceIdByStopPointRefIndex()
      .put(context.scheduledStopPointRefs.get(0).getRef(), "");

    netexEntitiesIndex
      .getFlexibleStopPlaceIdByStopPointRefIndex()
      .put(context.scheduledStopPointRefs.get(1).getRef(), "");

    ValidationReport validationReport = runValidation(netexEntitiesIndex);

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void testValidateServiceJourneyWithStopAreaFollowedByStopAreaNonIncreasingTime() {
    Context context = Context.create();
    // remove arrival time and departure time and add flex window on first stop and second stop
    // and add decreasing time on second stop
    context.timetabledPassingTimes
      .get(0)
      .withEarliestDepartureTime(context.departureTimes.get(0))
      .withLatestArrivalTime(context.departureTimes.get(0))
      .withArrivalTime(null)
      .withDepartureTime(null);

    context.timetabledPassingTimes
      .get(1)
      .withEarliestDepartureTime(context.departureTimes.get(1).minusMinutes(1))
      .withLatestArrivalTime(context.departureTimes.get(1).plusMinutes(1))
      .withArrivalTime(null)
      .withDepartureTime(null);

    NetexEntitiesIndex netexEntitiesIndex =
      context.netexEntitiesTestFactory.create();

    netexEntitiesIndex
      .getFlexibleStopPlaceIdByStopPointRefIndex()
      .put(context.scheduledStopPointRefs.get(0).getRef(), "");

    mockGetStopName(
      ScheduledStopPointId.of(context.scheduledStopPointRefs.get(0))
    );
    ValidationReport validationReport = runValidation(netexEntitiesIndex);

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
    assertThat(
      validationReport
        .getNumberOfValidationEntriesPerRule()
        .containsKey(
          NonIncreasingPassingTimeValidator.RULE_INCOMPLETE_TIME.name()
        ),
      is(true)
    );
  }

  @Test
  void testValidateServiceJourneyWithStopAreaFollowedByRegularStopNonIncreasingTime() {
    Context context = Context.create();

    // remove arrival time and departure time and add flex window on first stop
    // and add decreasing time on second stop

    context.timetabledPassingTimes
      .get(0)
      .withEarliestDepartureTime(context.departureTimes.get(0))
      .withLatestArrivalTime(context.departureTimes.get(0))
      .withArrivalTime(null)
      .withDepartureTime(null);

    context.timetabledPassingTimes
      .get(1)
      .withArrivalTime(context.departureTimes.get(0).minusMinutes(1))
      .withDepartureTime(null);

    NetexEntitiesIndex netexEntitiesIndex =
      context.netexEntitiesTestFactory.create();

    netexEntitiesIndex
      .getFlexibleStopPlaceIdByStopPointRefIndex()
      .put(context.scheduledStopPointRefs.get(0).getRef(), "");

    mockGetStopName(
      ScheduledStopPointId.of(context.scheduledStopPointRefs.get(0))
    );
    ValidationReport validationReport = runValidation(netexEntitiesIndex);

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
    assertThat(
      validationReport
        .getNumberOfValidationEntriesPerRule()
        .containsKey(
          NonIncreasingPassingTimeValidator.RULE_NON_INCREASING_TIME.name()
        ),
      is(true)
    );
  }
}
