package no.entur.antu.validation.validator.journeypattern.stoppoint.samequayref;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.List;
import no.entur.antu.netextestdata.NetexEntitiesTestFactory;
import no.entur.antu.validation.ValidationTest;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.model.QuayId;
import org.entur.netex.validation.validator.model.ScheduledStopPointId;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.JourneyPattern;

class SameQuayRefValidatorTest extends ValidationTest {

  private ValidationReport runValidation(
    NetexEntitiesIndex netexEntitiesIndex
  ) {
    return runValidationOnLineFile(
      netexEntitiesIndex,
      SameQuayRefValidator.class
    );
  }

  @Test
  void testNoStopPointsInJourneyPattern() {
    NetexEntitiesTestFactory testFragment = new NetexEntitiesTestFactory();

    JourneyPattern journeyPattern = testFragment
      .journeyPattern()
      .withId(1)
      .withNumberOfStopPointInJourneyPattern(0)
      .create();

    ValidationReport validationReport = runValidation(
      testFragment
        .netexEntitiesIndex()
        .addJourneyPatterns(journeyPattern)
        .create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void testNoSameQuayRefOnStopPoints() {
    NetexEntitiesTestFactory testFragment = new NetexEntitiesTestFactory();

    JourneyPattern journeyPattern = testFragment
      .journeyPattern()
      .withId(1)
      .withStopPointsInJourneyPattern(
        List.of(
          testFragment
            .stopPointInJourneyPattern(1)
            .withId(1)
            .withScheduledStopPointId(1)
            .create(),
          testFragment
            .stopPointInJourneyPattern(1)
            .withId(2)
            .withScheduledStopPointId(2)
            .create()
        )
      )
      .create();

    mockGetQuayId(
      new ScheduledStopPointId("TST:ScheduledStopPoint:1"),
      new QuayId("TST:Quay:1")
    );

    mockGetQuayId(
      new ScheduledStopPointId("TST:ScheduledStopPoint:2"),
      new QuayId("TST:Quay:2")
    );

    ValidationReport validationReport = runValidation(
      testFragment
        .netexEntitiesIndex()
        .addJourneyPatterns(journeyPattern)
        .create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void testSameQuayRefOnStopPoints() {
    NetexEntitiesTestFactory testFragment = new NetexEntitiesTestFactory();

    JourneyPattern journeyPattern = testFragment
      .journeyPattern()
      .withId(1)
      .withStopPointsInJourneyPattern(
        List.of(
          testFragment
            .stopPointInJourneyPattern(1)
            .withId(1)
            .withScheduledStopPointId(1)
            .create(),
          testFragment
            .stopPointInJourneyPattern(1)
            .withId(2)
            .withScheduledStopPointId(2)
            .create()
        )
      )
      .create();

    QuayId testQuayId1 = new QuayId("TST:Quay:1");

    mockGetQuayId(
      new ScheduledStopPointId("TST:ScheduledStopPoint:1"),
      testQuayId1
    );
    mockGetQuayId(
      new ScheduledStopPointId("TST:ScheduledStopPoint:2"),
      testQuayId1
    );

    ValidationReport validationReport = runValidation(
      testFragment
        .netexEntitiesIndex()
        .addJourneyPatterns(journeyPattern)
        .create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
  }
}
