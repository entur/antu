package no.entur.antu.validation.validator.journeypattern.stoppoint.stoppointscount;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.stream.IntStream;
import no.entur.antu.netextestdata.NetexTestFragment;
import no.entur.antu.validation.ValidationTest;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.rutebanken.netex.model.JourneyPattern;

class StopPointsCountTest extends ValidationTest {

  private ValidationReport runValidation(
    NetexEntitiesIndex netexEntitiesIndex
  ) {
    return runValidationOnLineFile(netexEntitiesIndex, StopPointsCount.class);
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
    NetexTestFragment testFragment = new NetexTestFragment();
    int stopPointInJourneyPatternIdOffset = 123;

    JourneyPattern journeyPattern = testFragment
      .journeyPattern()
      .withId(123)
      .withStopPointsInJourneyPattern(
        IntStream
          .rangeClosed(1, 10)
          .mapToObj(i ->
            testFragment
              .stopPointInJourneyPattern(123)
              .withId(stopPointInJourneyPatternIdOffset + 1)
              .create()
          )
          .toList()
      )
      .withNumberOfServiceLinksInJourneyPattern(0)
      .create();

    ValidationReport validationReport = runValidation(
      testFragment
        .netexEntitiesIndex()
        .addJourneyPatterns(journeyPattern)
        .create()
    );

    assertTrue(validationReport.getValidationReportEntries().isEmpty());
  }

  private ValidationReport runWith10StopPoints(int numberOfServiceLinks) {
    NetexTestFragment testFragment = new NetexTestFragment();
    int stopPointInJourneyPatternIdOffset = 123;
    int linksInJourneyPatternIdOffset = 234;

    JourneyPattern journeyPattern = testFragment
      .journeyPattern()
      .withId(123)
      .withStopPointsInJourneyPattern(
        IntStream
          .rangeClosed(1, 10)
          .mapToObj(i ->
            testFragment
              .stopPointInJourneyPattern(123)
              .withId(stopPointInJourneyPatternIdOffset + 1)
              .create()
          )
          .toList()
      )
      .withServiceLinksInJourneyPattern(
        IntStream
          .rangeClosed(1, numberOfServiceLinks)
          .mapToObj(i ->
            testFragment
              .linkInJourneyPattern(123)
              .withId(linksInJourneyPatternIdOffset + 1)
              .create()
          )
          .toList()
      )
      .create();

    return runValidation(
      testFragment
        .netexEntitiesIndex()
        .addJourneyPatterns(journeyPattern)
        .create()
    );
  }
}
