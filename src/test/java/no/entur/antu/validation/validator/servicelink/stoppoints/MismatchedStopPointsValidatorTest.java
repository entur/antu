package no.entur.antu.validation.validator.servicelink.stoppoints;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.IntStream;
import no.entur.antu.netextestdata.NetexEntitiesTestFactory;
import no.entur.antu.validation.ValidationTest;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.model.FromToScheduledStopPointId;
import org.entur.netex.validation.validator.model.ScheduledStopPointId;
import org.entur.netex.validation.validator.model.ServiceLinkId;
import org.junit.jupiter.api.Test;

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
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

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
      netexEntitiesTestFactory.create()
    );

    assertTrue(validationReport.getValidationReportEntries().isEmpty());
  }

  @Test
  void testStopPointsInAllServiceLinkMatchesAllJourneyPattern() {
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

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
      netexEntitiesTestFactory.create()
    );

    assertTrue(validationReport.getValidationReportEntries().isEmpty());
  }

  @Test
  void testServiceLinkMissing() {
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

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
      netexEntitiesTestFactory.create()
    );

    // 4 errors, one for each link in journey pattern
    assertEquals(4, validationReport.getValidationReportEntries().size());
  }

  @Test
  void testFromStopPointInServiceLinkDoesNotMatchesJourneyPattern() {
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

    NetexEntitiesTestFactory.CreateJourneyPattern createJourneyPattern =
      netexEntitiesTestFactory.createJourneyPattern();

    createJourneyPattern
      .createStopPointInJourneyPattern(1)
      .withScheduledStopPointRef(
        NetexEntitiesTestFactory.createScheduledStopPointRef(1)
      );

    createJourneyPattern
      .createStopPointInJourneyPattern(2)
      .withScheduledStopPointRef(
        NetexEntitiesTestFactory.createScheduledStopPointRef(2)
      );

    createJourneyPattern
      .createServiceLinkInJourneyPattern(1)
      .withServiceLinkRef(NetexEntitiesTestFactory.createServiceLinkRef(1));

    mockGetFromToScheduledStopPointId(
      new ServiceLinkId("TST:ServiceLink:1"),
      new FromToScheduledStopPointId(
        ScheduledStopPointId.of(
          NetexEntitiesTestFactory.createScheduledStopPointRef(3)
        ),
        ScheduledStopPointId.of(
          NetexEntitiesTestFactory.createScheduledStopPointRef(2)
        )
      )
    );

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.create()
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
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

    NetexEntitiesTestFactory.CreateJourneyPattern createJourneyPattern =
      netexEntitiesTestFactory.createJourneyPattern();

    createJourneyPattern
      .createStopPointInJourneyPattern(1)
      .withScheduledStopPointRef(
        NetexEntitiesTestFactory.createScheduledStopPointRef(1)
      );

    createJourneyPattern
      .createStopPointInJourneyPattern(2)
      .withScheduledStopPointRef(
        NetexEntitiesTestFactory.createScheduledStopPointRef(2)
      );

    createJourneyPattern
      .createServiceLinkInJourneyPattern(1)
      .withServiceLinkRef(NetexEntitiesTestFactory.createServiceLinkRef(1));

    mockGetFromToScheduledStopPointId(
      new ServiceLinkId("TST:ServiceLink:1"),
      new FromToScheduledStopPointId(
        ScheduledStopPointId.of(
          NetexEntitiesTestFactory.createScheduledStopPointRef(1)
        ),
        ScheduledStopPointId.of(
          NetexEntitiesTestFactory.createScheduledStopPointRef(3)
        )
      )
    );

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.create()
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

  private void createJourneyPatterns(
    NetexEntitiesTestFactory netexEntitiesTestFactory,
    int numberOfJourneyPatterns,
    int numberOfStopPointsInJourneyPattern
  ) {
    IntStream
      .rangeClosed(1, numberOfJourneyPatterns)
      .forEach(journeyPatternId -> {
        NetexEntitiesTestFactory.CreateJourneyPattern createJourneyPattern =
          netexEntitiesTestFactory.createJourneyPattern(journeyPatternId);

        IntStream
          .rangeClosed(1, numberOfStopPointsInJourneyPattern)
          .map(stopPointId ->
            Integer.parseInt(journeyPatternId + "" + stopPointId)
          )
          .forEach(stopPointInJourneyPatternId ->
            createJourneyPattern
              .createStopPointInJourneyPattern(stopPointInJourneyPatternId)
              .withScheduledStopPointRef(
                NetexEntitiesTestFactory.createScheduledStopPointRef(
                  stopPointInJourneyPatternId
                )
              )
          );

        IntStream
          // Links are 1 less than stop points
          .rangeClosed(1, numberOfStopPointsInJourneyPattern - 1)
          .map(serviceLinkId ->
            Integer.parseInt(journeyPatternId + "" + serviceLinkId)
          )
          .forEach(serviceLinkInJourneyPatternId ->
            createJourneyPattern
              .createServiceLinkInJourneyPattern(1)
              .withServiceLinkRef(
                NetexEntitiesTestFactory.createServiceLinkRef(
                  serviceLinkInJourneyPatternId
                )
              )
              .create()
          );
      });
  }
}
