package no.entur.antu.validation.validator.journeypattern.stoppoint.samestoppoints;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.List;
import java.util.stream.IntStream;
import no.entur.antu.netextestdata.NetexTestFragment;
import no.entur.antu.validation.ValidationTest;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.JourneyPattern;

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
    NetexTestFragment testFragment = new NetexTestFragment();

    NetexTestFragment.CreateNetexEntitiesIndex createNetexEntitiesIndex =
      testFragment.netexEntitiesIndex();

    IntStream
      .rangeClosed(1, 8)
      .forEach(i ->
        createNetexEntitiesIndex.addJourneyPatterns(
          testFragment
            .journeyPattern()
            .withId(i)
            .withStopPointsInJourneyPattern(
              List.of(
                testFragment.stopPointInJourneyPattern(i).withId(i).create()
              )
            )
            .create()
        )
      );

    ValidationReport validationReport = runValidation(
      createNetexEntitiesIndex.create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void testAllJourneyPatternsHaveSameStopPoints() {
    NetexTestFragment testFragment = new NetexTestFragment();
    int sameStopPointId = 987;

    NetexTestFragment.CreateNetexEntitiesIndex createNetexEntitiesIndex =
      testFragment.netexEntitiesIndex();

    IntStream
      .rangeClosed(1, 8)
      .forEach(i ->
        createNetexEntitiesIndex.addJourneyPatterns(
          testFragment
            .journeyPattern()
            .withId(i)
            .withStopPointsInJourneyPattern(
              List.of(
                testFragment
                  .stopPointInJourneyPattern(1)
                  .withId(sameStopPointId)
                  .create()
              )
            )
            .create()
        )
      );

    ValidationReport validationReport = runValidation(
      createNetexEntitiesIndex.create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
  }

  @Test
  void testMultiplePairsOfJourneyPatternsHaveSameStopPoints() {
    NetexTestFragment testFragment = new NetexTestFragment();
    int sameStopPointId1 = 987;
    int sameStopPointId2 = 988;

    JourneyPattern journeyPatternWithSameStopPoints1_1 = testFragment
      .journeyPattern()
      .withId(123)
      .withStopPointsInJourneyPattern(
        List.of(
          testFragment
            .stopPointInJourneyPattern(123)
            .withId(sameStopPointId1)
            .create()
        )
      )
      .create();

    JourneyPattern journeyPatternWithSameStopPoints1_2 = testFragment
      .journeyPattern()
      .withId(345)
      .withStopPointsInJourneyPattern(
        List.of(
          testFragment
            .stopPointInJourneyPattern(123)
            .withId(sameStopPointId1)
            .create()
        )
      )
      .create();

    JourneyPattern journeyPatternWithSameStopPoints2_1 = testFragment
      .journeyPattern()
      .withId(567)
      .withStopPointsInJourneyPattern(
        List.of(
          testFragment
            .stopPointInJourneyPattern(567)
            .withId(sameStopPointId2)
            .create()
        )
      )
      .create();

    JourneyPattern journeyPatternWithSameStopPoints2_2 = testFragment
      .journeyPattern()
      .withId(789)
      .withStopPointsInJourneyPattern(
        List.of(
          testFragment
            .stopPointInJourneyPattern(567)
            .withId(sameStopPointId2)
            .create()
        )
      )
      .create();

    ValidationReport validationReport = runValidation(
      testFragment
        .netexEntitiesIndex()
        .addJourneyPatterns(
          journeyPatternWithSameStopPoints1_1,
          journeyPatternWithSameStopPoints1_2,
          journeyPatternWithSameStopPoints2_1,
          journeyPatternWithSameStopPoints2_2
        )
        .create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(2));
  }

  @Test
  void testAllJourneyPatternsWithMultipleStopPointsHaveSameStopPoints() {
    NetexTestFragment testFragment = new NetexTestFragment();
    int stopPointInJourneyPatternId = 987;
    JourneyPattern journeyPattern1 = testFragment
      .journeyPattern()
      .withId(123)
      .withStopPointsInJourneyPattern(
        IntStream
          .rangeClosed(1, 10)
          .mapToObj(i ->
            testFragment
              .stopPointInJourneyPattern(123)
              .withId(stopPointInJourneyPatternId + 1)
              .create()
          )
          .toList()
      )
      .create();

    JourneyPattern journeyPattern2 = testFragment
      .journeyPattern()
      .withId(345)
      .withStopPointsInJourneyPattern(
        IntStream
          .rangeClosed(1, 10)
          .mapToObj(i ->
            testFragment
              .stopPointInJourneyPattern(123)
              .withId(stopPointInJourneyPatternId + 1)
              .create()
          )
          .toList()
      )
      .create();

    ValidationReport validationReport = runValidation(
      testFragment
        .netexEntitiesIndex()
        .addJourneyPatterns(journeyPattern1, journeyPattern2)
        .create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
  }

  @Test
  void testTwoJourneyPatternsOutOfTenHaveSameStopPoints() {
    NetexTestFragment testFragment = new NetexTestFragment();
    int stopPointInJourneyPatternId = 987;

    NetexTestFragment.CreateNetexEntitiesIndex createNetexEntitiesIndex =
      testFragment.netexEntitiesIndex();

    IntStream
      .rangeClosed(1, 8)
      .forEach(i ->
        createNetexEntitiesIndex.addJourneyPatterns(
          testFragment
            .journeyPattern()
            .withId(i)
            .withStopPointsInJourneyPattern(
              List.of(
                testFragment
                  .stopPointInJourneyPattern(123)
                  .withId(stopPointInJourneyPatternId + i)
                  .create()
              )
            )
            .create()
        )
      );

    JourneyPattern journeyPatternWithSameStopPoints1 = testFragment
      .journeyPattern()
      .withId(123)
      .withStopPointsInJourneyPattern(
        List.of(
          testFragment
            .stopPointInJourneyPattern(123)
            .withId(stopPointInJourneyPatternId)
            .create()
        )
      )
      .create();

    JourneyPattern journeyPatternWithSameStopPoints2 = testFragment
      .journeyPattern()
      .withId(345)
      .withStopPointsInJourneyPattern(
        List.of(
          testFragment
            .stopPointInJourneyPattern(123)
            .withId(stopPointInJourneyPatternId)
            .create()
        )
      )
      .create();

    createNetexEntitiesIndex.addJourneyPatterns(
      journeyPatternWithSameStopPoints1,
      journeyPatternWithSameStopPoints2
    );

    ValidationReport validationReport = runValidation(
      createNetexEntitiesIndex.create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
  }

  @Test
  void testJourneyPatternsWithUnSortedSameStopPoints() {
    NetexTestFragment testFragment = new NetexTestFragment();
    int stopPointInJourneyPatternId = 987;

    List<Integer> stopPointsOrder1 = List.of(8, 3, 5, 2, 4, 10, 9, 1, 7, 6);

    JourneyPattern journeyPattern1 = testFragment
      .journeyPattern()
      .withId(123)
      .withStopPointsInJourneyPattern(
        IntStream
          .rangeClosed(1, 10)
          .mapToObj(i ->
            testFragment
              .stopPointInJourneyPattern(123)
              .withId(stopPointInJourneyPatternId + 1)
              .withOrder(stopPointsOrder1.get(i - 1)) // not in order, as appeared in the xml
              .create()
          )
          .toList()
      )
      .create();

    JourneyPattern journeyPattern2 = testFragment
      .journeyPattern()
      .withId(345)
      .withStopPointsInJourneyPattern(
        IntStream
          .rangeClosed(1, 10)
          .mapToObj(i ->
            testFragment
              .stopPointInJourneyPattern(123)
              .withId(stopPointInJourneyPatternId + 1)
              .withOrder(i) // In correct order, also as appeared in xml
              .create()
          )
          .toList()
      )
      .create();

    ValidationReport validationReport = runValidation(
      testFragment
        .netexEntitiesIndex()
        .addJourneyPatterns(journeyPattern1, journeyPattern2)
        .create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
  }
}
