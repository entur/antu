package no.entur.antu.validation.validator.journeypattern.stoppoint.stoppointscount;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.IntStream;
import no.entur.antu.netextestdata.NetexEntitiesTestFactory;
import no.entur.antu.validation.ValidationTest;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class StopPointsCountValidatorTest extends ValidationTest {

  private ValidationReport runValidation(
    NetexEntitiesIndex netexEntitiesIndex
  ) {
    return runValidationOnLineFile(
      netexEntitiesIndex,
      StopPointsCountValidator.class
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
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();
    int stopPointInJourneyPatternIdOffset = 123;

    NetexEntitiesTestFactory.CreateJourneyPattern createJourneyPattern123 =
      netexEntitiesTestFactory
        .createJourneyPattern(123)
        .withNoServiceLinksInJourneyPattern();

    IntStream
      .rangeClosed(1, 10)
      .forEach(i ->
        createJourneyPattern123.createStopPointInJourneyPattern(
          stopPointInJourneyPatternIdOffset + 1
        )
      );

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.create()
    );

    assertTrue(validationReport.getValidationReportEntries().isEmpty());
  }

  private ValidationReport runWith10StopPoints(int numberOfServiceLinks) {
    NetexEntitiesTestFactory testFragment = new NetexEntitiesTestFactory();
    int stopPointInJourneyPatternIdOffset = 123;
    int linksInJourneyPatternIdOffset = 234;

    NetexEntitiesTestFactory.CreateJourneyPattern createJourneyPattern123 =
      testFragment.createJourneyPattern(123);

    IntStream
      .rangeClosed(1, 10)
      .forEach(i ->
        createJourneyPattern123.createStopPointInJourneyPattern(
          stopPointInJourneyPatternIdOffset + 1
        )
      );

    IntStream
      .rangeClosed(1, numberOfServiceLinks)
      .forEach(i ->
        createJourneyPattern123.createServiceLinkInJourneyPattern(
          linksInJourneyPatternIdOffset + 1
        )
      );

    return runValidation(testFragment.create());
  }
}
