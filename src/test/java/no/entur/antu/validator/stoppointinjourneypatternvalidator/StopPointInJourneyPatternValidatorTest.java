package no.entur.antu.validator.stoppointinjourneypatternvalidator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.IntStream;
import no.entur.antu.commondata.CommonDataRepository;
import no.entur.antu.model.QuayId;
import no.entur.antu.netextestdata.NetexTestData;
import no.entur.antu.validator.ValidationContextWithNetexEntitiesIndex;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.index.impl.NetexEntitiesIndexImpl;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.entur.netex.validation.validator.ValidationReportEntrySeverity;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.rutebanken.netex.model.*;

class StopPointInJourneyPatternValidatorTest {

  @Test
  void testAllStopPlaceAssignmentsExists() {
    NetexTestData testData = new NetexTestData();
    JourneyPattern journeyPattern = testData.journeyPattern().create();
    ServiceJourney serviceJourney = testData
      .serviceJourney(journeyPattern)
      .create();

    NetexEntitiesIndex netexEntitiesIndex = createNetexEntitiesIndex(
      journeyPattern,
      serviceJourney
    );
    CommonDataRepository commonDataRepository = Mockito.mock(
      CommonDataRepository.class
    );

    IntStream
      .range(
        0,
        journeyPattern
          .getPointsInSequence()
          .getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern()
          .size()
      )
      .forEach(index -> {
        QuayId testQuayId = new QuayId("TST:Quay:" + (index + 1));
        Mockito
          .when(
            commonDataRepository.findQuayIdForScheduledStopPoint(
              eq("RUT:ScheduledStopPoint:" + (index + 1)),
              anyString()
            )
          )
          .thenReturn(testQuayId);
      });

    ValidationReport validationReport = setupAndRunValidation(
      netexEntitiesIndex,
      commonDataRepository
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void testMissingStopPlaceAssignmentsButServiceJourneyExists() {
    NetexTestData testData = new NetexTestData();
    JourneyPattern journeyPattern = testData.journeyPattern().create();
    ServiceJourney serviceJourney = testData
      .serviceJourney(journeyPattern)
      .create();

    NetexEntitiesIndex netexEntitiesIndex = createNetexEntitiesIndex(
      journeyPattern,
      serviceJourney
    );
    CommonDataRepository commonDataRepository = Mockito.mock(
      CommonDataRepository.class
    );

    IntStream
      .range(
        0,
        journeyPattern
          .getPointsInSequence()
          .getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern()
          .size() -
        1
      )
      .forEach(index -> {
        QuayId testQuayId = new QuayId("TST:Quay:" + (index + 1));
        Mockito
          .when(
            commonDataRepository.findQuayIdForScheduledStopPoint(
              eq("RUT:ScheduledStopPoint:" + (index + 1)),
              anyString()
            )
          )
          .thenReturn(testQuayId);
      });

    ValidationReport validationReport = setupAndRunValidation(
      netexEntitiesIndex,
      commonDataRepository
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
  }

  @Test
  /*
   * Missing SPA -> No DeadRun -> Yes SJ -> Error
   */
  void testMissingSingleStopPlaceAssignmentsUsedInMultipleJourneyPatternsButServiceJourneyExists() {
    NetexTestData testData = new NetexTestData();
    List<JourneyPattern> journeyPatterns = testData.createJourneyPatterns(4);
    List<Journey_VersionStructure> serviceJourneys = journeyPatterns
      .stream()
      .map(testData::serviceJourney)
      .map(NetexTestData.CreateServiceJourney::create)
      .map(Journey_VersionStructure.class::cast)
      .toList();
    NetexEntitiesIndex netexEntitiesIndex = createNetexEntitiesIndex(
      journeyPatterns,
      serviceJourneys
    );
    CommonDataRepository commonDataRepository = Mockito.mock(
      CommonDataRepository.class
    );

    ValidationReport validationReport = setupAndRunValidation(
      netexEntitiesIndex,
      commonDataRepository
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(16));
  }

  @Test
  /*
   * Missing SPA -> No DeadRun -> No SJ -> Error
   */
  void testMissingStopPlaceAssignmentsAndNoServiceJourneyExists() {
    NetexTestData testData = new NetexTestData();
    JourneyPattern journeyPattern = testData.journeyPattern().create();

    NetexEntitiesIndex netexEntitiesIndex = new NetexEntitiesIndexImpl();
    netexEntitiesIndex
      .getJourneyPatternIndex()
      .put(journeyPattern.getId(), journeyPattern);

    CommonDataRepository commonDataRepository = Mockito.mock(
      CommonDataRepository.class
    );

    ValidationReport validationReport = setupAndRunValidation(
      netexEntitiesIndex,
      commonDataRepository
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(4));
  }

  @Test
  /*
   * Missing SPA -> Yes DeadRun -> No SJ -> OK
   */
  void testMissingStopPlaceAssignmentsAndDeadRunExists() {
    NetexTestData testData = new NetexTestData();
    JourneyPattern journeyPattern = testData.journeyPattern().create();
    DeadRun deadRun = testData.deadRun(journeyPattern).create();

    NetexEntitiesIndex netexEntitiesIndex = createNetexEntitiesIndex(
      journeyPattern,
      deadRun
    );
    CommonDataRepository commonDataRepository = Mockito.mock(
      CommonDataRepository.class
    );

    ValidationReport validationReport = setupAndRunValidation(
      netexEntitiesIndex,
      commonDataRepository
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  /*
   * Missing SPA -> Yes DeadRun -> Yes SJ -> Error
   */
  void testMissingStopPlaceAssignmentsAndBothDeadRunAndServiceJourneyExists() {
    NetexTestData testData = new NetexTestData();
    JourneyPattern journeyPattern = testData.journeyPattern().create();
    DeadRun deadRun = testData.deadRun(journeyPattern).create();
    ServiceJourney serviceJourney = testData
      .serviceJourney(journeyPattern)
      .create();

    NetexEntitiesIndex netexEntitiesIndex = createNetexEntitiesIndex(
      journeyPattern,
      List.of(deadRun, serviceJourney)
    );
    CommonDataRepository commonDataRepository = Mockito.mock(
      CommonDataRepository.class
    );

    ValidationReport validationReport = setupAndRunValidation(
      netexEntitiesIndex,
      commonDataRepository
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(4));
  }

  @Test
  void testMissingMultipleStopPlaceAssignmentsButServiceJourneyExists() {
    NetexTestData testData = new NetexTestData();
    JourneyPattern journeyPattern = testData.journeyPattern().create();
    ServiceJourney serviceJourney = testData
      .serviceJourney(journeyPattern)
      .create();

    NetexEntitiesIndex netexEntitiesIndex = createNetexEntitiesIndex(
      journeyPattern,
      serviceJourney
    );
    CommonDataRepository commonDataRepository = Mockito.mock(
      CommonDataRepository.class
    );

    IntStream
      .range(
        0,
        journeyPattern
          .getPointsInSequence()
          .getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern()
          .size() -
        2
      )
      .forEach(index -> {
        QuayId testQuayId = new QuayId("TST:Quay:" + (index + 1));
        Mockito
          .when(
            commonDataRepository.findQuayIdForScheduledStopPoint(
              eq("RUT:ScheduledStopPoint:" + (index + 1)),
              anyString()
            )
          )
          .thenReturn(testQuayId);
      });

    ValidationReport validationReport = setupAndRunValidation(
      netexEntitiesIndex,
      commonDataRepository
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(2));
  }

  private static ValidationReport setupAndRunValidation(
    NetexEntitiesIndex netexEntitiesIndex,
    CommonDataRepository commonDataRepository
  ) {
    StopPointInJourneyPatternValidator nonIncreasingPassingTimeValidator =
      new StopPointInJourneyPatternValidator(
        (code, message, dataLocation) ->
          new ValidationReportEntry(
            message,
            code,
            ValidationReportEntrySeverity.ERROR
          ),
        commonDataRepository
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

    nonIncreasingPassingTimeValidator.validate(
      testValidationReport,
      validationContext
    );

    return testValidationReport;
  }

  private static NetexEntitiesIndex createNetexEntitiesIndex(
    JourneyPattern journeyPattern,
    Journey_VersionStructure journey
  ) {
    NetexEntitiesIndex netexEntitiesIndex = new NetexEntitiesIndexImpl();
    netexEntitiesIndex
      .getJourneyPatternIndex()
      .put(journeyPattern.getId(), journeyPattern);

    netexEntitiesIndex
      .getTimetableFrames()
      .add(
        new TimetableFrame()
          .withVehicleJourneys(
            new JourneysInFrame_RelStructure()
              .withId("JR:123")
              .withVehicleJourneyOrDatedVehicleJourneyOrNormalDatedVehicleJourney(
                journey
              )
          )
      );

    return netexEntitiesIndex;
  }

  private static NetexEntitiesIndex createNetexEntitiesIndex(
    JourneyPattern journeyPattern,
    List<Journey_VersionStructure> journeys
  ) {
    NetexEntitiesIndex netexEntitiesIndex = new NetexEntitiesIndexImpl();
    netexEntitiesIndex
      .getJourneyPatternIndex()
      .put(journeyPattern.getId(), journeyPattern);

    journeys.forEach(journey ->
      netexEntitiesIndex
        .getTimetableFrames()
        .add(
          new TimetableFrame()
            .withVehicleJourneys(
              new JourneysInFrame_RelStructure()
                .withId("JR:123")
                .withVehicleJourneyOrDatedVehicleJourneyOrNormalDatedVehicleJourney(
                  journey
                )
            )
        )
    );

    return netexEntitiesIndex;
  }

  private static NetexEntitiesIndex createNetexEntitiesIndex(
    List<JourneyPattern> journeyPatterns,
    List<Journey_VersionStructure> journeys
  ) {
    NetexEntitiesIndex netexEntitiesIndex = new NetexEntitiesIndexImpl();
    journeyPatterns.forEach(journeyPattern ->
      netexEntitiesIndex
        .getJourneyPatternIndex()
        .put(journeyPattern.getId(), journeyPattern)
    );

    journeys.forEach(journey ->
      netexEntitiesIndex
        .getTimetableFrames()
        .add(
          new TimetableFrame()
            .withVehicleJourneys(
              new JourneysInFrame_RelStructure()
                .withId("JR:123")
                .withVehicleJourneyOrDatedVehicleJourneyOrNormalDatedVehicleJourney(
                  journey
                )
            )
        )
    );
    return netexEntitiesIndex;
  }
}
