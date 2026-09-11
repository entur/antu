package no.entur.antu.validation.validator.journeypattern.stoppoint.stoppointscount;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.IntStream;
import no.entur.antu.netex.test.NetexTestData;
import no.entur.antu.netex.test.ValidatorTestBase;
import no.entur.antu.netex.test.builder.JourneyPatternBuilder;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class StopPointsCountValidatorTest extends ValidatorTestBase {

  private ValidationReport runValidation(
    NetexEntitiesIndex netexEntitiesIndex
  ) {
    return runValidationOnLineFile(
      netexEntitiesIndex,
      new StopPointsCountValidator()
    );
  }

  @Test
  void testJourneyPatternWithCorrectStopPointsAndServiceLinksCount() {
    ValidationReport validationReport = runWith10StopPoints(9);
    assertTrue(validationReport.getValidationReportEntries().isEmpty());
  }

  @ParameterizedTest
  @ValueSource(ints = { 11, 10, 5 })
  void testJourneyPatternWithInCorrectStopPointsAndServiceLinksCount(
    int numberOfServiceLinks
  ) {
    ValidationReport validationReport = runWith10StopPoints(
      numberOfServiceLinks
    );
    assertThat(validationReport.getValidationReportEntries().size(), is(1));
  }

  @Test
  void testJourneyPatternWithNoServiceLinks() {
    NetexTestData netexEntitiesTestFactory = new NetexTestData();
    int stopPointInJourneyPatternIdOffset = 123;

    JourneyPatternBuilder journeyPatternBuilder = netexEntitiesTestFactory
      .addJourneyPattern(123)
      .withNoServiceLinksInJourneyPattern();

    IntStream
      .rangeClosed(1, 10)
      .forEach(i ->
        journeyPatternBuilder.addStopPoint(
          stopPointInJourneyPatternIdOffset + 1
        )
      );

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.build()
    );

    assertTrue(validationReport.getValidationReportEntries().isEmpty());
  }

  private ValidationReport runWith10StopPoints(int numberOfServiceLinks) {
    NetexTestData testFragment = new NetexTestData();
    int stopPointInJourneyPatternIdOffset = 123;
    int linksInJourneyPatternIdOffset = 234;

    JourneyPatternBuilder journeyPatternBuilder =
      testFragment.addJourneyPattern(123);

    IntStream
      .rangeClosed(1, 10)
      .forEach(i ->
        journeyPatternBuilder.addStopPoint(
          stopPointInJourneyPatternIdOffset + 1
        )
      );

    IntStream
      .rangeClosed(1, numberOfServiceLinks)
      .forEach(i ->
        journeyPatternBuilder.addLink(linksInJourneyPatternIdOffset + 1)
      );

    return runValidation(testFragment.build());
  }
}
