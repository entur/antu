package no.entur.antu.validator.speedvalidator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import no.entur.antu.commondata.CommonDataRepository;
import no.entur.antu.model.QuayId;
import no.entur.antu.model.StopPlaceCoordinates;
import no.entur.antu.stop.StopPlaceRepository;
import no.entur.antu.validator.ValidationContextWithNetexEntitiesIndex;
import no.entur.antu.validator.nonincreasingpassingtime.NetexTestDataSample;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.index.impl.NetexEntitiesIndexImpl;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.entur.netex.validation.validator.ValidationReportEntrySeverity;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.rutebanken.netex.model.*;

class SpeedValidatorTest {

  @Test
  void normalSpeedShouldNotReturnAnyValidationEntry() {
    ValidationReport validationReport = runTestWithStopPlaceCoordinates(
      List.of(
        new StopPlaceCoordinates(6.621791, 60.424023),
        new StopPlaceCoordinates(6.612112, 60.471748),
        new StopPlaceCoordinates(6.622312, 60.481548),
        new StopPlaceCoordinates(6.632312, 60.491548)
      )
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void lowSpeedShouldReturnValidationEntryForLowSpeed() {
    ValidationReport validationReport = runTestWithStopPlaceCoordinates(
      List.of(
        new StopPlaceCoordinates(6.621791, 60.424023),
        new StopPlaceCoordinates(6.612112, 60.471748),
        new StopPlaceCoordinates(6.612312, 60.471548),
        new StopPlaceCoordinates(6.632312, 60.491548)
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
      is(SpeedError.RuleCode.LOW_SPEED.name())
    );
  }

  @Test
  void highSpeedShouldReturnValidationEntryForHighSpeed() {
    ValidationReport validationReport = runTestWithStopPlaceCoordinates(
      List.of(
        new StopPlaceCoordinates(6.621791, 60.424023),
        new StopPlaceCoordinates(6.612112, 60.471748),
        new StopPlaceCoordinates(6.602312, 60.471548),
        new StopPlaceCoordinates(6.592312, 61.491548)
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
      is(SpeedError.RuleCode.HIGH_SPEED.name())
    );
  }

  @Test
  void warningSpeedShouldReturnValidationEntryForHighSpeed() {
    ValidationReport validationReport = runTestWithStopPlaceCoordinates(
      List.of(
        new StopPlaceCoordinates(6.621791, 60.424023),
        new StopPlaceCoordinates(6.612112, 60.471748),
        new StopPlaceCoordinates(6.602312, 60.471548),
        new StopPlaceCoordinates(6.592312, 60.551548)
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
      is(SpeedError.RuleCode.WARNING_SPEED.name())
    );
  }

  @Test
  void multipleSpeedViolationShouldBeDetected() {
    ValidationReport validationReport = runTestWithStopPlaceCoordinates(
      List.of(
        new StopPlaceCoordinates(6.621791, 60.424023),
        new StopPlaceCoordinates(6.612112, 60.471748),
        new StopPlaceCoordinates(6.612312, 60.471548),
        new StopPlaceCoordinates(6.592312, 61.491548)
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
          SpeedError.RuleCode.LOW_SPEED.name(),
          SpeedError.RuleCode.HIGH_SPEED.name()
        )
      )
    );
  }

  @Test
  void testSameDepartureArrivalTimeErrorThrown() {
    ValidationReport validationReport = runTestWithStopPlaceCoordinates(
      new NetexTestDataSample(new int[] { 0, 0 }), // Same departure time for both passingTimes.
      List.of(
        StopPlaceCoordinates.fromString("11.189184§60.41041"),
        StopPlaceCoordinates.fromString("11.193265§60.446804")
      )
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

  private static ValidationReport runTestWithStopPlaceCoordinates(
    List<StopPlaceCoordinates> stopPlaceCoordinates
  ) {
    return runTestWithStopPlaceCoordinates(
      new NetexTestDataSample(),
      stopPlaceCoordinates
    );
  }

  private static ValidationReport runTestWithStopPlaceCoordinates(
    NetexTestDataSample sample,
    List<StopPlaceCoordinates> stopPlaceCoordinates
  ) {
    ServiceJourney serviceJourney = sample.getServiceJourney();
    JourneyPattern journeyPattern = sample.getJourneyPattern();

    serviceJourney.withTransportMode(AllVehicleModesOfTransportEnumeration.BUS);

    CommonDataRepository commonDataRepository = Mockito.mock(
      CommonDataRepository.class
    );
    StopPlaceRepository stopPlaceRepository = Mockito.mock(
      StopPlaceRepository.class
    );

    for (int i = 0; i < stopPlaceCoordinates.size(); i++) {
      QuayId testQuayId = new QuayId("TST:Quay:" + (i + 1));

      Mockito
        .when(
          commonDataRepository.findQuayIdForScheduledStopPoint(
            eq("RUT:ScheduledStopPoint:" + (i + 1)),
            anyString()
          )
        )
        .thenReturn(testQuayId);
      Mockito
        .when(stopPlaceRepository.getCoordinatesForQuayId(testQuayId))
        .thenReturn(stopPlaceCoordinates.get(i));
    }

    return setupAndRunValidation(
      createNetexEntitiesIndex(journeyPattern, serviceJourney),
      commonDataRepository,
      stopPlaceRepository
    );
  }

  private static NetexEntitiesIndex createNetexEntitiesIndex(
    JourneyPattern journeyPattern,
    ServiceJourney serviceJourney
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
                serviceJourney
              )
          )
      );

    return netexEntitiesIndex;
  }

  private static ValidationReport setupAndRunValidation(
    NetexEntitiesIndex netexEntitiesIndex,
    CommonDataRepository commonDataRepository,
    StopPlaceRepository stopPlaceRepository
  ) {
    SpeedValidator speedValidator = new SpeedValidator(
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

    speedValidator.validate(testValidationReport, validationContext);

    return testValidationReport;
  }
}
