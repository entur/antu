package no.entur.antu.validation.validator.interchange.stoppoints;

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
import org.junit.jupiter.api.Assertions;
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
            0,
            true,
            true
          )
        ),
        ServiceJourneyId.ofValidId("TST:ServiceJourney:2"),
        List.of(
          new ServiceJourneyStop(
            new ScheduledStopPointId("TST:ScheduledStopPoint:2"),
            null,
            null,
            0,
            0,
            true,
            true
          )
        )
      )
    );

    ValidationReport validationReport = runTestFor();

    Assertions.assertEquals(
      0,
      validationReport.getValidationReportEntries().size()
    );
  }

  @Test
  void interchangeWithNoExistingFromServiceJourneyGiveNoErrors() {
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
            0,
            true,
            true
          )
        )
      )
    );

    ValidationReport validationReport = runTestFor();

    Collection<ValidationReportEntry> validationReportEntries =
      validationReport.getValidationReportEntries();
    Assertions.assertEquals(0, validationReportEntries.size());
  }

  @Test
  void noInterchangeNoEffect() {
    ValidationReport validationReport = runDatasetValidation(
      StopPointsInVehicleJourneyValidator.class
    );

    Assertions.assertEquals(
      0,
      validationReport.getValidationReportEntries().size()
    );
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

    Assertions.assertEquals(
      0,
      validationReport.getValidationReportEntries().size()
    );
  }

  @Test
  void interchangeWithNoExistingToServiceJourneyGiveNoErrors() {
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
            0,
            true,
            true
          )
        )
      )
    );

    ValidationReport validationReport = runTestFor();

    Collection<ValidationReportEntry> validationReportEntries =
      validationReport.getValidationReportEntries();
    Assertions.assertEquals(0, validationReportEntries.size());
  }

  @Test
  void interchangeWithNoMatchingFromStopsGivesValidationError() {
    mockGetServiceJourneyStops(
      Map.of(
        ServiceJourneyId.ofValidId("TST:ServiceJourney:1"),
        List.of(
          new ServiceJourneyStop(
            new ScheduledStopPointId("TST:ScheduledStopPoint:999"),
            null,
            null,
            0,
            0,
            true,
            true
          )
        )
      )
    );

    ValidationReport validationReport = runTestFor();
    Assertions.assertEquals(
      1,
      validationReport.getValidationReportEntries().size()
    );
    var validationReportEntry = validationReport
      .getValidationReportEntries()
      .iterator()
      .next();
    Assertions.assertEquals(
      StopPointsInVehicleJourneyValidator.RULE_FROM_POINT_REF_IN_INTERCHANGE_IS_NOT_PART_OF_FROM_JOURNEY_REF.name(),
      validationReportEntry.getName()
    );
  }

  @Test
  void interchangeWithNoMatchingToStopsGivesValidationError() {
    mockGetServiceJourneyStops(
      Map.of(
        ServiceJourneyId.ofValidId("TST:ServiceJourney:1"),
        List.of(
          new ServiceJourneyStop(
            new ScheduledStopPointId("TST:ScheduledStopPoint:1"),
            null,
            null,
            0,
            0,
            true,
            true
          )
        ),
        ServiceJourneyId.ofValidId("TST:ServiceJourney:2"),
        List.of(
          new ServiceJourneyStop(
            new ScheduledStopPointId("TST:ScheduledStopPoint:999"),
            null,
            null,
            0,
            0,
            true,
            true
          )
        )
      )
    );

    ValidationReport validationReport = runTestFor();
    Assertions.assertEquals(
      1,
      validationReport.getValidationReportEntries().size()
    );
    var validationReportEntry = validationReport
      .getValidationReportEntries()
      .iterator()
      .next();
    Assertions.assertEquals(
      StopPointsInVehicleJourneyValidator.RULE_TO_POINT_REF_IN_INTERCHANGE_IS_NOT_PART_OF_TO_JOURNEY_REF.name(),
      validationReportEntry.getName()
    );
  }

  private ValidationReport runTestFor() {
    NetexEntitiesTestFactory fragment = new NetexEntitiesTestFactory();

    ServiceJourneyInterchange serviceJourneyInterchange = fragment
      .createServiceJourneyInterchange()
      .withFromPointRef(NetexEntitiesTestFactory.createScheduledStopPointRef(1))
      .withToPointRef(NetexEntitiesTestFactory.createScheduledStopPointRef(2))
      .withFromJourneyRef(NetexEntitiesTestFactory.createServiceJourneyRef(1))
      .withToJourneyRef(NetexEntitiesTestFactory.createServiceJourneyRef(2))
      .create();

    mockGetServiceJourneyInterchangeInfo(
      List.of(
        ServiceJourneyInterchangeInfo.of("test.xml", serviceJourneyInterchange)
      )
    );

    return runDatasetValidation(StopPointsInVehicleJourneyValidator.class);
  }
}
