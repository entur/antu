package no.entur.antu.validation.validator.passengerstopassignemnt;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.List;
import java.util.stream.IntStream;
import no.entur.antu.netextestdata.NetexEntitiesTestFactory;
import no.entur.antu.validation.ValidationTest;
import no.entur.antu.validation.validator.passengerstopassignment.MissingPassengerStopAssignmentValidator;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.index.impl.NetexEntitiesIndexImpl;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.model.QuayId;
import org.entur.netex.validation.validator.model.ScheduledStopPointId;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.DeadRun;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.PassengerStopAssignment;
import org.rutebanken.netex.model.ServiceJourney;

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
    NetexEntitiesTestFactory testData = new NetexEntitiesTestFactory();
    JourneyPattern journeyPattern = testData.journeyPattern().create();
    ServiceJourney serviceJourney = testData
      .serviceJourney(journeyPattern)
      .create();

    NetexEntitiesIndex netexEntitiesIndex = testData
      .netexEntitiesIndex(journeyPattern, serviceJourney)
      .create();

    IntStream
      .range(
        0,
        journeyPattern
          .getPointsInSequence()
          .getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern()
          .size()
      )
      .forEach(index ->
        mockGetQuayId(
          new ScheduledStopPointId("TST:ScheduledStopPoint:" + (index + 1)),
          new QuayId("TST:Quay:" + (index + 1))
        )
      );

    ValidationReport validationReport = runValidation(netexEntitiesIndex);

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void testMissingStopPlaceAssignmentsButServiceJourneyExists() {
    NetexEntitiesTestFactory testData = new NetexEntitiesTestFactory();
    JourneyPattern journeyPattern = testData.journeyPattern().create();
    ServiceJourney serviceJourney = testData
      .serviceJourney(journeyPattern)
      .create();

    NetexEntitiesIndex netexEntitiesIndex = testData
      .netexEntitiesIndex(journeyPattern, serviceJourney)
      .create();

    IntStream
      .range(
        0,
        journeyPattern
          .getPointsInSequence()
          .getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern()
          .size() -
        1
      )
      .forEach(index ->
        mockGetQuayId(
          new ScheduledStopPointId("TST:ScheduledStopPoint:" + (index + 1)),
          new QuayId("TST:Quay:" + (index + 1))
        )
      );

    ValidationReport validationReport = runValidation(netexEntitiesIndex);

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
  }

  @Test
  /*
   * Missing SPA -> No DeadRun -> Yes SJ -> Error
   */
  void testMissingSingleStopPlaceAssignmentsUsedInMultipleJourneyPatternsButServiceJourneyExists() {
    NetexEntitiesTestFactory testData = new NetexEntitiesTestFactory();
    List<JourneyPattern> journeyPatterns = testData.createJourneyPatterns(4);
    Line line = testData.line().create();
    List<ServiceJourney> serviceJourneys = journeyPatterns
      .stream()
      .map(journeyPattern -> testData.serviceJourney(line, journeyPattern))
      .map(NetexEntitiesTestFactory.CreateServiceJourney::create)
      .map(ServiceJourney.class::cast)
      .toList();

    NetexEntitiesIndex netexEntitiesIndex = testData
      .netexEntitiesIndex()
      .addJourneyPatterns(journeyPatterns.toArray(JourneyPattern[]::new))
      .addServiceJourneys(serviceJourneys.toArray(ServiceJourney[]::new))
      .create();

    ValidationReport validationReport = runValidation(netexEntitiesIndex);

    assertThat(validationReport.getValidationReportEntries().size(), is(16));
  }

  @Test
  /*
   * Missing SPA -> No DeadRun -> No SJ -> Error
   */
  void testMissingStopPlaceAssignmentsAndNoServiceJourneyExists() {
    NetexEntitiesTestFactory testData = new NetexEntitiesTestFactory();
    JourneyPattern journeyPattern = testData.journeyPattern().create();

    NetexEntitiesIndex netexEntitiesIndex = new NetexEntitiesIndexImpl();
    netexEntitiesIndex
      .getJourneyPatternIndex()
      .put(journeyPattern.getId(), journeyPattern);

    ValidationReport validationReport = runValidation(netexEntitiesIndex);

    assertThat(validationReport.getValidationReportEntries().size(), is(4));
  }

  @Test
  /*
   * Missing SPA -> Yes DeadRun -> No SJ -> OK
   */
  void testMissingStopPlaceAssignmentsAndDeadRunExists() {
    NetexEntitiesTestFactory testData = new NetexEntitiesTestFactory();
    JourneyPattern journeyPattern = testData.journeyPattern().create();
    DeadRun deadRun = testData.deadRun(journeyPattern).create();

    NetexEntitiesIndex netexEntitiesIndex = testData
      .netexEntitiesIndex()
      .addJourneyPatterns(journeyPattern)
      .addDeadRuns(deadRun)
      .create();

    ValidationReport validationReport = runValidation(netexEntitiesIndex);

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  /*
   * Missing SPA -> Yes DeadRun -> Yes SJ -> Error
   */
  void testMissingStopPlaceAssignmentsAndBothDeadRunAndServiceJourneyExists() {
    NetexEntitiesTestFactory testData = new NetexEntitiesTestFactory();
    JourneyPattern journeyPattern = testData.journeyPattern().create();
    Line line = testData.line().create();
    DeadRun deadRun = testData.deadRun(line, journeyPattern).create();
    ServiceJourney serviceJourney = testData
      .serviceJourney(line, journeyPattern)
      .create();

    NetexEntitiesIndex netexEntitiesIndex = testData
      .netexEntitiesIndex()
      .addJourneyPatterns(journeyPattern)
      .addServiceJourneys(serviceJourney)
      .addDeadRuns(deadRun)
      .create();

    ValidationReport validationReport = runValidation(netexEntitiesIndex);

    assertThat(validationReport.getValidationReportEntries().size(), is(4));
  }

  @Test
  void testMissingMultipleStopPlaceAssignmentsButServiceJourneyExists() {
    NetexEntitiesTestFactory testData = new NetexEntitiesTestFactory();
    JourneyPattern journeyPattern = testData.journeyPattern().create();
    ServiceJourney serviceJourney = testData
      .serviceJourney(journeyPattern)
      .create();

    NetexEntitiesIndex netexEntitiesIndex = testData
      .netexEntitiesIndex(journeyPattern, serviceJourney)
      .create();

    IntStream
      .range(
        0,
        journeyPattern
          .getPointsInSequence()
          .getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern()
          .size() -
        2
      )
      .forEach(index ->
        mockGetQuayId(
          new ScheduledStopPointId("TST:ScheduledStopPoint:" + (index + 1)),
          new QuayId("TST:Quay:" + (index + 1))
        )
      );

    ValidationReport validationReport = runValidation(netexEntitiesIndex);

    assertThat(validationReport.getValidationReportEntries().size(), is(2));
  }

  @Test
  void testPassengerStopAssignmentsInLineFileAndNotOnCommonFileShouldBeOk() {
    NetexEntitiesTestFactory testData = new NetexEntitiesTestFactory();
    JourneyPattern journeyPattern = testData.journeyPattern().create();
    ServiceJourney serviceJourney = testData
      .serviceJourney(journeyPattern)
      .create();

    NetexEntitiesTestFactory.CreateNetexEntitiesIndex createNetexEntitiesIndex =
      testData.netexEntitiesIndex(journeyPattern, serviceJourney);

    IntStream
      .range(
        0,
        journeyPattern
          .getPointsInSequence()
          .getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern()
          .size()
      )
      .forEach(index -> {
        PassengerStopAssignment passengerStopAssignment = testData
          .passengerStopAssignment()
          .withScheduleStopPointId(index + 1)
          .withStopPlaceId(index + 1)
          .withQuayId(index + 1)
          .create();

        createNetexEntitiesIndex.addPassengerStopAssignment(
          passengerStopAssignment
        );
      });

    mockNoQuayIdsInNetexDataRepository();

    ValidationReport validationReport = runValidation(
      createNetexEntitiesIndex.create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }
}
