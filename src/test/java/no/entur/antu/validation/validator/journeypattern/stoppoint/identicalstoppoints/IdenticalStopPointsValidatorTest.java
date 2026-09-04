package no.entur.antu.validation.validator.journeypattern.stoppoint.identicalstoppoints;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;
import no.entur.antu.netex.test.NetexTestData;
import no.entur.antu.netex.test.ValidatorTestBase;
import no.entur.antu.netex.test.builder.JourneyPatternBuilder;
import no.entur.antu.netex.test.builder.NetexRefs;
import no.entur.antu.netex.test.builder.StopPointInJourneyPatternBuilder;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.model.QuayId;
import org.entur.netex.validation.validator.model.ScheduledStopPointId;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

class IdenticalStopPointsValidatorTest extends ValidatorTestBase {

  private ValidationReport runValidation(
    NetexEntitiesIndex netexEntitiesIndex
  ) {
    return runValidationOnLineFile(
      netexEntitiesIndex,
      new IdenticalStopPointsValidator()
    );
  }

  @Test
  void testNoStopPointsInJourneyPattern() {
    int numberOfJourneyPatterns = 1;
    int numberOfStopPoints = 0;

    ValidationReport validationReport = getValidationReport(
      numberOfJourneyPatterns,
      numberOfStopPoints,
      (journeyPatternRef, stopPointInJourneyPatternRef) -> {}
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void testIdenticalStopPoints() {
    int numberOfJourneyPatterns = 2;
    int numberOfStopPoints = 5;

    ValidationReport validationReport = getValidationReport(
      numberOfJourneyPatterns,
      numberOfStopPoints,
      (journeyPatternRef, stopPointInJourneyPatternRef) -> {}
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
  }

  /**
   * Test the different destination displays for the stop points in journey patterns are valid
   */
  @Test
  void testDifferentDestinationDisplays() {
    int numberOfJourneyPatterns = 2;
    int numberOfStopPoints = 2;

    ValidationReport validationReport = getValidationReport(
      numberOfJourneyPatterns,
      numberOfStopPoints,
      (journeyPatternRef, stopPointInJourneyPatternRef) -> {
        if (
          journeyPatternRef.ref().equals("TST:JourneyPattern:1") &&
          stopPointInJourneyPatternRef
            .ref()
            .equals("TST:StopPointInJourneyPattern:1")
        ) {
          stopPointInJourneyPatternRef.withDestinationDisplayId(
            NetexRefs.destinationDisplayRef(1)
          );
        }
        if (
          journeyPatternRef.ref().equals("TST:JourneyPattern:2") &&
          stopPointInJourneyPatternRef
            .ref()
            .equals("TST:StopPointInJourneyPattern:1")
        ) {
          stopPointInJourneyPatternRef.withDestinationDisplayId(
            NetexRefs.destinationDisplayRef(2)
          );
        }
      }
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  /**
   * Test the different ScheduleStopPoints for the stop points in journey patterns are valid
   */
  @Test
  void testDifferentScheduledStopPoints() {
    int numberOfJourneyPatterns = 2;
    int numberOfStopPoints = 2;

    ValidationReport validationReport = getValidationReport(
      numberOfJourneyPatterns,
      numberOfStopPoints,
      (journeyPatternRef, stopPointInJourneyPatternRef) -> {
        if (
          journeyPatternRef.ref().equals("TST:JourneyPattern:1") &&
          stopPointInJourneyPatternRef
            .ref()
            .equals("TST:StopPointInJourneyPattern:1")
        ) {
          stopPointInJourneyPatternRef.withScheduledStopPointRef(
            NetexRefs.scheduledStopPointRef(1)
          );
        }
        if (
          journeyPatternRef.ref().equals("TST:JourneyPattern:2") &&
          stopPointInJourneyPatternRef
            .ref()
            .equals("TST:StopPointInJourneyPattern:1")
        ) {
          stopPointInJourneyPatternRef.withScheduledStopPointRef(
            NetexRefs.scheduledStopPointRef(2)
          );
        }
      }
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  /**
   * Test the different ScheduleStopPoints for the stop points in journey patterns are valid
   */
  @Test
  void testDifferentBoardingAndAlighting() {
    int numberOfJourneyPatterns = 2;
    int numberOfStopPoints = 3;

    ValidationReport validationReport = getValidationReport(
      numberOfJourneyPatterns,
      numberOfStopPoints,
      (journeyPatternRef, stopPointInJourneyPatternRef) -> {
        if (
          journeyPatternRef.ref().equals("TST:JourneyPattern:1") &&
          stopPointInJourneyPatternRef
            .ref()
            .equals("TST:StopPointInJourneyPattern:2")
        ) {
          stopPointInJourneyPatternRef.withForBoarding(true);
        }
        if (
          journeyPatternRef.ref().equals("TST:JourneyPattern:2") &&
          stopPointInJourneyPatternRef
            .ref()
            .equals("TST:StopPointInJourneyPattern:1")
        ) {
          stopPointInJourneyPatternRef.withForBoarding(false);
        }
      }
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @NotNull
  private ValidationReport getValidationReport(
    int numberOfJourneyPatterns,
    int numberOfStopPoints,
    BiConsumer<JourneyPatternBuilder, StopPointInJourneyPatternBuilder> customizeStopPoint
  ) {
    NetexTestData netexEntitiesTestFactory = new NetexTestData();

    List<JourneyPatternBuilder> journeyPatternBuilders = IntStream
      .rangeClosed(1, numberOfJourneyPatterns)
      .mapToObj(netexEntitiesTestFactory::addJourneyPattern)
      .toList();

    if (numberOfStopPoints > 0) {
      journeyPatternBuilders.forEach(journeyPatternBuilder -> {
        List<StopPointInJourneyPatternBuilder> stopPointsInJourneyPatterns =
          journeyPatternBuilder.addStopPoints(numberOfStopPoints);

        stopPointsInJourneyPatterns.forEach(createStopPointInJourneyPattern ->
          customizeStopPoint.accept(
            journeyPatternBuilder,
            createStopPointInJourneyPattern
          )
        );
      });
    }

    IntStream
      .rangeClosed(1, numberOfStopPoints)
      .forEach(stopPointId ->
        withQuayId(
          new ScheduledStopPointId("TST:ScheduledStopPoint:" + stopPointId),
          new QuayId("TST:Quay:" + stopPointId)
        )
      );

    return runValidation(netexEntitiesTestFactory.build());
  }
}
