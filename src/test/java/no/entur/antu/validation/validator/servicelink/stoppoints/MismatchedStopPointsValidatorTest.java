package no.entur.antu.validation.validator.servicelink.stoppoints;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.IntStream;
import no.entur.antu.netex.test.NetexTestData;
import no.entur.antu.netex.test.ValidatorTestBase;
import no.entur.antu.netex.test.builder.JourneyPatternBuilder;
import no.entur.antu.netex.test.builder.NetexRefs;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.model.FromToScheduledStopPointId;
import org.entur.netex.validation.validator.model.ScheduledStopPointId;
import org.entur.netex.validation.validator.model.ServiceLinkId;
import org.junit.jupiter.api.Test;

class MismatchedStopPointsValidatorTest extends ValidatorTestBase {

  private ValidationReport runValidation(
    NetexEntitiesIndex netexEntitiesIndex
  ) {
    return runValidationOnLineFile(
      netexEntitiesIndex,
      new MismatchedStopPointsValidator()
    );
  }

  @Test
  void testStopPointsInServiceLinkMatchesJourneyPattern() {
    NetexTestData netexEntitiesTestFactory = new NetexTestData();

    int numberOfJourneyPatterns = 1;
    int numberOfStopPointsInJourneyPattern = 2;

    createJourneyPatterns(
      netexEntitiesTestFactory,
      numberOfJourneyPatterns,
      numberOfStopPointsInJourneyPattern
    );

    mockGetFromToScheduledStopPointIdInServiceLink(
      numberOfJourneyPatterns,
      numberOfStopPointsInJourneyPattern
    );

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.build()
    );

    assertTrue(validationReport.getValidationReportEntries().isEmpty());
  }

  @Test
  void testStopPointsInAllServiceLinkMatchesAllJourneyPattern() {
    NetexTestData netexEntitiesTestFactory = new NetexTestData();

    int numberOfJourneyPatterns = 5;
    int numberOfStopPointsInJourneyPattern = 5;

    createJourneyPatterns(
      netexEntitiesTestFactory,
      numberOfJourneyPatterns,
      numberOfStopPointsInJourneyPattern
    );

    mockGetFromToScheduledStopPointIdInServiceLink(
      numberOfJourneyPatterns,
      numberOfStopPointsInJourneyPattern
    );

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.build()
    );

    assertTrue(validationReport.getValidationReportEntries().isEmpty());
  }

  @Test
  void testServiceLinkMissing() {
    NetexTestData netexEntitiesTestFactory = new NetexTestData();

    int numberOfJourneyPatterns = 5;
    int numberOfServiceLinks = 4;
    int numberOfStopPointsInJourneyPattern = 5;

    createJourneyPatterns(
      netexEntitiesTestFactory,
      numberOfJourneyPatterns,
      numberOfStopPointsInJourneyPattern
    );

    mockGetFromToScheduledStopPointIdInServiceLink(
      numberOfServiceLinks,
      numberOfStopPointsInJourneyPattern
    );

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.build()
    );

    // 4 errors, one for each link in journey pattern
    assertEquals(4, validationReport.getValidationReportEntries().size());
  }

  @Test
  void testFromStopPointInServiceLinkDoesNotMatchesJourneyPattern() {
    NetexTestData netexEntitiesTestFactory = new NetexTestData();

    JourneyPatternBuilder journeyPatternBuilder =
      netexEntitiesTestFactory.addJourneyPattern();

    journeyPatternBuilder
      .addStopPoint(1)
      .withScheduledStopPointRef(NetexRefs.scheduledStopPointRef(1));

    journeyPatternBuilder
      .addStopPoint(2)
      .withScheduledStopPointRef(NetexRefs.scheduledStopPointRef(2));

    journeyPatternBuilder
      .addLink(1)
      .withServiceLinkRef(NetexRefs.serviceLinkRef(1));

    withFromToScheduledStopPointId(
      new ServiceLinkId("TST:ServiceLink:1"),
      new FromToScheduledStopPointId(
        ScheduledStopPointId.of(NetexRefs.scheduledStopPointRef(3)),
        ScheduledStopPointId.of(NetexRefs.scheduledStopPointRef(2))
      )
    );

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.build()
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
    NetexTestData netexEntitiesTestFactory = new NetexTestData();

    JourneyPatternBuilder journeyPatternBuilder =
      netexEntitiesTestFactory.addJourneyPattern();

    journeyPatternBuilder
      .addStopPoint(1)
      .withScheduledStopPointRef(NetexRefs.scheduledStopPointRef(1));

    journeyPatternBuilder
      .addStopPoint(2)
      .withScheduledStopPointRef(NetexRefs.scheduledStopPointRef(2));

    journeyPatternBuilder
      .addLink(1)
      .withServiceLinkRef(NetexRefs.serviceLinkRef(1));

    withFromToScheduledStopPointId(
      new ServiceLinkId("TST:ServiceLink:1"),
      new FromToScheduledStopPointId(
        ScheduledStopPointId.of(NetexRefs.scheduledStopPointRef(1)),
        ScheduledStopPointId.of(NetexRefs.scheduledStopPointRef(3))
      )
    );

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.build()
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
            withFromToScheduledStopPointId(
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

  private void createJourneyPatterns(
    NetexTestData netexEntitiesTestFactory,
    int numberOfJourneyPatterns,
    int numberOfStopPointsInJourneyPattern
  ) {
    IntStream
      .rangeClosed(1, numberOfJourneyPatterns)
      .forEach(journeyPatternId -> {
        JourneyPatternBuilder journeyPatternBuilder =
          netexEntitiesTestFactory.addJourneyPattern(journeyPatternId);

        IntStream
          .rangeClosed(1, numberOfStopPointsInJourneyPattern)
          .map(stopPointId ->
            Integer.parseInt(journeyPatternId + "" + stopPointId)
          )
          .forEach(stopPointInJourneyPatternId ->
            journeyPatternBuilder
              .addStopPoint(stopPointInJourneyPatternId)
              .withScheduledStopPointRef(
                NetexRefs.scheduledStopPointRef(stopPointInJourneyPatternId)
              )
          );

        IntStream
          // Links are 1 less than stop points
          .rangeClosed(1, numberOfStopPointsInJourneyPattern - 1)
          .map(serviceLinkId ->
            Integer.parseInt(journeyPatternId + "" + serviceLinkId)
          )
          .forEach(serviceLinkInJourneyPatternId ->
            journeyPatternBuilder
              .addLink(1)
              .withServiceLinkRef(
                NetexRefs.serviceLinkRef(serviceLinkInJourneyPatternId)
              )
              .build()
          );
      });
  }
}
