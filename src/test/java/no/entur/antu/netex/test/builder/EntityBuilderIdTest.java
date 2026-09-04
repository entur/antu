package no.entur.antu.netex.test.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.Month;
import org.junit.jupiter.api.Test;

/**
 * Pins the generated entity ids and refs.
 *
 * <p>Tests across the repo assert on these as string literals, so a change to how an id is
 * derived is a wide, silent breakage. This test exists to fail loudly instead.
 */
class EntityBuilderIdTest {

  private static final LineBuilder LINE = new LineBuilder(1);
  private static final JourneyPatternBuilder JOURNEY_PATTERN =
    new JourneyPatternBuilder(1);

  @Test
  void entityIdsFollowTheTstConvention() {
    assertEquals("TST:Line:1", LINE.ref());
    assertEquals("TST:FlexibleLine:1", new FlexibleLineBuilder(1).ref());
    assertEquals("TST:Route:1", new RouteBuilder(1, LINE).ref());
    assertEquals("TST:JourneyPattern:1", JOURNEY_PATTERN.ref());
    assertEquals(
      "TST:StopPointInJourneyPattern:1",
      new StopPointInJourneyPatternBuilder(1).ref()
    );
    assertEquals(
      "TST:LinkInJourneyPattern:1",
      new LinkInJourneyPatternBuilder(1).ref()
    );
    assertEquals(
      "TST:ServiceJourney:1",
      new ServiceJourneyBuilder(1, LINE, JOURNEY_PATTERN).ref()
    );
    assertEquals(
      "TST:DeadRun:1",
      new DeadRunBuilder(1, LINE, JOURNEY_PATTERN).ref()
    );
    assertEquals(
      "TST:DatedServiceJourney:1",
      new DatedServiceJourneyBuilder(
        1,
        new ServiceJourneyBuilder(1, LINE, JOURNEY_PATTERN),
        new OperatingDayBuilder(1, LocalDate.of(2026, Month.JANUARY, 1))
      )
        .ref()
    );
    assertEquals(
      "TST:ServiceJourneyInterchange:1",
      new ServiceJourneyInterchangeBuilder(1).ref()
    );
    assertEquals(
      "TST:TimetabledPassingTime:1",
      new TimetabledPassingTimeBuilder(
        1,
        new StopPointInJourneyPatternBuilder(1)
      )
        .ref()
    );
    assertEquals("TST:ServiceLink:1", new ServiceLinkBuilder(1).ref());
    assertEquals(
      "TST:FlexibleStopPlace:1",
      new FlexibleStopPlaceBuilder(1).ref()
    );
    assertEquals("TST:FlexibleArea:1", new FlexibleAreaBuilder(1).ref());
    assertEquals(
      "TST:PassengerStopAssignment:1",
      new PassengerStopAssignmentBuilder(1).ref()
    );
    assertEquals(
      "TST:OperatingDay:1",
      new OperatingDayBuilder(1, LocalDate.of(2026, Month.JANUARY, 1)).ref()
    );
    assertEquals("TST:DayType:1", new DayTypeBuilder(1).ref());
    assertEquals(
      "TST:DayTypeAssignment:1",
      new DayTypeAssignmentBuilder(1).ref()
    );
  }

  @Test
  void entityIdsCarryTheGivenNumber() {
    assertEquals("TST:Line:7", new LineBuilder(7).ref());
    assertEquals("TST:JourneyPattern:42", new JourneyPatternBuilder(42).ref());
  }

  @Test
  void refStructuresFollowTheTstConvention() {
    assertEquals(
      "TST:ScheduledStopPoint:1",
      NetexRefs.scheduledStopPointRef(1).getRef()
    );
    assertEquals("TST:Operator:1", NetexRefs.operatorRef(1).getRef());
    assertEquals("TST:Quay:1", NetexRefs.quayRef(1).getRef());
    assertEquals("TST:StopPoint:1", NetexRefs.stopPointRef(1).getRef());
    assertEquals("TST:ServiceLink:1", NetexRefs.serviceLinkRef(1).getRef());
    assertEquals(
      "TST:ServiceJourney:1",
      NetexRefs.serviceJourneyRef(1).getRef()
    );
    assertEquals(
      "TST:DestinationDisplay:1",
      NetexRefs.destinationDisplayRef(1).getRef()
    );
  }

  @Test
  void serviceJourneyRefObjectMatchesItsOwnId() {
    ServiceJourneyBuilder serviceJourney = new ServiceJourneyBuilder(
      3,
      LINE,
      JOURNEY_PATTERN
    );
    assertEquals(serviceJourney.ref(), serviceJourney.refObject().getRef());
  }
}
