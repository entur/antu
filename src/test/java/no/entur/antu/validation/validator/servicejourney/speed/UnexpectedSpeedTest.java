package no.entur.antu.validation.validator.servicejourney.speed;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import no.entur.antu.commondata.CommonDataRepository;
import no.entur.antu.model.QuayCoordinates;
import no.entur.antu.model.QuayId;
import no.entur.antu.model.ScheduledStopPointId;
import no.entur.antu.netextestdata.NetexTestFragment;
import no.entur.antu.stop.StopPlaceRepository;
import no.entur.antu.validation.ValidationContextWithNetexEntitiesIndex;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.entur.netex.validation.validator.ValidationReportEntrySeverity;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.PassengerStopAssignment;
import org.rutebanken.netex.model.ServiceJourney;

class UnexpectedSpeedTest {

  @Test
  void normalSpeedShouldNotReturnAnyValidationEntry() {
    ValidationReport validationReport = runTestWithQuayCoordinates(
      List.of(
        new QuayCoordinates(6.621791, 60.424023),
        new QuayCoordinates(6.612112, 60.471748),
        new QuayCoordinates(6.622312, 60.481548),
        new QuayCoordinates(6.632312, 60.491548)
      )
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void lowSpeedShouldReturnValidationEntryForLowSpeed() {
    ValidationReport validationReport = runTestWithQuayCoordinates(
      List.of(
        new QuayCoordinates(6.621791, 60.424023),
        new QuayCoordinates(6.612112, 60.471748),
        new QuayCoordinates(6.612312, 60.471548),
        new QuayCoordinates(6.632312, 60.491548)
      )
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
    assertThat(
      validationReport
        .getValidationReportEntries()
        .stream()
        .map(ValidationReportEntry::getName)
        .findFirst()
        .orElse(null),
      is(UnexpectedSpeedError.RuleCode.LOW_SPEED.name())
    );
  }

  @Test
  void highSpeedShouldReturnValidationEntryForHighSpeed() {
    ValidationReport validationReport = runTestWithQuayCoordinates(
      List.of(
        new QuayCoordinates(6.621791, 60.424023),
        new QuayCoordinates(6.612112, 60.471748),
        new QuayCoordinates(6.602312, 60.471548),
        new QuayCoordinates(6.592312, 61.491548)
      )
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
    assertThat(
      validationReport
        .getValidationReportEntries()
        .stream()
        .map(ValidationReportEntry::getName)
        .findFirst()
        .orElse(null),
      is(UnexpectedSpeedError.RuleCode.HIGH_SPEED.name())
    );
  }

  @Test
  void warningSpeedShouldReturnValidationEntryForHighSpeed() {
    ValidationReport validationReport = runTestWithQuayCoordinates(
      List.of(
        new QuayCoordinates(6.621791, 60.424023),
        new QuayCoordinates(6.612112, 60.471748),
        new QuayCoordinates(6.602312, 60.471548),
        new QuayCoordinates(6.592312, 60.551548)
      )
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
    assertThat(
      validationReport
        .getValidationReportEntries()
        .stream()
        .map(ValidationReportEntry::getName)
        .findFirst()
        .orElse(null),
      is(UnexpectedSpeedError.RuleCode.WARNING_SPEED.name())
    );
  }

  @Test
  void multipleSpeedViolationShouldBeDetected() {
    ValidationReport validationReport = runTestWithQuayCoordinates(
      List.of(
        new QuayCoordinates(6.621791, 60.424023),
        new QuayCoordinates(6.612112, 60.471748),
        new QuayCoordinates(6.612312, 60.471548),
        new QuayCoordinates(6.592312, 61.491548)
      )
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(2));
    assertThat(
      validationReport
        .getValidationReportEntries()
        .stream()
        .map(ValidationReportEntry::getName)
        .toList(),
      is(
        List.of(
          UnexpectedSpeedError.RuleCode.LOW_SPEED.name(),
          UnexpectedSpeedError.RuleCode.HIGH_SPEED.name()
        )
      )
    );
  }

  @Test
  void testSameDepartureArrivalTimeErrorThrown() {
    NetexTestFragment testData = new NetexTestFragment();
    JourneyPattern journeyPattern = testData
      .journeyPattern()
      .withNumberOfStopPointInJourneyPattern(2)
      .create();

    // Setting departureTimeOffset to 0 to will make the departure time same for both passingTimes.
    ServiceJourney serviceJourney = testData
      .serviceJourney(journeyPattern)
      .withCreateTimetabledPassingTimes(
        testData.timetabledPassingTimes().withDepartureTimeOffset(0)
      )
      .create();

    serviceJourney.withTransportMode(AllVehicleModesOfTransportEnumeration.BUS);

    ValidationReport validationReport = runTestWith(
      List.of(
        new QuayCoordinates(6.622312, 60.481548),
        new QuayCoordinates(6.632312, 60.491548)
      ),
      testData.netexEntitiesIndex(journeyPattern, serviceJourney).create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
    assertThat(
      validationReport
        .getValidationReportEntries()
        .stream()
        .map(ValidationReportEntry::getName)
        .toList(),
      is(
        List.of(
          SameDepartureArrivalTimeError.RuleCode.SAME_DEPARTURE_ARRIVAL_TIME.name()
        )
      )
    );
  }

  @Test
  void testPassengerStopAssignmentsInLineFileAndNotOnCommonFileShouldBeOk() {
    List<QuayCoordinates> quayCoordinates = List.of(
      new QuayCoordinates(6.621791, 60.424023),
      new QuayCoordinates(6.612112, 60.471748),
      new QuayCoordinates(6.622312, 60.481548),
      new QuayCoordinates(6.632312, 60.491548)
    );

    NetexTestFragment testData = new NetexTestFragment();
    JourneyPattern journeyPattern = testData.journeyPattern().create();
    ServiceJourney serviceJourney = testData
      .serviceJourney(journeyPattern)
      .create();
    serviceJourney.withTransportMode(AllVehicleModesOfTransportEnumeration.BUS);

    NetexTestFragment.CreateNetexEntitiesIndex createNetexEntitiesIndex =
      testData.netexEntitiesIndex(journeyPattern, serviceJourney);

    CommonDataRepository commonDataRepository = Mockito.mock(
      CommonDataRepository.class
    );
    StopPlaceRepository stopPlaceRepository = Mockito.mock(
      StopPlaceRepository.class
    );

    for (int i = 0; i < quayCoordinates.size(); i++) {
      QuayId testQuayId = new QuayId("TST:Quay:" + (i + 1));

      Mockito
        .when(commonDataRepository.hasQuayIds(anyString()))
        .thenReturn(false);
      Mockito
        .when(stopPlaceRepository.getCoordinatesForQuayId(testQuayId))
        .thenReturn(quayCoordinates.get(i));

      PassengerStopAssignment passengerStopAssignment = testData
        .passengerStopAssignment()
        .withScheduleStopPointId(i + 1)
        .withStopPlaceId(i + 1)
        .withQuayId(i + 1)
        .create();

      createNetexEntitiesIndex.addPassengerStopAssignment(
        passengerStopAssignment
      );
    }

    ValidationReport validationReport = setupAndRunValidation(
      createNetexEntitiesIndex.create(),
      commonDataRepository,
      stopPlaceRepository
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void testNoPassengerStopAssignmentsFoundShouldIgnoreValidationGracefully() {
    NetexTestFragment testData = new NetexTestFragment();
    JourneyPattern journeyPattern = testData.journeyPattern().create();
    ServiceJourney serviceJourney = testData
      .serviceJourney(journeyPattern)
      .create();
    serviceJourney.withTransportMode(AllVehicleModesOfTransportEnumeration.BUS);

    NetexTestFragment.CreateNetexEntitiesIndex createNetexEntitiesIndex =
      testData.netexEntitiesIndex(journeyPattern, serviceJourney);

    CommonDataRepository commonDataRepository = Mockito.mock(
      CommonDataRepository.class
    );
    StopPlaceRepository stopPlaceRepository = Mockito.mock(
      StopPlaceRepository.class
    );

    Mockito
      .when(commonDataRepository.hasQuayIds(anyString()))
      .thenReturn(false);

    ValidationReport validationReport = setupAndRunValidation(
      createNetexEntitiesIndex.create(),
      commonDataRepository,
      stopPlaceRepository
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  private static ValidationReport runTestWithQuayCoordinates(
    List<QuayCoordinates> quayCoordinates
  ) {
    NetexTestFragment testData = new NetexTestFragment();
    JourneyPattern journeyPattern = testData.journeyPattern().create();
    ServiceJourney serviceJourney = testData
      .serviceJourney(journeyPattern)
      .create();
    serviceJourney.withTransportMode(AllVehicleModesOfTransportEnumeration.BUS);

    return runTestWith(
      quayCoordinates,
      testData.netexEntitiesIndex(journeyPattern, serviceJourney).create()
    );
  }

  private static ValidationReport runTestWith(
    List<QuayCoordinates> quayCoordinates,
    NetexEntitiesIndex netexEntitiesIndex
  ) {
    CommonDataRepository commonDataRepository = Mockito.mock(
      CommonDataRepository.class
    );
    StopPlaceRepository stopPlaceRepository = Mockito.mock(
      StopPlaceRepository.class
    );

    for (int i = 0; i < quayCoordinates.size(); i++) {
      QuayId testQuayId = new QuayId("TST:Quay:" + (i + 1));

      Mockito
        .when(
          commonDataRepository.findQuayIdForScheduledStopPoint(
            eq(new ScheduledStopPointId("TST:ScheduledStopPoint:" + (i + 1))),
            anyString()
          )
        )
        .thenReturn(testQuayId);
      Mockito
        .when(commonDataRepository.hasQuayIds(anyString()))
        .thenReturn(true);
      Mockito
        .when(stopPlaceRepository.getCoordinatesForQuayId(testQuayId))
        .thenReturn(quayCoordinates.get(i));
    }

    return setupAndRunValidation(
      netexEntitiesIndex,
      commonDataRepository,
      stopPlaceRepository
    );
  }

  private static ValidationReport setupAndRunValidation(
    NetexEntitiesIndex netexEntitiesIndex,
    CommonDataRepository commonDataRepository,
    StopPlaceRepository stopPlaceRepository
  ) {
    UnexpectedSpeed unexpectedSpeed = new UnexpectedSpeed(
      (code, message, dataLocation) ->
        new ValidationReportEntry(
          message,
          code,
          ValidationReportEntrySeverity.ERROR
        ),
      commonDataRepository,
      stopPlaceRepository
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

    unexpectedSpeed.validate(testValidationReport, validationContext);

    return testValidationReport;
  }
}
