package no.entur.antu.validation.validator.interchange.distance;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.stream.IntStream;
import no.entur.antu.netex.test.NetexTestData;
import no.entur.antu.netex.test.ValidatorTestBase;
import no.entur.antu.netex.test.builder.NetexRefs;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.entur.netex.validation.validator.model.QuayCoordinates;
import org.entur.netex.validation.validator.model.QuayId;
import org.entur.netex.validation.validator.model.ScheduledStopPointId;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.ScheduledStopPointRefStructure;

class UnexpectedInterchangeDistanceValidatorTest extends ValidatorTestBase {

  private ValidationReport runValidation(
    NetexEntitiesIndex netexEntitiesIndex
  ) {
    return runValidationOnLineFile(
      netexEntitiesIndex,
      new UnexpectedInterchangeDistanceValidator()
    );
  }

  @Test
  void interchangeWithDistanceBetweenStopPointsWithinLimits() {
    ValidationReport validationReport = runTestWithCoordinates(
      new QuayCoordinates(10.7522, 59.9139),
      new QuayCoordinates(10.7487, 59.9127)
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void multipleInterchangesWithDistanceBetweenStopPointsWithinLimits() {
    ValidationReport validationReport = runTestWithCoordinates(
      new QuayCoordinates(10.7522, 59.9139),
      new QuayCoordinates(10.7585, 59.9172),
      new QuayCoordinates(10.7530, 59.9210),
      new QuayCoordinates(10.7490, 59.9150),
      new QuayCoordinates(10.7450, 59.9200),
      new QuayCoordinates(10.7545, 59.9145)
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void interchangeWithDistanceBetweenStopPointsAboveWarningLimit() {
    ValidationReport validationReport = runTestWithCoordinates(
      new QuayCoordinates(10.7333, 59.9170),
      new QuayCoordinates(10.7480, 59.9260)
    );

    Collection<ValidationReportEntry> validationReportEntries =
      validationReport.getValidationReportEntries();
    assertThat(validationReportEntries.size(), is(1));
    validationReportEntries.forEach(entry ->
      assertThat(
        entry.getName(),
        is(UnexpectedInterchangeDistanceValidator.RULE_WARN_LIMIT.name())
      )
    );
  }

  @Test
  void multipleInterchangesWithDistanceBetweenStopPointsAboveWarningLimit() {
    ValidationReport validationReport = runTestWithCoordinates(
      new QuayCoordinates(10.7522, 59.9139),
      new QuayCoordinates(10.7480, 59.9260),
      new QuayCoordinates(10.7600, 59.9360),
      new QuayCoordinates(10.7700, 59.9100),
      new QuayCoordinates(10.7200, 59.9300),
      new QuayCoordinates(10.7350, 59.9200)
    );

    Collection<ValidationReportEntry> validationReportEntries =
      validationReport.getValidationReportEntries();
    assertThat(validationReportEntries.size(), is(3));
    assertTrue(
      validationReportEntries
        .stream()
        .allMatch(entry ->
          entry
            .getName()
            .equals(
              UnexpectedInterchangeDistanceValidator.RULE_WARN_LIMIT.name()
            )
        )
    );
  }

  @Test
  void interchangeWithDistanceBetweenStopPointsAboveMaximumLimit() {
    ValidationReport validationReport = runTestWithCoordinates(
      new QuayCoordinates(10.7530, 59.9074),
      new QuayCoordinates(10.6997, 59.9260)
    );

    Collection<ValidationReportEntry> validationReportEntries =
      validationReport.getValidationReportEntries();
    assertThat(validationReportEntries.size(), is(1));
    validationReportEntries.forEach(entry ->
      assertThat(
        entry.getName(),
        is(UnexpectedInterchangeDistanceValidator.RULE_MAX_LIMIT.name())
      )
    );
  }

  @Test
  void multipleInterchangesWithDistanceBetweenStopPointsAboveMaximumLimit() {
    ValidationReport validationReport = runTestWithCoordinates(
      new QuayCoordinates(10.7522, 59.9139),
      new QuayCoordinates(10.7191, 59.9420),
      new QuayCoordinates(10.7616, 59.9020),
      new QuayCoordinates(10.7975, 59.9300),
      new QuayCoordinates(10.7545, 59.8800),
      new QuayCoordinates(10.6750, 59.9600)
    );

    Collection<ValidationReportEntry> validationReportEntries =
      validationReport.getValidationReportEntries();
    assertThat(validationReportEntries.size(), is(3));
    assertTrue(
      validationReportEntries
        .stream()
        .allMatch(entry ->
          entry
            .getName()
            .equals(
              UnexpectedInterchangeDistanceValidator.RULE_MAX_LIMIT.name()
            )
        )
    );
  }

  @Test
  void multipleInterchangesWithMultipleProblems() {
    ValidationReport validationReport = runTestWithCoordinates(
      new QuayCoordinates(10.7522, 59.9139),
      new QuayCoordinates(10.7550, 59.9200),
      new QuayCoordinates(10.7340, 59.9100),
      new QuayCoordinates(10.7550, 59.9300),
      new QuayCoordinates(10.7000, 59.9000),
      new QuayCoordinates(10.8000, 59.9400)
    );

    Collection<ValidationReportEntry> validationReportEntries =
      validationReport.getValidationReportEntries();
    assertThat(validationReportEntries.size(), is(2));
  }

  private ValidationReport runTestWithCoordinates(
    QuayCoordinates... coordinates
  ) {
    assert coordinates.length % 2 == 0;

    NetexTestData netexEntitiesTestFactory = new NetexTestData();

    IntStream
      .rangeClosed(1, coordinates.length / 2)
      .forEach(i -> {
        int idx1 = (i - 1) * 2;
        int idx2 = (i - 1) * 2 + 1;

        QuayCoordinates fromCoordinates = coordinates[idx1];
        QuayCoordinates toCoordinates = coordinates[idx2];

        ScheduledStopPointRefStructure fromPointRef =
          NetexRefs.scheduledStopPointRef(idx1);
        ScheduledStopPointRefStructure toPointRef =
          NetexRefs.scheduledStopPointRef(idx2);

        withCoordinates(
          ScheduledStopPointId.of(fromPointRef),
          new QuayId("TST:Quay:" + idx1),
          fromCoordinates
        );

        withCoordinates(
          ScheduledStopPointId.of(toPointRef),
          new QuayId("TST:Quay:" + idx2),
          toCoordinates
        );

        netexEntitiesTestFactory
          .addServiceJourneyInterchange(i)
          .withFromPointRef(fromPointRef)
          .withToPointRef(toPointRef)
          .withFromJourneyRef(NetexRefs.serviceJourneyRef(idx1))
          .withToJourneyRef(NetexRefs.serviceJourneyRef(idx2));
      });

    return runValidation(netexEntitiesTestFactory.build());
  }
}
