package no.entur.antu.validation.validator.passengerstopassignemnt;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.stream.IntStream;
import no.entur.antu.netex.test.NetexTestData;
import no.entur.antu.netex.test.ValidatorTestBase;
import no.entur.antu.netex.test.builder.JourneyPatternBuilder;
import no.entur.antu.netex.test.builder.NetexRefs;
import no.entur.antu.validation.validator.passengerstopassignment.MissingPassengerStopAssignmentValidator;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.model.QuayId;
import org.entur.netex.validation.validator.model.ScheduledStopPointId;
import org.junit.jupiter.api.Test;

class MissingPassengerStopAssignmentValidatorTest extends ValidatorTestBase {

  private ValidationReport runValidation(
    NetexEntitiesIndex netexEntitiesIndex
  ) {
    return runValidationOnLineFile(
      netexEntitiesIndex,
      new MissingPassengerStopAssignmentValidator()
    );
  }

  @Test
  void testAllStopPlaceAssignmentsExists() {
    NetexTestData netexEntitiesTestFactory = new NetexTestData();

    int numberOfStopPointsInJourneyPattern = 4;

    JourneyPatternBuilder journeyPatternBuilder =
      netexEntitiesTestFactory.addJourneyPattern();
    journeyPatternBuilder.addStopPoints(numberOfStopPointsInJourneyPattern);

    netexEntitiesTestFactory.addServiceJourney(journeyPatternBuilder);

    IntStream
      .range(0, numberOfStopPointsInJourneyPattern)
      .forEach(index ->
        withQuayId(
          new ScheduledStopPointId("TST:ScheduledStopPoint:" + (index + 1)),
          new QuayId("TST:Quay:" + (index + 1))
        )
      );

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.build()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void testMissingStopPlaceAssignmentsButServiceJourneyExists() {
    NetexTestData netexEntitiesTestFactory = new NetexTestData();

    int numberOfStopPointsInJourneyPattern = 4;

    JourneyPatternBuilder journeyPatternBuilder =
      netexEntitiesTestFactory.addJourneyPattern();

    journeyPatternBuilder.addStopPoints(numberOfStopPointsInJourneyPattern);

    netexEntitiesTestFactory.addServiceJourney(journeyPatternBuilder);

    IntStream
      .range(0, numberOfStopPointsInJourneyPattern - 1)
      .forEach(index ->
        withQuayId(
          new ScheduledStopPointId("TST:ScheduledStopPoint:" + (index + 1)),
          new QuayId("TST:Quay:" + (index + 1))
        )
      );

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.build()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
  }

  @Test
  /*
   * Missing SPA -> No DeadRun -> Yes SJ -> Error
   */
  void testMissingSingleStopPlaceAssignmentsUsedInMultipleJourneyPatternsButServiceJourneyExists() {
    NetexTestData netexEntitiesTestFactory = new NetexTestData();

    IntStream
      .rangeClosed(1, 4)
      .mapToObj(netexEntitiesTestFactory::addJourneyPattern)
      .forEach(journeyPatternBuilder -> {
        journeyPatternBuilder.addStopPoints(4);
        netexEntitiesTestFactory.addServiceJourney(journeyPatternBuilder);
      });

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.build()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(16));
  }

  @Test
  /*
   * Missing SPA -> No DeadRun -> No SJ -> Error
   */
  void testMissingStopPlaceAssignmentsAndNoServiceJourneyExists() {
    NetexTestData netexEntitiesTestFactory = new NetexTestData();
    JourneyPatternBuilder journeyPatternBuilder =
      netexEntitiesTestFactory.addJourneyPattern();
    journeyPatternBuilder.addStopPoints(4);

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.build()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(4));
  }

  @Test
  /*
   * Missing SPA -> Yes DeadRun -> No SJ -> OK
   */
  void testMissingStopPlaceAssignmentsAndDeadRunExists() {
    NetexTestData netexEntitiesTestFactory = new NetexTestData();

    netexEntitiesTestFactory.addDeadRun(
      netexEntitiesTestFactory.addJourneyPattern()
    );

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.build()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  /*
   * Missing SPA -> Yes DeadRun -> Yes SJ -> Error
   */
  void testMissingStopPlaceAssignmentsAndBothDeadRunAndServiceJourneyExists() {
    NetexTestData netexEntitiesTestFactory = new NetexTestData();

    JourneyPatternBuilder journeyPatternBuilder =
      netexEntitiesTestFactory.addJourneyPattern();
    journeyPatternBuilder.addStopPoints(4);

    netexEntitiesTestFactory.addDeadRun(journeyPatternBuilder).build();
    netexEntitiesTestFactory.addServiceJourney(journeyPatternBuilder).build();

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.build()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(4));
  }

  @Test
  void testMissingMultipleStopPlaceAssignmentsButServiceJourneyExists() {
    NetexTestData netexEntitiesTestFactory = new NetexTestData();

    int numberOfStopPointsInJourneyPattern = 4;

    JourneyPatternBuilder journeyPatternBuilder =
      netexEntitiesTestFactory.addJourneyPattern();

    journeyPatternBuilder.addStopPoints(numberOfStopPointsInJourneyPattern);

    netexEntitiesTestFactory.addServiceJourney(journeyPatternBuilder);

    IntStream
      .range(0, numberOfStopPointsInJourneyPattern - 2)
      .forEach(index ->
        withQuayId(
          new ScheduledStopPointId("TST:ScheduledStopPoint:" + (index + 1)),
          new QuayId("TST:Quay:" + (index + 1))
        )
      );

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.build()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(2));
  }

  @Test
  void testPassengerStopAssignmentsInLineFileAndNotOnCommonFileShouldBeOk() {
    NetexTestData netexEntitiesTestFactory = new NetexTestData();

    int numberOfStopPointsInJourneyPattern = 4;

    JourneyPatternBuilder journeyPatternBuilder =
      netexEntitiesTestFactory.addJourneyPattern();
    journeyPatternBuilder.addStopPoints(numberOfStopPointsInJourneyPattern);

    netexEntitiesTestFactory.addServiceJourney(journeyPatternBuilder);

    IntStream
      .range(0, numberOfStopPointsInJourneyPattern)
      .forEach(index ->
        netexEntitiesTestFactory
          .addPassengerStopAssignment()
          .withScheduledStopPointRef(NetexRefs.scheduledStopPointRef(index + 1))
          .withStopPlaceRef(NetexRefs.stopPointRef(index + 1))
          .withQuayRef(NetexRefs.quayRef(index + 1))
      );

    withNoSharedScheduledStopPoints();

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.build()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }
}
