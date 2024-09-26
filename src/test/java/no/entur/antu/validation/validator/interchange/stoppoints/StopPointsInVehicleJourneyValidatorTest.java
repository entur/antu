package no.entur.antu.validation.validator.interchange.stoppoints;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Collection;
import java.util.List;
import no.entur.antu.netextestdata.NetexTestFragment;
import no.entur.antu.validation.ValidationTest;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.entur.netex.validation.validator.model.ScheduledStopPointId;
import org.entur.netex.validation.validator.model.ServiceJourneyId;
import org.entur.netex.validation.validator.model.ServiceJourneyStop;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.ServiceJourneyInterchange;

class StopPointsInVehicleJourneyValidatorTest extends ValidationTest {

  private ValidationReport runValidation(
    NetexEntitiesIndex netexEntitiesIndex
  ) {
    return runValidationOnLineFile(
      netexEntitiesIndex,
      StopPointsInVehicleJourneyValidator.class
    );
  }

  @Test
  void interchangeStopPointArePartOfVehicleJourneys() {
    // Mocking both the fromPointRef and toPointRef,
    // to test that both fromPointRef and toPointRef are part of the vehicle journey.
    mockGetServiceJourneyStops(
      ServiceJourneyId.ofValidId("TST:ServiceJourney:1"),
      List.of(
        new ServiceJourneyStop(
          new ScheduledStopPointId("TST:ScheduledStopPoint:1"),
          null,
          null,
          0,
          0
        )
      )
    );
    mockGetServiceJourneyStops(
      ServiceJourneyId.ofValidId("TST:ServiceJourney:2"),
      List.of(
        new ServiceJourneyStop(
          new ScheduledStopPointId("TST:ScheduledStopPoint:2"),
          null,
          null,
          0,
          0
        )
      )
    );

    ValidationReport validationReport = runTestFor(1, 2, 1, 2);

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void interchangeFromStopPointIsNotAPartOfVehicleJourneys() {
    // Mocking only the toPointRef, to test that the fromPointRef is not part of the vehicle journey
    mockGetServiceJourneyStops(
      ServiceJourneyId.ofValidId("TST:ServiceJourney:2"),
      List.of(
        new ServiceJourneyStop(
          new ScheduledStopPointId("TST:ScheduledStopPoint:2"),
          null,
          null,
          0,
          0
        )
      )
    );

    ValidationReport validationReport = runTestFor(1, 2, 1, 2);

    Collection<ValidationReportEntry> validationReportEntries =
      validationReport.getValidationReportEntries();
    assertThat(validationReportEntries.size(), is(1));
    validationReportEntries.forEach(entry ->
      assertThat(
        entry.getName(),
        is(
          StopPointsInVehicleJourneyError.RuleCode.FROM_POINT_REF_IN_INTERCHANGE_IS_NOT_PART_OF_FROM_JOURNEY_REF.name()
        )
      )
    );
  }

  @Test
  void noInterchangeNoEffect() {
    NetexTestFragment fragment = new NetexTestFragment();
    ValidationReport validationReport = runValidation(
      fragment.netexEntitiesIndex().create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void interchangeWithMissingAttributes() {
    NetexTestFragment fragment = new NetexTestFragment();
    ServiceJourneyInterchange serviceJourneyInterchange = fragment
      .serviceJourneyInterchange()
      .create();

    ValidationReport validationReport = runValidation(
      fragment
        .netexEntitiesIndex()
        .addInterchanges(serviceJourneyInterchange)
        .create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void interchangeToStopPointIsNotAPartOfVehicleJourneys() {
    // Mocking only the fromPointRef, to test that the toPointRef is not part of the vehicle journey
    mockGetServiceJourneyStops(
      ServiceJourneyId.ofValidId("TST:ServiceJourney:1"),
      List.of(
        new ServiceJourneyStop(
          new ScheduledStopPointId("TST:ScheduledStopPoint:1"),
          null,
          null,
          0,
          0
        )
      )
    );

    ValidationReport validationReport = runTestFor(1, 2, 1, 2);

    Collection<ValidationReportEntry> validationReportEntries =
      validationReport.getValidationReportEntries();
    assertThat(validationReportEntries.size(), is(1));
    validationReportEntries.forEach(entry ->
      assertThat(
        entry.getName(),
        is(
          StopPointsInVehicleJourneyError.RuleCode.TO_POINT_REF_IN_INTERCHANGE_IS_NOT_PART_OF_TO_JOURNEY_REF.name()
        )
      )
    );
  }

  private ValidationReport runTestFor(
    int fromServiceJourneyId,
    int toServiceJourneyId,
    int fromPointRefId,
    int toPointRefId
  ) {
    assert fromServiceJourneyId != toServiceJourneyId;
    assert fromPointRefId != toPointRefId;

    NetexTestFragment fragment = new NetexTestFragment();

    ScheduledStopPointId fromPointRef = new ScheduledStopPointId(
      "TST:ScheduledStopPoint:" + fromPointRefId
    );
    ScheduledStopPointId toPointRef = new ScheduledStopPointId(
      "TST:ScheduledStopPoint:" + toPointRefId
    );

    ServiceJourneyId fromServiceJourneyRef = new ServiceJourneyId(
      "TST:ServiceJourney:" + fromServiceJourneyId
    );
    ServiceJourneyId toServiceJourneyRef = new ServiceJourneyId(
      "TST:ServiceJourney:" + toServiceJourneyId
    );

    JourneyPattern journeyPattern = fragment
      .journeyPattern()
      .withId(1)
      .withStopPointsInJourneyPattern(
        List.of(
          fragment
            .stopPointInJourneyPattern(1)
            .withScheduledStopPointId(fromPointRefId)
            .create(),
          fragment
            .stopPointInJourneyPattern(2)
            .withScheduledStopPointId(toPointRefId)
            .create()
        )
      )
      .create();

    ServiceJourney serviceJourney1 = fragment
      .serviceJourney(journeyPattern)
      .withId(fromServiceJourneyId)
      .create();

    ServiceJourney serviceJourney2 = fragment
      .serviceJourney(journeyPattern)
      .withId(toServiceJourneyId)
      .create();

    ServiceJourneyInterchange serviceJourneyInterchange = fragment
      .serviceJourneyInterchange()
      .withFromPointRef(fromPointRef)
      .withToPointRef(toPointRef)
      .withFromJourneyRef(fromServiceJourneyRef)
      .withToJourneyRef(toServiceJourneyRef)
      .create();

    return runValidation(
      fragment
        .netexEntitiesIndex()
        .addJourneyPatterns(journeyPattern)
        .addServiceJourneys(serviceJourney1, serviceJourney2)
        .addInterchanges(serviceJourneyInterchange)
        .create()
    );
  }
}
