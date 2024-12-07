package no.entur.antu.validation.validator.passengerstopassignemnt;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.stream.IntStream;
import no.entur.antu.netextestdata.NetexEntitiesTestFactory;
import no.entur.antu.validation.ValidationTest;
import no.entur.antu.validation.validator.passengerstopassignment.MissingPassengerStopAssignmentValidator;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.model.QuayId;
import org.entur.netex.validation.validator.model.ScheduledStopPointId;
import org.junit.jupiter.api.Test;

class MissingPassengerStopAssignmentValidatorTest extends ValidationTest {

  private ValidationReport runValidation(
    NetexEntitiesIndex netexEntitiesIndex
  ) {
    return runValidationOnLineFile(
      netexEntitiesIndex,
      MissingPassengerStopAssignmentValidator.class
    );
  }

  @Test
  void testAllStopPlaceAssignmentsExists() {
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

    int numberOfStopPointsInJourneyPattern = 4;

    NetexEntitiesTestFactory.CreateJourneyPattern createJourneyPattern =
      netexEntitiesTestFactory.createJourneyPattern();
    createJourneyPattern.createStopPointsInJourneyPattern(
      numberOfStopPointsInJourneyPattern
    );

    netexEntitiesTestFactory.createServiceJourney(createJourneyPattern);

    IntStream
      .range(0, numberOfStopPointsInJourneyPattern)
      .forEach(index ->
        mockGetQuayId(
          new ScheduledStopPointId("TST:ScheduledStopPoint:" + (index + 1)),
          new QuayId("TST:Quay:" + (index + 1))
        )
      );

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void testMissingStopPlaceAssignmentsButServiceJourneyExists() {
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

    int numberOfStopPointsInJourneyPattern = 4;

    NetexEntitiesTestFactory.CreateJourneyPattern createJourneyPattern =
      netexEntitiesTestFactory.createJourneyPattern();

    createJourneyPattern.createStopPointsInJourneyPattern(
      numberOfStopPointsInJourneyPattern
    );

    netexEntitiesTestFactory.createServiceJourney(createJourneyPattern);

    IntStream
      .range(0, numberOfStopPointsInJourneyPattern - 1)
      .forEach(index ->
        mockGetQuayId(
          new ScheduledStopPointId("TST:ScheduledStopPoint:" + (index + 1)),
          new QuayId("TST:Quay:" + (index + 1))
        )
      );

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
  }

  @Test
  /*
   * Missing SPA -> No DeadRun -> Yes SJ -> Error
   */
  void testMissingSingleStopPlaceAssignmentsUsedInMultipleJourneyPatternsButServiceJourneyExists() {
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

    IntStream
      .rangeClosed(1, 4)
      .mapToObj(netexEntitiesTestFactory::createJourneyPattern)
      .forEach(createJourneyPattern -> {
        createJourneyPattern.createStopPointsInJourneyPattern(4);
        netexEntitiesTestFactory.createServiceJourney(createJourneyPattern);
      });

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(16));
  }

  @Test
  /*
   * Missing SPA -> No DeadRun -> No SJ -> Error
   */
  void testMissingStopPlaceAssignmentsAndNoServiceJourneyExists() {
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();
    NetexEntitiesTestFactory.CreateJourneyPattern createJourneyPattern =
      netexEntitiesTestFactory.createJourneyPattern();
    createJourneyPattern.createStopPointsInJourneyPattern(4);

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(4));
  }

  @Test
  /*
   * Missing SPA -> Yes DeadRun -> No SJ -> OK
   */
  void testMissingStopPlaceAssignmentsAndDeadRunExists() {
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

    netexEntitiesTestFactory.createDeadRun(
      netexEntitiesTestFactory.createJourneyPattern()
    );

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  /*
   * Missing SPA -> Yes DeadRun -> Yes SJ -> Error
   */
  void testMissingStopPlaceAssignmentsAndBothDeadRunAndServiceJourneyExists() {
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

    NetexEntitiesTestFactory.CreateJourneyPattern createJourneyPattern =
      netexEntitiesTestFactory.createJourneyPattern();
    createJourneyPattern.createStopPointsInJourneyPattern(4);

    netexEntitiesTestFactory.createDeadRun(createJourneyPattern).create();
    netexEntitiesTestFactory
      .createServiceJourney(createJourneyPattern)
      .create();

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(4));
  }

  @Test
  void testMissingMultipleStopPlaceAssignmentsButServiceJourneyExists() {
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

    int numberOfStopPointsInJourneyPattern = 4;

    NetexEntitiesTestFactory.CreateJourneyPattern createJourneyPattern =
      netexEntitiesTestFactory.createJourneyPattern();

    createJourneyPattern.createStopPointsInJourneyPattern(
      numberOfStopPointsInJourneyPattern
    );

    netexEntitiesTestFactory.createServiceJourney(createJourneyPattern);

    IntStream
      .range(0, numberOfStopPointsInJourneyPattern - 2)
      .forEach(index ->
        mockGetQuayId(
          new ScheduledStopPointId("TST:ScheduledStopPoint:" + (index + 1)),
          new QuayId("TST:Quay:" + (index + 1))
        )
      );

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(2));
  }

  @Test
  void testPassengerStopAssignmentsInLineFileAndNotOnCommonFileShouldBeOk() {
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

    int numberOfStopPointsInJourneyPattern = 4;

    NetexEntitiesTestFactory.CreateJourneyPattern createJourneyPattern =
      netexEntitiesTestFactory.createJourneyPattern();
    createJourneyPattern.createStopPointsInJourneyPattern(
      numberOfStopPointsInJourneyPattern
    );

    netexEntitiesTestFactory.createServiceJourney(createJourneyPattern);

    IntStream
      .range(0, numberOfStopPointsInJourneyPattern)
      .forEach(index ->
        netexEntitiesTestFactory
          .createPassengerStopAssignment()
          .withScheduledStopPointRef(
            NetexEntitiesTestFactory.createScheduledStopPointRef(index + 1)
          )
          .withStopPlaceRef(
            NetexEntitiesTestFactory.createStopPointRef(index + 1)
          )
          .withQuayRef(NetexEntitiesTestFactory.createQuayRef(index + 1))
      );

    mockNoQuayIdsInNetexDataRepository();

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }
}
