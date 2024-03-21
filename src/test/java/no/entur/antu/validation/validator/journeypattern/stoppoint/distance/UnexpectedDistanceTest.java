package no.entur.antu.validation.validator.journeypattern.stoppoint.distance;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;
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
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.Route;

class UnexpectedDistanceTest {

  @Test
  void testDistanceBetweenTwoStopPointsInJourneyPatternIsWithinLimits() {
    ValidationReport validationReport = runWithQuayCoordinates(
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
  void testMissingTransportModeOnLineShouldIgnoreValidationGracefully() {
    ValidationReport validationReport = runWithQuayCoordinates(
      null, // Mocking missing transportMode
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
  void testDistanceBetweenTwoStopPointsInJourneyPatternIsLessThanMinDistance() {
    ValidationReport validationReport = runWithQuayCoordinates(
      List.of(
        new QuayCoordinates(34.052235, -118.243683),
        new QuayCoordinates(34.052285, -118.243753),
        new QuayCoordinates(34.052330, -118.243832)
      )
    );

    Collection<ValidationReportEntry> validationReportEntries =
      validationReport.getValidationReportEntries();

    assertThat(validationReportEntries.size(), is(2));
    validationReportEntries.forEach(entry ->
      assertThat(
        entry.getName(),
        is(
          UnexpectedDistanceError.RuleCode.DISTANCE_BETWEEN_STOP_POINTS_LESS_THAN_EXPECTED.name()
        )
      )
    );
  }

  @Test
  void testDistanceBetweenTwoStopPointsInJourneyPatternIsMoreThanMaxDistance() {
    ValidationReport validationReport = runWithQuayCoordinates(
      List.of(
        new QuayCoordinates(51.5074, -0.1278),
        new QuayCoordinates(40.7128, -74.0060)
      )
    );

    Collection<ValidationReportEntry> validationReportEntries =
      validationReport.getValidationReportEntries();

    assertThat(validationReportEntries.size(), is(1));
    validationReportEntries.forEach(entry ->
      assertThat(
        entry.getName(),
        is(
          UnexpectedDistanceError.RuleCode.DISTANCE_BETWEEN_STOP_POINTS_MORE_THAN_EXPECTED.name()
        )
      )
    );
  }

  @Test
  void testVaryingDistancesBetweenStopPointsInJourneyPattern() {
    ValidationReport validationReport = runWithQuayCoordinates(
      List.of(
        new QuayCoordinates(34.052235, -118.243683),
        new QuayCoordinates(34.052285, -118.243753),
        new QuayCoordinates(34.052330, -118.243832),
        new QuayCoordinates(40.7128, -74.0060)
      )
    );

    Collection<ValidationReportEntry> validationReportEntries =
      validationReport.getValidationReportEntries();

    assertThat(validationReportEntries.size(), is(3));
    assertTrue(
      validationReportEntries
        .stream()
        .map(ValidationReportEntry::getName)
        .anyMatch(name ->
          name.equals(
            UnexpectedDistanceError.RuleCode.DISTANCE_BETWEEN_STOP_POINTS_LESS_THAN_EXPECTED.name()
          )
        )
    );
    assertTrue(
      validationReportEntries
        .stream()
        .map(ValidationReportEntry::getName)
        .anyMatch(name ->
          name.equals(
            UnexpectedDistanceError.RuleCode.DISTANCE_BETWEEN_STOP_POINTS_MORE_THAN_EXPECTED.name()
          )
        )
    );
  }

  private static ValidationReport runWithQuayCoordinates(
    List<QuayCoordinates> coordinates
  ) {
    return runWithQuayCoordinates(
      AllVehicleModesOfTransportEnumeration.BUS,
      coordinates
    );
  }

  private static ValidationReport runWithQuayCoordinates(
    AllVehicleModesOfTransportEnumeration transportMode,
    List<QuayCoordinates> coordinates
  ) {
    NetexTestFragment testFragment = new NetexTestFragment();

    Line line = testFragment.line().withTransportMode(transportMode).create();

    Route route = testFragment.route().withLine(line).create();

    JourneyPattern journeyPattern = testFragment
      .journeyPattern()
      .withId(123)
      .withStopPointsInJourneyPattern(
        IntStream
          .rangeClosed(1, coordinates.size())
          .mapToObj(i ->
            testFragment
              .stopPointInJourneyPattern(123)
              .withId(i)
              .withScheduledStopPointId(i)
              .create()
          )
          .toList()
      )
      .withRoute(route)
      .create();

    NetexEntitiesIndex netexEntitiesIndex = testFragment
      .netexEntitiesIndex()
      .addJourneyPatterns(journeyPattern)
      .addLine(line)
      .addRoute(route)
      .create();

    return runTestWith(coordinates, netexEntitiesIndex);
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

    Mockito.when(commonDataRepository.hasQuayIds(anyString())).thenReturn(true);

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
    UnexpectedDistance unexpectedDistance = new UnexpectedDistance(
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

    unexpectedDistance.validate(testValidationReport, validationContext);

    return testValidationReport;
  }
}
