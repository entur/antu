package no.entur.antu.validation.validator.journeypattern.stoppoint.samestoppoints;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.List;
import java.util.stream.IntStream;
import no.entur.antu.netextestdata.NetexEntitiesTestFactory;
import no.entur.antu.validation.ValidationTest;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.junit.jupiter.api.Test;

class SameStopPointsValidatorTest extends ValidationTest {

  private ValidationReport runValidation(
    NetexEntitiesIndex netexEntitiesIndex
  ) {
    return runValidationOnLineFile(
      netexEntitiesIndex,
      SameStopPointsValidator.class
    );
  }

  @Test
  void testAllJourneyPatternsHaveDifferentStopPoints() {
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

    IntStream
      .rangeClosed(1, 8)
      .forEach(i ->
        netexEntitiesTestFactory
          .createJourneyPattern(i)
          .createStopPointInJourneyPattern(i)
      );

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void testAllJourneyPatternsHaveSameStopPoints() {
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();
    int sameStopPointId = 987;

    IntStream
      .rangeClosed(1, 8)
      .forEach(i ->
        netexEntitiesTestFactory
          .createJourneyPattern(i)
          .createStopPointInJourneyPattern(sameStopPointId)
      );

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
  }

  @Test
  void testMultiplePairsOfJourneyPatternsHaveSameStopPoints() {
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();
    int sameStopPointId1 = 987;
    int sameStopPointId2 = 988;

    netexEntitiesTestFactory
      .createJourneyPattern(123)
      .createStopPointInJourneyPattern(sameStopPointId1);
    netexEntitiesTestFactory
      .createJourneyPattern(345)
      .createStopPointInJourneyPattern(sameStopPointId1);

    netexEntitiesTestFactory
      .createJourneyPattern(567)
      .createStopPointInJourneyPattern(sameStopPointId2);
    netexEntitiesTestFactory
      .createJourneyPattern(789)
      .createStopPointInJourneyPattern(sameStopPointId2);

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(2));
  }

  @Test
  void testAllJourneyPatternsWithMultipleStopPointsHaveSameStopPoints() {
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();
    int stopPointInJourneyPatternId = 987;

    NetexEntitiesTestFactory.CreateJourneyPattern createJourneyPattern123 =
      netexEntitiesTestFactory.createJourneyPattern(123);

    IntStream
      .rangeClosed(1, 10)
      .forEach(i ->
        createJourneyPattern123.createStopPointInJourneyPattern(
          stopPointInJourneyPatternId + 1
        )
      );

    NetexEntitiesTestFactory.CreateJourneyPattern createJourneyPattern345 =
      netexEntitiesTestFactory.createJourneyPattern(345);

    IntStream
      .rangeClosed(1, 10)
      .forEach(i ->
        createJourneyPattern345.createStopPointInJourneyPattern(
          stopPointInJourneyPatternId + 1
        )
      );

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
  }

  @Test
  void testTwoJourneyPatternsOutOfTenHaveSameStopPoints() {
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();
    int stopPointInJourneyPatternId = 987;

    netexEntitiesTestFactory
      .createJourneyPattern(123)
      .createStopPointInJourneyPattern(stopPointInJourneyPatternId);
    netexEntitiesTestFactory
      .createJourneyPattern(345)
      .createStopPointInJourneyPattern(stopPointInJourneyPatternId);

    IntStream
      .rangeClosed(1, 8)
      .forEach(i ->
        netexEntitiesTestFactory
          .createJourneyPattern(i)
          .createStopPointInJourneyPattern(stopPointInJourneyPatternId + i)
      );

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
  }

  @Test
  void testJourneyPatternsWithUnSortedSameStopPoints() {
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();
    int stopPointInJourneyPatternId = 987;

    List<Integer> stopPointsOrder1 = List.of(8, 3, 5, 2, 4, 10, 9, 1, 7, 6);

    NetexEntitiesTestFactory.CreateJourneyPattern createJourneyPattern123 =
      netexEntitiesTestFactory.createJourneyPattern(123);

    IntStream
      .rangeClosed(1, 10)
      .forEach(i ->
        createJourneyPattern123
          .createStopPointInJourneyPattern(stopPointInJourneyPatternId + 1)
          .withOrder(stopPointsOrder1.get(i - 1))
      );

    NetexEntitiesTestFactory.CreateJourneyPattern createJourneyPattern345 =
      netexEntitiesTestFactory.createJourneyPattern(345);

    IntStream
      .rangeClosed(1, 10)
      .forEach(i ->
        createJourneyPattern345
          .createStopPointInJourneyPattern(stopPointInJourneyPatternId + 1)
          .withOrder(i)
      );

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
  }
}
