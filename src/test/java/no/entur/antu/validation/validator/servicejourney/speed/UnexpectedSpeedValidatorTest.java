package no.entur.antu.validation.validator.servicejourney.speed;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.List;
import no.entur.antu.netextestdata.NetexTestFragment;
import no.entur.antu.validation.ValidationTest;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.entur.netex.validation.validator.model.QuayCoordinates;
import org.entur.netex.validation.validator.model.QuayId;
import org.entur.netex.validation.validator.model.ScheduledStopPointId;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.PassengerStopAssignment;
import org.rutebanken.netex.model.ServiceJourney;

class UnexpectedSpeedValidatorTest extends ValidationTest {

  private ValidationReport runValidation(
    NetexEntitiesIndex netexEntitiesIndex
  ) {
    return runValidationOnLineFile(
      netexEntitiesIndex,
      UnexpectedSpeedValidator.class
    );
  }

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

    // Setting departureTimeOffset to 0, for making the departure time same for both passingTimes.
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

    mockNoQuayIdsInNetexDataRepository();

    for (int i = 0; i < quayCoordinates.size(); i++) {
      mockGetCoordinates(
        new QuayId("TST:Quay:" + (i + 1)),
        quayCoordinates.get(i)
      );

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

    ValidationReport validationReport = runValidation(
      createNetexEntitiesIndex.create()
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

    mockNoQuayIdsInNetexDataRepository();

    ValidationReport validationReport = runValidation(
      createNetexEntitiesIndex.create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  private ValidationReport runTestWithQuayCoordinates(
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

  private ValidationReport runTestWith(
    List<QuayCoordinates> quayCoordinates,
    NetexEntitiesIndex netexEntitiesIndex
  ) {
    for (int i = 0; i < quayCoordinates.size(); i++) {
      mockGetCoordinates(
        new ScheduledStopPointId("TST:ScheduledStopPoint:" + (i + 1)),
        new QuayId("TST:Quay:" + (i + 1)),
        quayCoordinates.get(i)
      );
    }

    return runValidation(netexEntitiesIndex);
  }
}
