package no.entur.antu.validation.validator.journeypattern.stoppoint.samestoppoints;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.List;
import java.util.stream.IntStream;
import no.entur.antu.netex.test.NetexTestData;
import no.entur.antu.netex.test.ValidatorTestBase;
import no.entur.antu.netex.test.builder.JourneyPatternBuilder;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.junit.jupiter.api.Test;

class SameStopPointsValidatorTest extends ValidatorTestBase {

  private ValidationReport runValidation(
    NetexEntitiesIndex netexEntitiesIndex
  ) {
    return runValidationOnLineFile(
      netexEntitiesIndex,
      new SameStopPointsValidator()
    );
  }

  @Test
  void testAllJourneyPatternsHaveDifferentStopPoints() {
    NetexTestData netexEntitiesTestFactory = new NetexTestData();

    IntStream
      .rangeClosed(1, 8)
      .forEach(i ->
        netexEntitiesTestFactory.addJourneyPattern(i).addStopPoint(i)
      );

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.build()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void testAllJourneyPatternsHaveSameStopPoints() {
    NetexTestData netexEntitiesTestFactory = new NetexTestData();
    int sameStopPointId = 987;

    IntStream
      .rangeClosed(1, 8)
      .forEach(i ->
        netexEntitiesTestFactory
          .addJourneyPattern(i)
          .addStopPoint(sameStopPointId)
      );

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.build()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
  }

  @Test
  void testMultiplePairsOfJourneyPatternsHaveSameStopPoints() {
    NetexTestData netexEntitiesTestFactory = new NetexTestData();
    int sameStopPointId1 = 987;
    int sameStopPointId2 = 988;

    netexEntitiesTestFactory
      .addJourneyPattern(123)
      .addStopPoint(sameStopPointId1);
    netexEntitiesTestFactory
      .addJourneyPattern(345)
      .addStopPoint(sameStopPointId1);

    netexEntitiesTestFactory
      .addJourneyPattern(567)
      .addStopPoint(sameStopPointId2);
    netexEntitiesTestFactory
      .addJourneyPattern(789)
      .addStopPoint(sameStopPointId2);

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.build()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(2));
  }

  @Test
  void testAllJourneyPatternsWithMultipleStopPointsHaveSameStopPoints() {
    NetexTestData netexEntitiesTestFactory = new NetexTestData();
    int stopPointInJourneyPatternId = 987;

    JourneyPatternBuilder journeyPatternBuilder =
      netexEntitiesTestFactory.addJourneyPattern(123);

    IntStream
      .rangeClosed(1, 10)
      .forEach(i ->
        journeyPatternBuilder.addStopPoint(stopPointInJourneyPatternId + 1)
      );

    JourneyPatternBuilder journeyPatternBuilder2 =
      netexEntitiesTestFactory.addJourneyPattern(345);

    IntStream
      .rangeClosed(1, 10)
      .forEach(i ->
        journeyPatternBuilder2.addStopPoint(stopPointInJourneyPatternId + 1)
      );

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.build()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
  }

  @Test
  void testTwoJourneyPatternsOutOfTenHaveSameStopPoints() {
    NetexTestData netexEntitiesTestFactory = new NetexTestData();
    int stopPointInJourneyPatternId = 987;

    netexEntitiesTestFactory
      .addJourneyPattern(123)
      .addStopPoint(stopPointInJourneyPatternId);
    netexEntitiesTestFactory
      .addJourneyPattern(345)
      .addStopPoint(stopPointInJourneyPatternId);

    IntStream
      .rangeClosed(1, 8)
      .forEach(i ->
        netexEntitiesTestFactory
          .addJourneyPattern(i)
          .addStopPoint(stopPointInJourneyPatternId + i)
      );

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.build()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
  }

  @Test
  void testJourneyPatternsWithUnSortedSameStopPoints() {
    NetexTestData netexEntitiesTestFactory = new NetexTestData();
    int stopPointInJourneyPatternId = 987;

    List<Integer> stopPointsOrder1 = List.of(8, 3, 5, 2, 4, 10, 9, 1, 7, 6);

    JourneyPatternBuilder journeyPatternBuilder =
      netexEntitiesTestFactory.addJourneyPattern(123);

    IntStream
      .rangeClosed(1, 10)
      .forEach(i ->
        journeyPatternBuilder
          .addStopPoint(stopPointInJourneyPatternId + 1)
          .withOrder(stopPointsOrder1.get(i - 1))
      );

    JourneyPatternBuilder journeyPatternBuilder2 =
      netexEntitiesTestFactory.addJourneyPattern(345);

    IntStream
      .rangeClosed(1, 10)
      .forEach(i ->
        journeyPatternBuilder2
          .addStopPoint(stopPointInJourneyPatternId + 1)
          .withOrder(i)
      );

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.build()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
  }
}
