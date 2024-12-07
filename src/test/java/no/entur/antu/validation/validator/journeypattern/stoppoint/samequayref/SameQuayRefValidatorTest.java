package no.entur.antu.validation.validator.journeypattern.stoppoint.samequayref;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import no.entur.antu.netextestdata.NetexEntitiesTestFactory;
import no.entur.antu.validation.ValidationTest;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.model.QuayId;
import org.entur.netex.validation.validator.model.ScheduledStopPointId;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.ScheduledStopPointRefStructure;

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
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

    NetexEntitiesTestFactory.CreateJourneyPattern createJourneyPattern =
      netexEntitiesTestFactory.createJourneyPattern();

    createJourneyPattern.createStopPointsInJourneyPattern(0);

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void testNoSameQuayRefOnStopPoints() {
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

    NetexEntitiesTestFactory.CreateJourneyPattern createJourneyPattern =
      netexEntitiesTestFactory.createJourneyPattern();

    ScheduledStopPointRefStructure scheduledStopPointRef1 =
      NetexEntitiesTestFactory.createScheduledStopPointRef(1);
    ScheduledStopPointRefStructure scheduledStopPointRef2 =
      NetexEntitiesTestFactory.createScheduledStopPointRef(2);

    createJourneyPattern
      .createStopPointInJourneyPattern(1)
      .withScheduledStopPointRef(scheduledStopPointRef1);

    createJourneyPattern
      .createStopPointInJourneyPattern(2)
      .withScheduledStopPointRef(scheduledStopPointRef2);

    mockGetQuayId(
      ScheduledStopPointId.of(scheduledStopPointRef1),
      new QuayId("TST:Quay:1")
    );

    mockGetQuayId(
      ScheduledStopPointId.of(scheduledStopPointRef2),
      new QuayId("TST:Quay:2")
    );

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void testSameQuayRefOnStopPoints() {
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

    NetexEntitiesTestFactory.CreateJourneyPattern createJourneyPattern =
      netexEntitiesTestFactory.createJourneyPattern();

    ScheduledStopPointRefStructure scheduledStopPointRef1 =
      NetexEntitiesTestFactory.createScheduledStopPointRef(1);
    ScheduledStopPointRefStructure scheduledStopPointRef2 =
      NetexEntitiesTestFactory.createScheduledStopPointRef(2);

    createJourneyPattern
      .createStopPointInJourneyPattern(1)
      .withScheduledStopPointRef(scheduledStopPointRef1);

    createJourneyPattern
      .createStopPointInJourneyPattern(2)
      .withScheduledStopPointRef(scheduledStopPointRef2);

    QuayId testQuayId1 = new QuayId("TST:Quay:1");

    mockGetQuayId(ScheduledStopPointId.of(scheduledStopPointRef1), testQuayId1);
    mockGetQuayId(ScheduledStopPointId.of(scheduledStopPointRef2), testQuayId1);

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
  }
}
