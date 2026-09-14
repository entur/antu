package no.entur.antu.validation.validator.servicejourney.passingtime;

import static no.entur.antu.netex.test.ValidationIssueAssertions.assertHasIssuesForRule;
import static no.entur.antu.netex.test.ValidationIssueAssertions.assertNoIssuesForRule;
import static no.entur.antu.validation.validator.servicejourney.passingtime.NonIncreasingPassingTimeValidator.*;

import java.time.LocalTime;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import no.entur.antu.netex.test.NetexTestData;
import no.entur.antu.netex.test.ValidatorTestBase;
import no.entur.antu.netex.test.builder.JourneyPatternBuilder;
import no.entur.antu.netex.test.builder.ServiceJourneyBuilder;
import no.entur.antu.netex.test.builder.StopPointInJourneyPatternBuilder;
import no.entur.antu.netex.test.builder.TimetabledPassingTimeBuilder;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationIssue;
import org.junit.jupiter.api.Test;

class NonIncreasingTimetabledPassingTimeValidatorTest
  extends ValidatorTestBase {

  private static final String STOP_POINT_1 = "TST:ScheduledStopPoint:1";
  private static final String STOP_POINT_2 = "TST:ScheduledStopPoint:2";
  private static final String FLEXIBLE_STOP_PLACE = "TST:FlexibleStopPlace:1";

  /**
   * A journey pattern over four stop points, and a service journey over it whose passing times the
   * caller can still adjust before the data is built.
   */
  private record TestJourney(
    NetexTestData data,
    List<TimetabledPassingTimeBuilder> passingTimes
  ) {
    TimetabledPassingTimeBuilder passingTime(int oneBasedIndex) {
      return passingTimes.get(oneBasedIndex - 1);
    }
  }

  /**
   * Four regular stops departing at 05:00, 05:04, 05:10 and 05:15 — complete, consistent and
   * increasing.
   */
  private TestJourney journeyWithFourStopTimes() {
    int[] departureMinutes = { 0, 4, 10, 15 };
    NetexTestData data = new NetexTestData();
    JourneyPatternBuilder journeyPattern = data.addJourneyPattern();
    List<StopPointInJourneyPatternBuilder> stopPoints =
      journeyPattern.addStopPoints(departureMinutes.length);
    ServiceJourneyBuilder serviceJourney = data.addServiceJourney(
      journeyPattern
    );

    List<TimetabledPassingTimeBuilder> passingTimes = IntStream
      .rangeClosed(1, departureMinutes.length)
      .mapToObj(index ->
        serviceJourney
          .addTimetabledPassingTime(index, stopPoints.get(index - 1))
          .withDepartureTime(LocalTime.of(5, departureMinutes[index - 1]))
      )
      .toList();

    return new TestJourney(data, passingTimes);
  }

  /**
   * The same four stop point journey pattern, but a service journey with a single passing time at
   * the first stop and no times on it at all.
   */
  private TestJourney journeyWithOneStopTime() {
    NetexTestData data = new NetexTestData();
    JourneyPatternBuilder journeyPattern = data.addJourneyPattern();
    List<StopPointInJourneyPatternBuilder> stopPoints =
      journeyPattern.addStopPoints(4);
    ServiceJourneyBuilder serviceJourney = data.addServiceJourney(
      journeyPattern
    );

    return new TestJourney(
      data,
      List.of(serviceJourney.addTimetabledPassingTime(1, stopPoints.get(0)))
    );
  }

  private List<ValidationIssue> validate(NetexEntitiesIndex index) {
    return validateLineFile(index, new NonIncreasingPassingTimeValidator());
  }

  /**
   * Validate a journey whose flexible stops are assigned by the line file's own FlexibleStopPlace,
   * which lands in the entity index.
   */
  private List<ValidationIssue> withFlexibleStopsFromLineFile(
    Supplier<TestJourney> journey,
    String... scheduledStopPointRefs
  ) {
    NetexTestData data = journey.get().data();
    data
      .addFlexibleStopPlace(1)
      .withScheduledStopPointRefs(scheduledStopPointRefs)
      .addFlexibleArea(1);
    return validate(data.build());
  }

  /**
   * Validate the same journey with its flexible stops assigned by a shared file instead, which
   * lands in the common data repository. JAXBValidationContext resolves both sources, and this
   * validator must not care which one an assignment came from.
   */
  private List<ValidationIssue> withFlexibleStopsFromSharedFile(
    Supplier<TestJourney> journey,
    String... scheduledStopPointRefs
  ) {
    NetexTestData data = journey.get().data();
    for (String scheduledStopPointRef : scheduledStopPointRefs) {
      commonDataRepository.putFlexibleStopPlaceRef(
        scheduledStopPointRef,
        FLEXIBLE_STOP_PLACE
      );
    }
    return validate(data.build());
  }

  @Test
  void testValidateServiceJourneyWithCompleteStopTimes() {
    List<ValidationIssue> issues = validate(
      journeyWithFourStopTimes().data().build()
    );

    assertNoIssuesForRule(issues, RULE_INCOMPLETE_TIME);
  }

  @Test
  void testValidateServiceJourneyWithIncompleteStopTimesForRegularStop() {
    List<ValidationIssue> issues = validate(
      journeyWithOneStopTime().data().build()
    );

    assertHasIssuesForRule(issues, RULE_INCOMPLETE_TIME);
  }

  @Test
  void testValidateServiceJourneyWithInconsistentStopTimesForRegularStop() {
    TestJourney journey = journeyWithOneStopTime();
    journey
      .passingTime(1)
      .withDepartureTime(LocalTime.of(12, 12))
      .withArrivalTime(LocalTime.of(12, 13));

    List<ValidationIssue> issues = validate(journey.data().build());

    assertHasIssuesForRule(issues, RULE_INCONSISTENT_TIME);
  }

  @Test
  void testValidateServiceJourneyWithFlexibleStop() {
    Supplier<TestJourney> journey = () -> {
      TestJourney testJourney = journeyWithOneStopTime();
      testJourney
        .passingTime(1)
        .withEarliestDepartureTime(LocalTime.MIDNIGHT)
        .withLatestArrivalTime(LocalTime.MIDNIGHT);
      return testJourney;
    };

    assertNoIssuesForRule(
      withFlexibleStopsFromLineFile(journey, STOP_POINT_1),
      RULE_INCOMPLETE_TIME
    );
    assertNoIssuesForRule(
      withFlexibleStopsFromSharedFile(journey, STOP_POINT_1),
      RULE_INCOMPLETE_TIME
    );
  }

  @Test
  void testValidateServiceJourneyWithIncompletePassingTimeForFlexibleStop() {
    Supplier<TestJourney> journey = () -> {
      TestJourney testJourney = journeyWithOneStopTime();
      testJourney
        .passingTime(1)
        .withEarliestDepartureTime(LocalTime.MIDNIGHT)
        .withLatestArrivalTime(null);
      return testJourney;
    };

    assertHasIssuesForRule(
      withFlexibleStopsFromLineFile(journey, STOP_POINT_1),
      RULE_INCOMPLETE_TIME
    );
    assertHasIssuesForRule(
      withFlexibleStopsFromSharedFile(journey, STOP_POINT_1),
      RULE_INCOMPLETE_TIME
    );
  }

  @Test
  void testValidateServiceJourneyWithInconsistentPassingTimeForFlexibleStop() {
    Supplier<TestJourney> journey = () -> {
      TestJourney testJourney = journeyWithOneStopTime();
      testJourney
        .passingTime(1)
        .withEarliestDepartureTime(LocalTime.MIDNIGHT.plusMinutes(1))
        .withLatestArrivalTime(LocalTime.MIDNIGHT);
      return testJourney;
    };

    assertHasIssuesForRule(
      withFlexibleStopsFromLineFile(journey, STOP_POINT_1),
      RULE_INCONSISTENT_TIME
    );
    assertHasIssuesForRule(
      withFlexibleStopsFromSharedFile(journey, STOP_POINT_1),
      RULE_INCONSISTENT_TIME
    );
  }

  @Test
  void testValidateServiceJourneyWithRegularStopFollowedByRegularStopNonIncreasingTime() {
    TestJourney journey = journeyWithFourStopTimes();
    // arrive at the second stop a minute before departing the first
    journey.passingTime(2).withArrivalTime(LocalTime.of(4, 59));

    List<ValidationIssue> issues = validate(journey.data().build());

    assertHasIssuesForRule(issues, RULE_NON_INCREASING_TIME);
  }

  @Test
  void testValidateServiceJourneyWithStopAreaFollowedByRegularStop() {
    Supplier<TestJourney> journey = () -> {
      TestJourney testJourney = journeyWithFourStopTimes();
      // turn the first stop into a flexible window around its departure time
      testJourney
        .passingTime(1)
        .withEarliestDepartureTime(LocalTime.of(5, 0))
        .withLatestArrivalTime(LocalTime.of(5, 0))
        .withArrivalTime(null)
        .withDepartureTime(null);
      return testJourney;
    };

    List<ValidationIssue> fromLineFile = withFlexibleStopsFromLineFile(
      journey,
      STOP_POINT_1
    );
    assertNoIssuesForRule(fromLineFile, RULE_INCOMPLETE_TIME);
    assertNoIssuesForRule(fromLineFile, RULE_NON_INCREASING_TIME);

    List<ValidationIssue> fromSharedFile = withFlexibleStopsFromSharedFile(
      journey,
      STOP_POINT_1
    );
    assertNoIssuesForRule(fromSharedFile, RULE_INCOMPLETE_TIME);
    assertNoIssuesForRule(fromSharedFile, RULE_NON_INCREASING_TIME);
  }

  @Test
  void testValidateServiceJourneyWithStopAreaFollowedByStopArea() {
    Supplier<TestJourney> journey = () -> {
      TestJourney testJourney = journeyWithFourStopTimes();
      testJourney
        .passingTime(1)
        .withEarliestDepartureTime(LocalTime.of(5, 0))
        .withLatestArrivalTime(LocalTime.of(5, 0))
        .withArrivalTime(null)
        .withDepartureTime(null);
      testJourney
        .passingTime(2)
        .withEarliestDepartureTime(LocalTime.of(5, 5))
        .withLatestArrivalTime(LocalTime.of(5, 4))
        .withArrivalTime(null)
        .withDepartureTime(null);
      return testJourney;
    };

    List<ValidationIssue> fromLineFile = withFlexibleStopsFromLineFile(
      journey,
      STOP_POINT_1,
      STOP_POINT_2
    );
    assertNoIssuesForRule(fromLineFile, RULE_INCOMPLETE_TIME);
    assertNoIssuesForRule(fromLineFile, RULE_NON_INCREASING_TIME);

    List<ValidationIssue> fromSharedFile = withFlexibleStopsFromSharedFile(
      journey,
      STOP_POINT_1,
      STOP_POINT_2
    );
    assertNoIssuesForRule(fromSharedFile, RULE_INCOMPLETE_TIME);
    assertNoIssuesForRule(fromSharedFile, RULE_NON_INCREASING_TIME);
  }

  @Test
  void testValidateServiceJourneyWithStopAreaFollowedByStopAreaNonIncreasingTime() {
    Supplier<TestJourney> journey = () -> {
      TestJourney testJourney = journeyWithFourStopTimes();
      testJourney
        .passingTime(1)
        .withEarliestDepartureTime(LocalTime.of(5, 0))
        .withLatestArrivalTime(LocalTime.of(5, 0))
        .withArrivalTime(null)
        .withDepartureTime(null);
      // the second window opens a minute before the first one does
      testJourney
        .passingTime(2)
        .withEarliestDepartureTime(LocalTime.of(4, 59))
        .withLatestArrivalTime(LocalTime.of(5, 0))
        .withArrivalTime(null)
        .withDepartureTime(null);
      return testJourney;
    };

    assertHasIssuesForRule(
      withFlexibleStopsFromLineFile(journey, STOP_POINT_1, STOP_POINT_2),
      RULE_NON_INCREASING_TIME
    );
    assertHasIssuesForRule(
      withFlexibleStopsFromSharedFile(journey, STOP_POINT_1, STOP_POINT_2),
      RULE_NON_INCREASING_TIME
    );
  }

  @Test
  void testValidateServiceJourneyWithStopAreaFollowedByRegularStopNonIncreasingTime() {
    Supplier<TestJourney> journey = () -> {
      TestJourney testJourney = journeyWithFourStopTimes();
      testJourney
        .passingTime(1)
        .withEarliestDepartureTime(LocalTime.of(5, 0))
        .withLatestArrivalTime(LocalTime.of(5, 0))
        .withArrivalTime(null)
        .withDepartureTime(null);
      // arrive at the regular second stop before the first stop's window closes
      testJourney
        .passingTime(2)
        .withArrivalTime(LocalTime.of(4, 59))
        .withDepartureTime(null);
      return testJourney;
    };

    assertHasIssuesForRule(
      withFlexibleStopsFromLineFile(journey, STOP_POINT_1),
      RULE_NON_INCREASING_TIME
    );
    assertHasIssuesForRule(
      withFlexibleStopsFromSharedFile(journey, STOP_POINT_1),
      RULE_NON_INCREASING_TIME
    );
  }
}
