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
        netexEntitiesTestFactory.journeyPattern(i).stopPointInJourneyPattern(i)
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

    netexEntitiesTestFactory
      .journeyPattern(1)
      .stopPointInJourneyPattern(sameStopPointId);

    IntStream
      .rangeClosed(2, 8)
      .forEach(i ->
        netexEntitiesTestFactory
          .journeyPattern(i)
          .stopPointInJourneyPattern(sameStopPointId)
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
      .journeyPattern(123)
      .stopPointInJourneyPattern(sameStopPointId1);
    netexEntitiesTestFactory
      .journeyPattern(345)
      .stopPointInJourneyPattern(sameStopPointId1);

    netexEntitiesTestFactory
      .journeyPattern(567)
      .stopPointInJourneyPattern(sameStopPointId2);
    netexEntitiesTestFactory
      .journeyPattern(789)
      .stopPointInJourneyPattern(sameStopPointId2);

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
      netexEntitiesTestFactory.journeyPattern(123);

    IntStream
      .rangeClosed(1, 10)
      .forEach(i ->
        createJourneyPattern123.stopPointInJourneyPattern(
          stopPointInJourneyPatternId + 1
        )
      );

    NetexEntitiesTestFactory.CreateJourneyPattern createJourneyPattern345 =
      netexEntitiesTestFactory.journeyPattern(345);

    IntStream
      .rangeClosed(1, 10)
      .forEach(i ->
        createJourneyPattern345.stopPointInJourneyPattern(
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
      .journeyPattern(123)
      .stopPointInJourneyPattern(stopPointInJourneyPatternId);
    netexEntitiesTestFactory
      .journeyPattern(345)
      .stopPointInJourneyPattern(stopPointInJourneyPatternId);

    IntStream
      .rangeClosed(1, 8)
      .forEach(i ->
        netexEntitiesTestFactory
          .journeyPattern(i)
          .stopPointInJourneyPattern(stopPointInJourneyPatternId + i)
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
      netexEntitiesTestFactory.journeyPattern(123);

    IntStream
      .rangeClosed(1, 10)
      .forEach(i ->
        createJourneyPattern123
          .stopPointInJourneyPattern(stopPointInJourneyPatternId + 1)
          .withOrder(stopPointsOrder1.get(i - 1))
      );

    NetexEntitiesTestFactory.CreateJourneyPattern createJourneyPattern345 =
      netexEntitiesTestFactory.journeyPattern(345);

    IntStream
      .rangeClosed(1, 10)
      .forEach(i ->
        createJourneyPattern345
          .stopPointInJourneyPattern(stopPointInJourneyPatternId + 1)
          .withOrder(i)
      );

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
  }
}
