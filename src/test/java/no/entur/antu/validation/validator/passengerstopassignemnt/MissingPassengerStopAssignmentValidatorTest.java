package no.entur.antu.validation.validator.passengerstopassignemnt;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.List;
import java.util.stream.IntStream;
import no.entur.antu.netextestdata.NetexTestFragment;
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
import org.rutebanken.netex.model.Journey_VersionStructure;
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
    NetexTestFragment testData = new NetexTestFragment();
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
    NetexTestFragment testData = new NetexTestFragment();
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
    NetexTestFragment testData = new NetexTestFragment();
    List<JourneyPattern> journeyPatterns = testData.createJourneyPatterns(4);
    Line line = testData.line().create();
    List<Journey_VersionStructure> serviceJourneys = journeyPatterns
      .stream()
      .map(journeyPattern -> testData.serviceJourney(line, journeyPattern))
      .map(NetexTestFragment.CreateServiceJourney::create)
      .map(Journey_VersionStructure.class::cast)
      .toList();

    NetexEntitiesIndex netexEntitiesIndex = testData
      .netexEntitiesIndex()
      .addJourneyPatterns(journeyPatterns.toArray(JourneyPattern[]::new))
      .addJourneys(serviceJourneys.toArray(Journey_VersionStructure[]::new))
      .create();

    ValidationReport validationReport = runValidation(netexEntitiesIndex);

    assertThat(validationReport.getValidationReportEntries().size(), is(16));
  }

  @Test
  /*
   * Missing SPA -> No DeadRun -> No SJ -> Error
   */
  void testMissingStopPlaceAssignmentsAndNoServiceJourneyExists() {
    NetexTestFragment testData = new NetexTestFragment();
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
    NetexTestFragment testData = new NetexTestFragment();
    JourneyPattern journeyPattern = testData.journeyPattern().create();
    DeadRun deadRun = testData.deadRun(journeyPattern).create();

    NetexEntitiesIndex netexEntitiesIndex = testData
      .netexEntitiesIndex(journeyPattern, deadRun)
      .create();

    ValidationReport validationReport = runValidation(netexEntitiesIndex);

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  /*
   * Missing SPA -> Yes DeadRun -> Yes SJ -> Error
   */
  void testMissingStopPlaceAssignmentsAndBothDeadRunAndServiceJourneyExists() {
    NetexTestFragment testData = new NetexTestFragment();
    JourneyPattern journeyPattern = testData.journeyPattern().create();
    Line line = testData.line().create();
    DeadRun deadRun = testData.deadRun(line, journeyPattern).create();
    ServiceJourney serviceJourney = testData
      .serviceJourney(line, journeyPattern)
      .create();

    NetexEntitiesIndex netexEntitiesIndex = testData
      .netexEntitiesIndex()
      .addJourneyPatterns(journeyPattern)
      .addJourneys(deadRun, serviceJourney)
      .create();

    ValidationReport validationReport = runValidation(netexEntitiesIndex);

    assertThat(validationReport.getValidationReportEntries().size(), is(4));
  }

  @Test
  void testMissingMultipleStopPlaceAssignmentsButServiceJourneyExists() {
    NetexTestFragment testData = new NetexTestFragment();
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
    NetexTestFragment testData = new NetexTestFragment();
    JourneyPattern journeyPattern = testData.journeyPattern().create();
    ServiceJourney serviceJourney = testData
      .serviceJourney(journeyPattern)
      .create();

    NetexTestFragment.CreateNetexEntitiesIndex createNetexEntitiesIndex =
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
