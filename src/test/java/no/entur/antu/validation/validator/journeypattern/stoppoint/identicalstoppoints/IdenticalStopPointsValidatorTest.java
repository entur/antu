package no.entur.antu.validation.validator.journeypattern.stoppoint.identicalstoppoints;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;
import no.entur.antu.model.QuayId;
import no.entur.antu.model.ScheduledStopPointId;
import no.entur.antu.netextestdata.NetexTestFragment;
import no.entur.antu.netextestdata.NetexTestFragment.CreateStopPointInJourneyPattern;
import no.entur.antu.validation.ValidationTest;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.JourneyPattern;

class IdenticalStopPointsValidatorTest extends ValidationTest {

  private ValidationReport runValidation(
    NetexEntitiesIndex netexEntitiesIndex
  ) {
    return runValidationOnLineFile(
      netexEntitiesIndex,
      IdenticalStopPointsValidator.class
    );
  }

  @Test
  void testNoStopPointsInJourneyPattern() {
    int numberOfJourneyPatterns = 1;
    int numberOfStopPoints = 0;

    ValidationReport validationReport = getValidationReport(
      numberOfJourneyPatterns,
      numberOfStopPoints,
      UnaryOperator.identity()
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
      UnaryOperator.identity()
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
      create -> {
        if (create.journeyPatternId() == 1 && create.stopPointId() == 1) {
          create.createStopPointInJourneyPattern().withDestinationDisplayId(1);
        }
        if (create.journeyPatternId() == 2 && create.stopPointId() == 1) {
          create.createStopPointInJourneyPattern().withDestinationDisplayId(2);
        }
        return create;
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
      create -> {
        if (create.journeyPatternId() == 1 && create.stopPointId() == 1) {
          create.createStopPointInJourneyPattern().withScheduledStopPointId(1);
        }
        if (create.journeyPatternId() == 2 && create.stopPointId() == 1) {
          create.createStopPointInJourneyPattern().withScheduledStopPointId(2);
        }
        return create;
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
      create -> {
        if (create.journeyPatternId() == 1 && create.stopPointId() == 2) {
          create.createStopPointInJourneyPattern().withForBoarding(true);
        }
        if (create.journeyPatternId() == 2 && create.stopPointId() == 1) {
          create.createStopPointInJourneyPattern().withForBoarding(false);
        }
        return create;
      }
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  private record CreateStopPointInJourneyPatternContext(
    int stopPointId,
    int journeyPatternId,
    CreateStopPointInJourneyPattern createStopPointInJourneyPattern
  ) {}

  @NotNull
  private ValidationReport getValidationReport(
    int numberOfJourneyPatterns,
    int numberOfStopPoints,
    UnaryOperator<CreateStopPointInJourneyPatternContext> customizeStopPoint
  ) {
    NetexTestFragment testFragment = new NetexTestFragment();

    List<NetexTestFragment.CreateJourneyPattern> createJourneyPatterns =
      IntStream
        .rangeClosed(1, numberOfJourneyPatterns)
        .mapToObj(journeyPatternId ->
          testFragment.journeyPattern().withId(journeyPatternId)
        )
        .toList();

    if (numberOfStopPoints > 0) {
      createJourneyPatterns.forEach(createJourneyPattern ->
        createJourneyPattern.withStopPointsInJourneyPattern(
          IntStream
            .rangeClosed(1, numberOfStopPoints)
            .mapToObj(stopPointId ->
              new CreateStopPointInJourneyPatternContext(
                stopPointId,
                createJourneyPattern.id(),
                new CreateStopPointInJourneyPattern(createJourneyPattern)
                  .withId(stopPointId)
                  .withScheduledStopPointId(stopPointId)
                  .withDestinationDisplayId(stopPointId)
                  .withForBoarding(stopPointId == 1) // first stop point
                  .withForAlighting(stopPointId == numberOfStopPoints) // last stop point
              )
            )
            .map(customizeStopPoint)
            .map(
              CreateStopPointInJourneyPatternContext::createStopPointInJourneyPattern
            )
            .map(CreateStopPointInJourneyPattern::create)
            .toList()
        )
      );
    } else {
      createJourneyPatterns.forEach(createJourneyPattern ->
        createJourneyPattern.withNumberOfStopPointInJourneyPattern(0)
      );
    }

    IntStream
      .rangeClosed(1, numberOfStopPoints)
      .forEach(stopPointId ->
        mockGetQuayId(
          new ScheduledStopPointId("TST:ScheduledStopPoint:" + stopPointId),
          new QuayId("TST:Quay:" + stopPointId)
        )
      );

    return runValidation(
      testFragment
        .netexEntitiesIndex()
        .addJourneyPatterns(
          createJourneyPatterns
            .stream()
            .map(NetexTestFragment.CreateJourneyPattern::create)
            .toArray(JourneyPattern[]::new)
        )
        .create()
    );
  }
}
