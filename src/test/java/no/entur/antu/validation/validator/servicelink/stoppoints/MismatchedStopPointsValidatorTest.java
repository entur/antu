package no.entur.antu.validation.validator.servicelink.stoppoints;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collector;
import java.util.stream.IntStream;
import no.entur.antu.netextestdata.NetexEntitiesTestFactory;
import no.entur.antu.validation.ValidationTest;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.model.FromToScheduledStopPointId;
import org.entur.netex.validation.validator.model.ScheduledStopPointId;
import org.entur.netex.validation.validator.model.ServiceLinkId;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.StopPointInJourneyPattern;

class MismatchedStopPointsValidatorTest extends ValidationTest {

  private ValidationReport runValidation(
    NetexEntitiesIndex netexEntitiesIndex
  ) {
    return runValidationOnLineFile(
      netexEntitiesIndex,
      MismatchedStopPointsValidator.class
    );
  }

  @Test
  void testStopPointsInServiceLinkMatchesJourneyPattern() {
    NetexEntitiesTestFactory fragment = new NetexEntitiesTestFactory();

    int numberOfJourneyPatterns = 1;
    int numberOfStopPointsInJourneyPattern = 2;

    NetexEntitiesIndex netexEntitiesIndex = createJourneyPatterns(
      numberOfJourneyPatterns,
      numberOfStopPointsInJourneyPattern
    )
      .stream()
      .collect(collectToNetexEntitiesIndex(fragment));

    mockGetFromToScheduledStopPointIdInServiceLink(
      numberOfJourneyPatterns,
      numberOfStopPointsInJourneyPattern
    );

    ValidationReport validationReport = runValidation(netexEntitiesIndex);

    assertTrue(validationReport.getValidationReportEntries().isEmpty());
  }

  @Test
  void testStopPointsInAllServiceLinkMatchesAllJourneyPattern() {
    NetexEntitiesTestFactory fragment = new NetexEntitiesTestFactory();

    int numberOfJourneyPatterns = 5;
    int numberOfStopPointsInJourneyPattern = 5;

    NetexEntitiesIndex netexEntitiesIndex = createJourneyPatterns(
      numberOfJourneyPatterns,
      numberOfStopPointsInJourneyPattern
    )
      .stream()
      .collect(collectToNetexEntitiesIndex(fragment));

    mockGetFromToScheduledStopPointIdInServiceLink(
      numberOfJourneyPatterns,
      numberOfStopPointsInJourneyPattern
    );

    ValidationReport validationReport = runValidation(netexEntitiesIndex);

    assertTrue(validationReport.getValidationReportEntries().isEmpty());
  }

  @Test
  void testServiceLinkMissing() {
    NetexEntitiesTestFactory fragment = new NetexEntitiesTestFactory();

    int numberOfJourneyPatterns = 5;
    int numberOfServiceLinks = 4;
    int numberOfStopPointsInJourneyPattern = 5;

    NetexEntitiesIndex netexEntitiesIndex = createJourneyPatterns(
      numberOfJourneyPatterns,
      numberOfStopPointsInJourneyPattern
    )
      .stream()
      .collect(collectToNetexEntitiesIndex(fragment));

    mockGetFromToScheduledStopPointIdInServiceLink(
      numberOfServiceLinks,
      numberOfStopPointsInJourneyPattern
    );

    ValidationReport validationReport = runValidation(netexEntitiesIndex);

    // 4 errors, one for each link in journey pattern
    assertEquals(4, validationReport.getValidationReportEntries().size());
  }

  @Test
  void testFromStopPointInServiceLinkDoesNotMatchesJourneyPattern() {
    NetexEntitiesTestFactory fragment = new NetexEntitiesTestFactory();

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

    mockGetFromToScheduledStopPointId(
      new ServiceLinkId("TST:ServiceLink:1"),
      new FromToScheduledStopPointId(
        ScheduledStopPointId.of(fromStopPointInServiceLink),
        ScheduledStopPointId.of(toStopPoint)
      )
    );

    ValidationReport validationReport = runValidation(
      fragment.netexEntitiesIndex().addJourneyPatterns(journeyPattern).create()
    );

    assertEquals(1, validationReport.getValidationReportEntries().size());
    assertEquals(
      MismatchedStopPointsValidator.RULE.name(),
      validationReport
        .getValidationReportEntries()
        .stream()
        .findFirst()
        .orElseThrow()
        .getName()
    );
  }

  @Test
  void testToStopPointInServiceLinkDoesNotMatchesJourneyPattern() {
    NetexEntitiesTestFactory fragment = new NetexEntitiesTestFactory();

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

    mockGetFromToScheduledStopPointId(
      new ServiceLinkId("TST:ServiceLink:1"),
      new FromToScheduledStopPointId(
        ScheduledStopPointId.of(fromStopPoint),
        ScheduledStopPointId.of(toStopPointInServiceLink)
      )
    );

    ValidationReport validationReport = runValidation(
      fragment.netexEntitiesIndex().addJourneyPatterns(journeyPattern).create()
    );

    assertEquals(1, validationReport.getValidationReportEntries().size());
    assertEquals(
      MismatchedStopPointsValidator.RULE.name(),
      validationReport
        .getValidationReportEntries()
        .stream()
        .findFirst()
        .orElseThrow()
        .getName()
    );
  }

  private static @NotNull Collector<JourneyPattern, NetexEntitiesTestFactory.CreateNetexEntitiesIndex, NetexEntitiesIndex> collectToNetexEntitiesIndex(
    NetexEntitiesTestFactory fragment
  ) {
    return Collector.of(
      fragment::netexEntitiesIndex,
      NetexEntitiesTestFactory.CreateNetexEntitiesIndex::addJourneyPatterns,
      (createLeft, createRight) -> createLeft,
      NetexEntitiesTestFactory.CreateNetexEntitiesIndex::create
    );
  }

  private void mockGetFromToScheduledStopPointIdInServiceLink(
    int numberOfServiceLinks,
    int numberOfStopPointsInJourneyPattern
  ) {
    IntStream
      .rangeClosed(1, numberOfServiceLinks)
      .forEach(journeyPatternId ->
        IntStream
          .rangeClosed(1, numberOfStopPointsInJourneyPattern - 1)
          .forEach(stopPointId ->
            mockGetFromToScheduledStopPointId(
              new ServiceLinkId(
                "TST:ServiceLink:" + journeyPatternId + stopPointId
              ),
              new FromToScheduledStopPointId(
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
    NetexEntitiesTestFactory fragment = new NetexEntitiesTestFactory();

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
                  .withServiceLinkId(
                    Integer.parseInt(journeyPatternId + "" + serviceLinkId)
                  )
                  .create()
              )
              .toList()
          )
          .create()
      )
      .toList();
  }
}
