package no.entur.antu.validation.validator.interchange.stoppoints;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import no.entur.antu.netextestdata.NetexEntitiesTestFactory;
import no.entur.antu.validation.ValidationTest;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.entur.netex.validation.validator.model.ScheduledStopPointId;
import org.entur.netex.validation.validator.model.ServiceJourneyId;
import org.entur.netex.validation.validator.model.ServiceJourneyInterchangeInfo;
import org.entur.netex.validation.validator.model.ServiceJourneyStop;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.ServiceJourneyInterchange;

class StopPointsInVehicleJourneyValidatorTest extends ValidationTest {

  @Test
  void interchangeStopPointArePartOfVehicleJourneys() {
    // Mocking both the fromPointRef and toPointRef,
    // to test that both fromPointRef and toPointRef are part of the vehicle journey.
    mockGetServiceJourneyStops(
      Map.of(
        ServiceJourneyId.ofValidId("TST:ServiceJourney:1"),
        List.of(
          new ServiceJourneyStop(
            new ScheduledStopPointId("TST:ScheduledStopPoint:1"),
            null,
            null,
            0,
            0
          )
        ),
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
      )
    );

    ValidationReport validationReport = runTestFor(1, 2, 1, 2);

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void interchangeFromStopPointIsNotAPartOfVehicleJourneys() {
    // Mocking only the toPointRef, to test that the fromPointRef is not part of the vehicle journey
    mockGetServiceJourneyStops(
      Map.of(
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
          StopPointsInVehicleJourneyValidator.RULE_FROM_POINT_REF_IN_INTERCHANGE_IS_NOT_PART_OF_FROM_JOURNEY_REF.name()
        )
      )
    );
  }

  @Test
  void noInterchangeNoEffect() {
    ValidationReport validationReport = runDatasetValidation(
      StopPointsInVehicleJourneyValidator.class
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void interchangeWithMissingAttributes() {
    NetexEntitiesTestFactory fragment = new NetexEntitiesTestFactory();
    ServiceJourneyInterchange serviceJourneyInterchange = fragment
      .createServiceJourneyInterchange()
      .create();

    mockGetServiceJourneyInterchangeInfo(
      List.of(
        ServiceJourneyInterchangeInfo.of("test.xml", serviceJourneyInterchange)
      )
    );

    ValidationReport validationReport = runDatasetValidation(
      StopPointsInVehicleJourneyValidator.class
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void interchangeToStopPointIsNotAPartOfVehicleJourneys() {
    // Mocking only the fromPointRef, to test that the toPointRef is not part of the vehicle journey
    mockGetServiceJourneyStops(
      Map.of(
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
          StopPointsInVehicleJourneyValidator.RULE_TO_POINT_REF_IN_INTERCHANGE_IS_NOT_PART_OF_TO_JOURNEY_REF.name()
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

    NetexEntitiesTestFactory fragment = new NetexEntitiesTestFactory();

    ServiceJourneyInterchange serviceJourneyInterchange = fragment
      .createServiceJourneyInterchange()
      .withFromPointRef(
        NetexEntitiesTestFactory.createScheduledStopPointRef(fromPointRefId)
      )
      .withToPointRef(
        NetexEntitiesTestFactory.createScheduledStopPointRef(toPointRefId)
      )
      .withFromJourneyRef(
        NetexEntitiesTestFactory.createServiceJourneyRef(fromServiceJourneyId)
      )
      .withToJourneyRef(
        NetexEntitiesTestFactory.createServiceJourneyRef(toServiceJourneyId)
      )
      .create();

    mockGetServiceJourneyInterchangeInfo(
      List.of(
        ServiceJourneyInterchangeInfo.of("test.xml", serviceJourneyInterchange)
      )
    );

    return runDatasetValidation(StopPointsInVehicleJourneyValidator.class);
  }
}
