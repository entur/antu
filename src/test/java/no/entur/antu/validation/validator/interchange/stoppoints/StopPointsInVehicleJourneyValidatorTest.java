package no.entur.antu.validation.validator.interchange.stoppoints;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import no.entur.antu.netex.test.NetexTestData;
import no.entur.antu.netex.test.ValidatorTestBase;
import no.entur.antu.netex.test.builder.NetexRefs;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.entur.netex.validation.validator.model.ScheduledStopPointId;
import org.entur.netex.validation.validator.model.ServiceJourneyId;
import org.entur.netex.validation.validator.model.ServiceJourneyInterchangeInfo;
import org.entur.netex.validation.validator.model.ServiceJourneyStop;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.ServiceJourneyInterchange;

class StopPointsInVehicleJourneyValidatorTest extends ValidatorTestBase {

  @Test
  void interchangeStopPointArePartOfVehicleJourneys() {
    // Mocking both the fromPointRef and toPointRef,
    // to test that both fromPointRef and toPointRef are part of the vehicle journey.
    withServiceJourneyStops(
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
    withServiceJourneyStops(
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
      new StopPointsInVehicleJourneyValidator(
        validationReportEntryFactory(),
        netexDataRepository
      )
    );

    Assertions.assertEquals(
      0,
      validationReport.getValidationReportEntries().size()
    );
  }

  @Test
  void interchangeWithMissingAttributes() {
    NetexTestData fragment = new NetexTestData();
    ServiceJourneyInterchange serviceJourneyInterchange = fragment
      .addServiceJourneyInterchange()
      .build();

    withServiceJourneyInterchangeInfos(
      List.of(
        ServiceJourneyInterchangeInfo.of("test.xml", serviceJourneyInterchange)
      )
    );

    ValidationReport validationReport = runDatasetValidation(
      new StopPointsInVehicleJourneyValidator(
        validationReportEntryFactory(),
        netexDataRepository
      )
    );

    Assertions.assertEquals(
      0,
      validationReport.getValidationReportEntries().size()
    );
  }

  @Test
  void interchangeWithNoExistingToServiceJourneyGiveNoErrors() {
    // Mocking only the fromPointRef, to test that the toPointRef is not part of the vehicle journey
    withServiceJourneyStops(
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
    withServiceJourneyStops(
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
    withServiceJourneyStops(
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
    NetexTestData fragment = new NetexTestData();

    ServiceJourneyInterchange serviceJourneyInterchange = fragment
      .addServiceJourneyInterchange()
      .withFromPointRef(NetexRefs.scheduledStopPointRef(1))
      .withToPointRef(NetexRefs.scheduledStopPointRef(2))
      .withFromJourneyRef(NetexRefs.serviceJourneyRef(1))
      .withToJourneyRef(NetexRefs.serviceJourneyRef(2))
      .build();

    withServiceJourneyInterchangeInfos(
      List.of(
        ServiceJourneyInterchangeInfo.of("test.xml", serviceJourneyInterchange)
      )
    );

    return runDatasetValidation(
      new StopPointsInVehicleJourneyValidator(
        validationReportEntryFactory(),
        netexDataRepository
      )
    );
  }
}
