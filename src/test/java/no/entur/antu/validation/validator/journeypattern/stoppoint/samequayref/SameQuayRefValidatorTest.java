package no.entur.antu.validation.validator.journeypattern.stoppoint.samequayref;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import no.entur.antu.netex.test.NetexTestData;
import no.entur.antu.netex.test.ValidatorTestBase;
import no.entur.antu.netex.test.builder.JourneyPatternBuilder;
import no.entur.antu.netex.test.builder.NetexRefs;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.model.QuayId;
import org.entur.netex.validation.validator.model.ScheduledStopPointId;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.ScheduledStopPointRefStructure;

class SameQuayRefValidatorTest extends ValidatorTestBase {

  private ValidationReport runValidation(
    NetexEntitiesIndex netexEntitiesIndex
  ) {
    return runValidationOnLineFile(
      netexEntitiesIndex,
      new SameQuayRefValidator()
    );
  }

  @Test
  void testNoStopPointsInJourneyPattern() {
    NetexTestData netexEntitiesTestFactory = new NetexTestData();

    JourneyPatternBuilder createJourneyPattern =
      netexEntitiesTestFactory.addJourneyPattern();

    createJourneyPattern.addStopPoints(0);

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.build()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void testNoSameQuayRefOnStopPoints() {
    NetexTestData netexEntitiesTestFactory = new NetexTestData();

    JourneyPatternBuilder createJourneyPattern =
      netexEntitiesTestFactory.addJourneyPattern();

    ScheduledStopPointRefStructure scheduledStopPointRef1 =
      NetexRefs.scheduledStopPointRef(1);
    ScheduledStopPointRefStructure scheduledStopPointRef2 =
      NetexRefs.scheduledStopPointRef(2);

    createJourneyPattern
      .addStopPoint(1)
      .withScheduledStopPointRef(scheduledStopPointRef1);

    createJourneyPattern
      .addStopPoint(2)
      .withScheduledStopPointRef(scheduledStopPointRef2);

    withQuayId(
      ScheduledStopPointId.of(scheduledStopPointRef1),
      new QuayId("TST:Quay:1")
    );

    withQuayId(
      ScheduledStopPointId.of(scheduledStopPointRef2),
      new QuayId("TST:Quay:2")
    );

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.build()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void testSameQuayRefOnStopPoints() {
    NetexTestData netexEntitiesTestFactory = new NetexTestData();

    JourneyPatternBuilder createJourneyPattern =
      netexEntitiesTestFactory.addJourneyPattern();

    ScheduledStopPointRefStructure scheduledStopPointRef1 =
      NetexRefs.scheduledStopPointRef(1);
    ScheduledStopPointRefStructure scheduledStopPointRef2 =
      NetexRefs.scheduledStopPointRef(2);

    createJourneyPattern
      .addStopPoint(1)
      .withScheduledStopPointRef(scheduledStopPointRef1);

    createJourneyPattern
      .addStopPoint(2)
      .withScheduledStopPointRef(scheduledStopPointRef2);

    QuayId testQuayId1 = new QuayId("TST:Quay:1");

    withQuayId(ScheduledStopPointId.of(scheduledStopPointRef1), testQuayId1);
    withQuayId(ScheduledStopPointId.of(scheduledStopPointRef2), testQuayId1);

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.build()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
  }
}
