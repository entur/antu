package no.entur.antu.validation.validator.interchange.distance;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;
import no.entur.antu.netextestdata.NetexEntitiesTestFactory;
import no.entur.antu.validation.ValidationTest;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.entur.netex.validation.validator.model.QuayCoordinates;
import org.entur.netex.validation.validator.model.QuayId;
import org.entur.netex.validation.validator.model.ScheduledStopPointId;
import org.entur.netex.validation.validator.model.ServiceJourneyId;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.ServiceJourneyInterchange;

class UnexpectedInterchangeDistanceValidatorTest extends ValidationTest {

  private ValidationReport runValidation(
    NetexEntitiesIndex netexEntitiesIndex
  ) {
    return runValidationOnLineFile(
      netexEntitiesIndex,
      UnexpectedInterchangeDistanceValidator.class
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
        is(
          UnexpectedInterchangeDistanceError.RuleCode.DISTANCE_BETWEEN_STOP_POINTS_IN_INTERCHANGE_IS_MORE_THAN_WARNING_LIMIT.name()
        )
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
              UnexpectedInterchangeDistanceError.RuleCode.DISTANCE_BETWEEN_STOP_POINTS_IN_INTERCHANGE_IS_MORE_THAN_WARNING_LIMIT.name()
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
        is(
          UnexpectedInterchangeDistanceError.RuleCode.DISTANCE_BETWEEN_STOP_POINTS_IN_INTERCHANGE_IS_MORE_THAN_MAX_LIMIT.name()
        )
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
              UnexpectedInterchangeDistanceError.RuleCode.DISTANCE_BETWEEN_STOP_POINTS_IN_INTERCHANGE_IS_MORE_THAN_MAX_LIMIT.name()
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

    NetexEntitiesTestFactory fragment = new NetexEntitiesTestFactory();

    List<ServiceJourneyInterchange> serviceJourneyInterchanges = IntStream
      .rangeClosed(1, coordinates.length / 2)
      .mapToObj(i -> {
        int idx1 = (i - 1) * 2;
        int idx2 = (i - 1) * 2 + 1;

        QuayCoordinates fromCoordinates = coordinates[idx1];
        QuayCoordinates toCoordinates = coordinates[idx2];

        ScheduledStopPointId fromPointRef = new ScheduledStopPointId(
          "TST:ScheduledStopPoint:" + idx1
        );
        ScheduledStopPointId toPointRef = new ScheduledStopPointId(
          "TST:ScheduledStopPoint:" + idx2
        );

        mockGetCoordinates(
          fromPointRef,
          new QuayId("TST:Quay:" + idx1),
          fromCoordinates
        );
        mockGetCoordinates(
          toPointRef,
          new QuayId("TST:Quay:" + idx2),
          toCoordinates
        );

        return fragment
          .serviceJourneyInterchange()
          .withId(i)
          .withFromPointRef(fromPointRef)
          .withToPointRef(toPointRef)
          .withFromJourneyRef(
            ServiceJourneyId.ofValidId("TST:ServiceJourney:" + idx1)
          )
          .withToJourneyRef(
            ServiceJourneyId.ofValidId("TST:ServiceJourney:" + idx2)
          )
          .create();
      })
      .toList();

    return runValidation(
      fragment
        .netexEntitiesIndex()
        .addInterchanges(
          serviceJourneyInterchanges.toArray(ServiceJourneyInterchange[]::new)
        )
        .create()
    );
  }
}
