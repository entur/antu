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
import no.entur.antu.model.StopPlaceCoordinates;
import no.entur.antu.netextestdata.NetexTestData;
import no.entur.antu.stop.StopPlaceRepository;
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

    NetexEntitiesIndex netexEntitiesIndex = testData
      .netexEntitiesIndex(journeyPattern, serviceJourney)
      .create();

    CommonDataRepository commonDataRepository = mock(
      CommonDataRepository.class
    );
    when(commonDataRepository.hasQuayIds(anyString())).thenReturn(true);

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
        when(
          commonDataRepository.findQuayIdForScheduledStopPoint(
            eq("TST:ScheduledStopPoint:" + (index + 1)),
            anyString()
          )
        )
          .thenReturn(testQuayId);
      });

    ValidationReport validationReport = setupAndRunValidation(
      netexEntitiesIndex,
      commonDataRepository
    );
    when(commonDataRepository.hasQuayIds(anyString())).thenReturn(true);

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void testMissingStopPlaceAssignmentsButServiceJourneyExists() {
    NetexTestData testData = new NetexTestData();
    JourneyPattern journeyPattern = testData.journeyPattern().create();
    ServiceJourney serviceJourney = testData
      .serviceJourney(journeyPattern)
      .create();

    NetexEntitiesIndex netexEntitiesIndex = testData
      .netexEntitiesIndex(journeyPattern, serviceJourney)
      .create();

    CommonDataRepository commonDataRepository = mock(
      CommonDataRepository.class
    );

    when(commonDataRepository.hasQuayIds(anyString())).thenReturn(true);

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
        when(
          commonDataRepository.findQuayIdForScheduledStopPoint(
            eq("TST:ScheduledStopPoint:" + (index + 1)),
            anyString()
          )
        )
          .thenReturn(testQuayId);
      });

    ValidationReport validationReport = setupAndRunValidation(
      netexEntitiesIndex,
      commonDataRepository
    );
    when(commonDataRepository.hasQuayIds(anyString())).thenReturn(true);

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

    NetexEntitiesIndex netexEntitiesIndex = testData
      .netexEntitiesIndex()
      .addJourneyPatterns(journeyPatterns.toArray(JourneyPattern[]::new))
      .addServiceJourneys(
        serviceJourneys.toArray(Journey_VersionStructure[]::new)
      )
      .create();

    CommonDataRepository commonDataRepository = mock(
      CommonDataRepository.class
    );
    when(commonDataRepository.hasQuayIds(anyString())).thenReturn(true);

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

    CommonDataRepository commonDataRepository = mock(
      CommonDataRepository.class
    );
    when(commonDataRepository.hasQuayIds(anyString())).thenReturn(true);

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

    NetexEntitiesIndex netexEntitiesIndex = testData
      .netexEntitiesIndex(journeyPattern, deadRun)
      .create();

    CommonDataRepository commonDataRepository = mock(
      CommonDataRepository.class
    );
    when(commonDataRepository.hasQuayIds(anyString())).thenReturn(true);

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

    NetexEntitiesIndex netexEntitiesIndex = testData
      .netexEntitiesIndex()
      .addJourneyPatterns(journeyPattern)
      .addServiceJourneys(deadRun, serviceJourney)
      .create();

    CommonDataRepository commonDataRepository = mock(
      CommonDataRepository.class
    );

    when(commonDataRepository.hasQuayIds(anyString())).thenReturn(true);

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

    NetexEntitiesIndex netexEntitiesIndex = testData
      .netexEntitiesIndex(journeyPattern, serviceJourney)
      .create();

    CommonDataRepository commonDataRepository = mock(
      CommonDataRepository.class
    );

    when(commonDataRepository.hasQuayIds(anyString())).thenReturn(true);

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
        when(
          commonDataRepository.findQuayIdForScheduledStopPoint(
            eq("TST:ScheduledStopPoint:" + (index + 1)),
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

  @Test
  void testPassengerStopAssignmentsInLineFileAndNotOnCommonFileShouldBeOk() {
    NetexTestData testData = new NetexTestData();
    JourneyPattern journeyPattern = testData.journeyPattern().create();
    ServiceJourney serviceJourney = testData
      .serviceJourney(journeyPattern)
      .create();

    NetexTestData.CreateNetexEntitiesIndex createNetexEntitiesIndex =
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

    CommonDataRepository commonDataRepository = mock(
      CommonDataRepository.class
    );
    when(commonDataRepository.hasQuayIds(anyString())).thenReturn(false);

    ValidationReport validationReport = setupAndRunValidation(
      createNetexEntitiesIndex.create(),
      commonDataRepository
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  private static ValidationReport setupAndRunValidation(
    NetexEntitiesIndex netexEntitiesIndex,
    CommonDataRepository commonDataRepository
  ) {
    StopPointInJourneyPatternValidator stopPointInJourneyPatternValidator =
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

    stopPointInJourneyPatternValidator.validate(
      testValidationReport,
      validationContext
    );

    return testValidationReport;
  }
}
