package no.entur.antu.validation.validator.journeypattern.stoppoint.distance;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import no.entur.antu.netextestdata.NetexEntitiesTestFactory;
import no.entur.antu.validation.ValidationTest;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.entur.netex.validation.validator.model.QuayCoordinates;
import org.entur.netex.validation.validator.model.QuayId;
import org.entur.netex.validation.validator.model.ScheduledStopPointId;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;
import org.rutebanken.netex.model.BusSubmodeEnumeration;
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.Route;
import org.rutebanken.netex.model.TransportSubmodeStructure;

class UnexpectedDistanceBetweenStopPointsValidatorTest extends ValidationTest {

  private ValidationReport runValidation(
    NetexEntitiesIndex netexEntitiesIndex
  ) {
    return runValidationOnLineFile(
      netexEntitiesIndex,
      UnexpectedDistanceBetweenStopPointsValidator.class
    );
  }

  @Test
  void testNoStopPointsInJourneyPattern() {
    ValidationReport validationReport = runWithQuayCoordinates(
      Collections.emptyList() // This will create no stop points in the journey pattern
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

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
      null,
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
          UnexpectedDistanceBetweenStopPointsValidator.RULE_DISTANCE_BETWEEN_STOP_POINTS_LESS_THAN_EXPECTED.name()
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
          UnexpectedDistanceBetweenStopPointsValidator.RULE_DISTANCE_BETWEEN_STOP_POINTS_MORE_THAN_EXPECTED.name()
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
            UnexpectedDistanceBetweenStopPointsValidator.RULE_DISTANCE_BETWEEN_STOP_POINTS_LESS_THAN_EXPECTED.name()
          )
        )
    );
    assertTrue(
      validationReportEntries
        .stream()
        .map(ValidationReportEntry::getName)
        .anyMatch(name ->
          name.equals(
            UnexpectedDistanceBetweenStopPointsValidator.RULE_DISTANCE_BETWEEN_STOP_POINTS_MORE_THAN_EXPECTED.name()
          )
        )
    );
  }

  private ValidationReport runWithQuayCoordinates(
    List<QuayCoordinates> coordinates
  ) {
    return runWithQuayCoordinates(
      AllVehicleModesOfTransportEnumeration.BUS,
      new TransportSubmodeStructure()
        .withBusSubmode(BusSubmodeEnumeration.LOCAL_BUS),
      coordinates
    );
  }

  private ValidationReport runWithQuayCoordinates(
    AllVehicleModesOfTransportEnumeration transportMode,
    TransportSubmodeStructure submode,
    List<QuayCoordinates> coordinates
  ) {
    NetexEntitiesTestFactory testFragment = new NetexEntitiesTestFactory();

    Line line = testFragment
      .line()
      .withTransportMode(transportMode)
      .create()
      .withTransportSubmode(submode);

    Route route = testFragment.route().withLine(line).create();

    NetexEntitiesTestFactory.CreateJourneyPattern createJourneyPattern =
      testFragment.journeyPattern().withId(123).withRoute(route);

    if (coordinates.isEmpty()) {
      createJourneyPattern.withNumberOfStopPointInJourneyPattern(0);
    } else {
      createJourneyPattern.withStopPointsInJourneyPattern(
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
      );
    }

    NetexEntitiesIndex netexEntitiesIndex = testFragment
      .netexEntitiesIndex()
      .addJourneyPatterns(createJourneyPattern.create())
      .addLine(line)
      .addRoute(route)
      .create();

    return runTestWith(coordinates, netexEntitiesIndex);
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
