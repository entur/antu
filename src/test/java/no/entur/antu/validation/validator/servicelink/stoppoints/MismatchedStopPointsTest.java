package no.entur.antu.validation.validator.servicelink.stoppoints;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collector;
import java.util.stream.IntStream;
import no.entur.antu.model.ScheduledStopPointId;
import no.entur.antu.model.ScheduledStopPointIds;
import no.entur.antu.model.ServiceLinkId;
import no.entur.antu.netextestdata.NetexTestFragment;
import no.entur.antu.validation.ValidationTest;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.StopPointInJourneyPattern;

class MismatchedStopPointsTest extends ValidationTest {

  private ValidationReport runValidation(
    NetexEntitiesIndex netexEntitiesIndex
  ) {
    return runValidationOnLineFile(
      netexEntitiesIndex,
      MismatchedStopPoints.class
    );
  }

  @Test
  void testStopPointsInServiceLinkMatchesJourneyPattern() {
    NetexTestFragment fragment = new NetexTestFragment();

    int numberOfJourneyPatterns = 1;
    int numberOfStopPointsInJourneyPattern = 2;

    NetexEntitiesIndex netexEntitiesIndex = createJourneyPatterns(numberOfJourneyPatterns, numberOfStopPointsInJourneyPattern)
      .stream()
      .collect(collectToNetexEntitiesIndex(fragment));

    mockGetScheduledStopPointIdsInServiceLink(numberOfJourneyPatterns, numberOfStopPointsInJourneyPattern);

    ValidationReport validationReport = runValidation(netexEntitiesIndex);

    assertTrue(validationReport.getValidationReportEntries().isEmpty());
  }

  @Test
  void testStopPointsInAllServiceLinkMatchesAllJourneyPattern() {
    NetexTestFragment fragment = new NetexTestFragment();

    int numberOfJourneyPatterns = 5;
    int numberOfStopPointsInJourneyPattern = 5;

    NetexEntitiesIndex netexEntitiesIndex = createJourneyPatterns(numberOfJourneyPatterns, numberOfStopPointsInJourneyPattern)
      .stream()
      .collect(collectToNetexEntitiesIndex(fragment));

    mockGetScheduledStopPointIdsInServiceLink(numberOfJourneyPatterns, numberOfStopPointsInJourneyPattern);

    ValidationReport validationReport = runValidation(netexEntitiesIndex);

    assertTrue(validationReport.getValidationReportEntries().isEmpty());
  }

  @Test
  void testServiceLinkMissing() {
    NetexTestFragment fragment = new NetexTestFragment();

    int numberOfJourneyPatterns = 5;
    int numberOfServiceLinks = 4;
    int numberOfStopPointsInJourneyPattern = 5;

    NetexEntitiesIndex netexEntitiesIndex = createJourneyPatterns(numberOfJourneyPatterns, numberOfStopPointsInJourneyPattern)
      .stream()
      .collect(collectToNetexEntitiesIndex(fragment));

    mockGetScheduledStopPointIdsInServiceLink(numberOfServiceLinks, numberOfStopPointsInJourneyPattern);

    ValidationReport validationReport = runValidation(netexEntitiesIndex);

    // 4 errors, one for each link in journey pattern
    assertEquals(4, validationReport.getValidationReportEntries().size());
  }

  @Test
  void testFromStopPointInServiceLinkDoesNotMatchesJourneyPattern() {
    NetexTestFragment fragment = new NetexTestFragment();

    int journeyPatternId = 1;
    StopPointInJourneyPattern fromStopPointInJourneyPattern = fragment
      .stopPointInJourneyPattern(journeyPatternId)
      .withScheduledStopPointId(1)
      .create();

    StopPointInJourneyPattern fromStopPointInServiceLink = fragment
      .stopPointInJourneyPattern(journeyPatternId)
      .withScheduledStopPointId(3)
      .create();

    StopPointInJourneyPattern toStopPoint = fragment
      .stopPointInJourneyPattern(journeyPatternId)
      .withScheduledStopPointId(2)
      .create();

    JourneyPattern journeyPattern = fragment
      .journeyPattern()
      .withId(journeyPatternId)
      .withStopPointsInJourneyPattern(
        List.of(fromStopPointInJourneyPattern, toStopPoint)
      )
      .withServiceLinksInJourneyPattern(
        List.of(
          fragment
            .linkInJourneyPattern(journeyPatternId)
            .withServiceLinkId(1)
            .create()
        )
      )
      .create();

    mockGetScheduledStopPointIds(
      new ServiceLinkId("TST:ServiceLink:1"),
      new ScheduledStopPointIds(
        ScheduledStopPointId.of(fromStopPointInServiceLink),
        ScheduledStopPointId.of(toStopPoint)
      )
    );

    ValidationReport validationReport = runValidation(
      fragment.netexEntitiesIndex().addJourneyPatterns(journeyPattern).create()
    );

    assertEquals(1, validationReport.getValidationReportEntries().size());
    assertTrue(
      validationReport
        .getValidationReportEntries()
        .stream()
        .allMatch(entry ->
          entry
            .getName()
            .equals(
              MismatchedStopPointsError.RuleCode.STOP_POINTS_IN_SERVICE_LINK_DOES_NOT_MATCH_THE_JOURNEY_PATTERN.name()
            )
        )
    );
  }

  @Test
  void testToStopPointInServiceLinkDoesNotMatchesJourneyPattern() {
    NetexTestFragment fragment = new NetexTestFragment();

    int journeyPatternId = 1;
    StopPointInJourneyPattern fromStopPoint = fragment
      .stopPointInJourneyPattern(journeyPatternId)
      .withScheduledStopPointId(1)
      .create();

    StopPointInJourneyPattern toStopPointInJourneyPattern = fragment
      .stopPointInJourneyPattern(journeyPatternId)
      .withScheduledStopPointId(2)
      .create();

    StopPointInJourneyPattern toStopPointInServiceLink = fragment
      .stopPointInJourneyPattern(journeyPatternId)
      .withScheduledStopPointId(3)
      .create();

    JourneyPattern journeyPattern = fragment
      .journeyPattern()
      .withId(journeyPatternId)
      .withStopPointsInJourneyPattern(
        List.of(fromStopPoint, toStopPointInJourneyPattern)
      )
      .withServiceLinksInJourneyPattern(
        List.of(
          fragment
            .linkInJourneyPattern(journeyPatternId)
            .withServiceLinkId(1)
            .create()
        )
      )
      .create();

    mockGetScheduledStopPointIds(
      new ServiceLinkId("TST:ServiceLink:1"),
      new ScheduledStopPointIds(
        ScheduledStopPointId.of(fromStopPoint),
        ScheduledStopPointId.of(toStopPointInServiceLink)
      )
    );

    ValidationReport validationReport = runValidation(
      fragment.netexEntitiesIndex().addJourneyPatterns(journeyPattern).create()
    );

    assertEquals(1, validationReport.getValidationReportEntries().size());
    assertTrue(
      validationReport
        .getValidationReportEntries()
        .stream()
        .allMatch(entry ->
          entry
            .getName()
            .equals(
              MismatchedStopPointsError.RuleCode.STOP_POINTS_IN_SERVICE_LINK_DOES_NOT_MATCH_THE_JOURNEY_PATTERN.name()
            )
        )
    );
  }

  private static @NotNull Collector<JourneyPattern, NetexTestFragment.CreateNetexEntitiesIndex, NetexEntitiesIndex> collectToNetexEntitiesIndex(
    NetexTestFragment fragment
  ) {
    return Collector.of(
      fragment::netexEntitiesIndex,
      NetexTestFragment.CreateNetexEntitiesIndex::addJourneyPatterns,
      (createLeft, createRight) -> createLeft,
      NetexTestFragment.CreateNetexEntitiesIndex::create
    );
  }

  private void mockGetScheduledStopPointIdsInServiceLink(
    int numberOfServiceLinks,
    int numberOfStopPointsInJourneyPattern
  ) {
    IntStream
      .rangeClosed(1, numberOfServiceLinks)
      .forEach(journeyPatternId ->
        IntStream
          .rangeClosed(1, numberOfStopPointsInJourneyPattern - 1)
          .forEach(stopPointId ->
            mockGetScheduledStopPointIds(
              new ServiceLinkId("TST:ServiceLink:" + journeyPatternId + stopPointId),
              new ScheduledStopPointIds(
                new ScheduledStopPointId(
                  "TST:ScheduledStopPoint:" + journeyPatternId + stopPointId
                ),
                new ScheduledStopPointId(
                  "TST:ScheduledStopPoint:" +
                  journeyPatternId +
                  (stopPointId + 1)
                )
              )
            )
          )
      );
  }

  private List<JourneyPattern> createJourneyPatterns(
    int numberOfJourneyPatterns,
    int numberOfStopPointsInJourneyPattern
  ) {
    NetexTestFragment fragment = new NetexTestFragment();

    BiFunction<Integer, Integer, StopPointInJourneyPattern> stopPoint = (
        journeyPatternId,
        stoPointId
      ) ->
      fragment
        .stopPointInJourneyPattern(journeyPatternId)
        .withScheduledStopPointId(
          Integer.parseInt(journeyPatternId + "" + stoPointId)
        )
        .create();

    return IntStream
      .rangeClosed(1, numberOfJourneyPatterns)
      .mapToObj(journeyPatternId ->
        fragment
          .journeyPattern()
          .withId(journeyPatternId)
          .withStopPointsInJourneyPattern(
            IntStream
              .rangeClosed(1, numberOfStopPointsInJourneyPattern)
              .mapToObj(stopPointInJourneyPatternId ->
                stopPoint.apply(journeyPatternId, stopPointInJourneyPatternId)
              )
              .toList()
          )
          .withServiceLinksInJourneyPattern(
            IntStream
              // Links are 1 less than stop points
              .rangeClosed(1, numberOfStopPointsInJourneyPattern - 1)
              .mapToObj(serviceLinkId ->
                fragment
                  .linkInJourneyPattern(journeyPatternId)
                  .withServiceLinkId(Integer.parseInt(journeyPatternId + "" + serviceLinkId))
                  .create()
              )
              .toList()
          )
          .create()
      )
      .toList();
  }
}
