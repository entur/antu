package no.entur.antu.validation.validator.interchange.mandatoryfields;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.List;
import no.entur.antu.netextestdata.NetexEntitiesTestFactory;
import no.entur.antu.validation.ValidationTest;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.entur.netex.validation.validator.model.QuayId;
import org.entur.netex.validation.validator.model.ScheduledStopPointId;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.ScheduledStopPointRefStructure;

class MandatoryFieldsValidatorTest extends ValidationTest {

  private ValidationReport runValidation(
    NetexEntitiesIndex netexEntitiesIndex
  ) {
    return runValidationOnLineFile(
      netexEntitiesIndex,
      MandatoryFieldsValidator.class
    );
  }

  @Test
  void testAllMandatoryFieldArePresent() {
    ScheduledStopPointRefStructure scheduledStopPointId1 =
      NetexEntitiesTestFactory.createScheduledStopPointRef(1);
    ScheduledStopPointRefStructure scheduledStopPointId2 =
      NetexEntitiesTestFactory.createScheduledStopPointRef(2);

    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

    List<NetexEntitiesTestFactory.CreateServiceJourney> serviceJourneys =
      netexEntitiesTestFactory.createServiceJourneys(
        netexEntitiesTestFactory.createJourneyPattern(),
        2
      );

    netexEntitiesTestFactory
      .createServiceJourneyInterchange()
      .withFromJourneyRef(serviceJourneys.get(0).refObject())
      .withToJourneyRef(serviceJourneys.get(1).refObject())
      .withFromPointRef(scheduledStopPointId1)
      .withToPointRef(scheduledStopPointId2)
      .create();

    mockGetQuayId(
      ScheduledStopPointId.of(scheduledStopPointId1),
      new QuayId("TST:Quay:1")
    );

    mockGetQuayId(
      ScheduledStopPointId.of(scheduledStopPointId2),
      new QuayId("TST:Quay:2")
    );

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void testFromServiceJourneyRefMissing() {
    ScheduledStopPointRefStructure scheduledStopPointId1 =
      NetexEntitiesTestFactory.createScheduledStopPointRef(1);
    ScheduledStopPointRefStructure scheduledStopPointId2 =
      NetexEntitiesTestFactory.createScheduledStopPointRef(2);

    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

    NetexEntitiesTestFactory.CreateServiceJourney serviceJourney =
      netexEntitiesTestFactory.createServiceJourney(
        netexEntitiesTestFactory.createJourneyPattern()
      );

    netexEntitiesTestFactory
      .createServiceJourneyInterchange()
      .withToJourneyRef(serviceJourney.refObject())
      .withFromPointRef(scheduledStopPointId1)
      .withToPointRef(scheduledStopPointId2)
      .create();

    mockGetQuayId(
      ScheduledStopPointId.of(scheduledStopPointId1),
      new QuayId("TST:Quay:1")
    );

    mockGetQuayId(
      ScheduledStopPointId.of(scheduledStopPointId2),
      new QuayId("TST:Quay:2")
    );

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
    assertThat(
      validationReport
        .getValidationReportEntries()
        .stream()
        .findFirst()
        .map(ValidationReportEntry::getMessage)
        .orElse(null),
      is(
        MandatoryFieldsValidator.RULE_MISSING_FROM_SERVICE_JOURNEY_IN_INTERCHANGE.message()
      )
    );
  }

  @Test
  void testToServiceJourneyRefMissing() {
    ScheduledStopPointRefStructure scheduledStopPointId1 =
      NetexEntitiesTestFactory.createScheduledStopPointRef(1);
    ScheduledStopPointRefStructure scheduledStopPointId2 =
      NetexEntitiesTestFactory.createScheduledStopPointRef(2);

    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

    NetexEntitiesTestFactory.CreateServiceJourney serviceJourney =
      netexEntitiesTestFactory.createServiceJourney(
        netexEntitiesTestFactory.createJourneyPattern()
      );

    netexEntitiesTestFactory
      .createServiceJourneyInterchange()
      .withFromJourneyRef(serviceJourney.refObject())
      .withFromPointRef(scheduledStopPointId1)
      .withToPointRef(scheduledStopPointId2)
      .create();

    mockGetQuayId(
      ScheduledStopPointId.of(scheduledStopPointId1),
      new QuayId("TST:Quay:1")
    );

    mockGetQuayId(
      ScheduledStopPointId.of(scheduledStopPointId2),
      new QuayId("TST:Quay:2")
    );

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
    assertThat(
      validationReport
        .getValidationReportEntries()
        .stream()
        .findFirst()
        .map(ValidationReportEntry::getMessage)
        .orElse(null),
      is(
        MandatoryFieldsValidator.RULE_MISSING_TO_SERVICE_JOURNEY_IN_INTERCHANGE.message()
      )
    );
  }

  @Test
  void testFromPointRefMissing() {
    ScheduledStopPointRefStructure scheduledStopPointId =
      NetexEntitiesTestFactory.createScheduledStopPointRef(1);

    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

    NetexEntitiesTestFactory.CreateServiceJourney serviceJourney =
      netexEntitiesTestFactory.createServiceJourney(
        netexEntitiesTestFactory.createJourneyPattern()
      );

    netexEntitiesTestFactory
      .createServiceJourneyInterchange()
      .withFromJourneyRef(serviceJourney.refObject())
      .withToJourneyRef(serviceJourney.refObject())
      .withToPointRef(scheduledStopPointId)
      .create();

    mockGetQuayId(
      ScheduledStopPointId.of(scheduledStopPointId),
      new QuayId("TST:Quay:1")
    );

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
    assertThat(
      validationReport
        .getValidationReportEntries()
        .stream()
        .findFirst()
        .map(ValidationReportEntry::getMessage)
        .orElse(null),
      is(
        MandatoryFieldsValidator.RULE_MISSING_FROM_STOP_POINT_IN_INTERCHANGE.message()
      )
    );
  }

  @Test
  void testToPointRefMissing() {
    ScheduledStopPointRefStructure scheduledStopPointId =
      NetexEntitiesTestFactory.createScheduledStopPointRef(1);

    NetexEntitiesTestFactory netexEntitiesFactory =
      new NetexEntitiesTestFactory();

    NetexEntitiesTestFactory.CreateServiceJourney serviceJourney =
      netexEntitiesFactory.createServiceJourney(
        netexEntitiesFactory.createJourneyPattern()
      );

    netexEntitiesFactory
      .createServiceJourneyInterchange()
      .withFromJourneyRef(serviceJourney.refObject())
      .withToJourneyRef(serviceJourney.refObject())
      .withFromPointRef(scheduledStopPointId)
      .create();

    mockGetQuayId(
      ScheduledStopPointId.of(scheduledStopPointId),
      new QuayId("TST:Quay:1")
    );

    ValidationReport validationReport = runValidation(
      netexEntitiesFactory.create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
    assertThat(
      validationReport
        .getValidationReportEntries()
        .stream()
        .findFirst()
        .map(ValidationReportEntry::getMessage)
        .orElse(null),
      is(
        MandatoryFieldsValidator.RULE_MISSING_TO_STOP_POINT_IN_INTERCHANGE.message()
      )
    );
  }
  /*
   * Keeping these tests in comments for using them later in other validation rules.
  @Test
  void testFromPointRefHasNoAssignment() {
    ScheduledStopPointId scheduledStopPointId1 = new ScheduledStopPointId(
      "TST:ScheduledStopPoint:1"
    );
    ScheduledStopPointId scheduledStopPointId2 = new ScheduledStopPointId(
      "TST:ScheduledStopPoint:2"
    );

    NetexTestFragment netexEntitiesFactory = new NetexTestFragment();
    List<ServiceJourney> serviceJourneys = netexEntitiesFactory.createServiceJourneys(
      netexEntitiesFactory.journeyPattern().create(),
      2
    );

    ServiceJourneyInterchange serviceJourneyInterchange = netexEntitiesFactory
      .serviceJourneyInterchange()
      .withId(1)
      .withFromJourneyRef(serviceJourneys.get(0).getId())
      .withToJourneyRef(serviceJourneys.get(1).getId())
      .withFromPointRef(scheduledStopPointId1.id())
      .withToPointRef(scheduledStopPointId2.id())
      .create();

    mockGetQuayId(
      new ScheduledStopPointId(scheduledStopPointId2.id()),
      new QuayId("TST:Quay:2")
    );

    ValidationReport validationReport = runValidation(
      netexEntitiesFactory
        .netexEntitiesIndex()
        .addServiceJourneys(
          serviceJourneys.toArray(Journey_VersionStructure[]::new)
        )
        .addInterchanges(serviceJourneyInterchange)
        .create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
    assertThat(
      validationReport
        .getValidationReportEntries()
        .stream()
        .findFirst()
        .map(ValidationReportEntry::getMessage)
        .orElse(null),
      is(
        MandatoryFieldsError.RuleCode.MISSING_FROM_STOP_POINT_IN_INTERCHANGE.getErrorMessage()
      )
    );
  }

  @Test
  void testToPointRefHasNoAssignment() {
    ScheduledStopPointId scheduledStopPointId1 = new ScheduledStopPointId(
      "TST:ScheduledStopPoint:1"
    );
    ScheduledStopPointId scheduledStopPointId2 = new ScheduledStopPointId(
      "TST:ScheduledStopPoint:2"
    );

    NetexTestFragment netexEntitiesFactory = new NetexTestFragment();
    List<ServiceJourney> serviceJourneys = netexEntitiesFactory.createServiceJourneys(
      netexEntitiesFactory.journeyPattern().create(),
      2
    );

    ServiceJourneyInterchange serviceJourneyInterchange = netexEntitiesFactory
      .serviceJourneyInterchange()
      .withId(1)
      .withFromJourneyRef(serviceJourneys.get(0).getId())
      .withToJourneyRef(serviceJourneys.get(1).getId())
      .withFromPointRef(scheduledStopPointId1.id())
      .withToPointRef(scheduledStopPointId2.id())
      .create();

    mockGetQuayId(
      new ScheduledStopPointId(scheduledStopPointId1.id()),
      new QuayId("TST:Quay:1")
    );

    ValidationReport validationReport = runValidation(
      netexEntitiesFactory
        .netexEntitiesIndex()
        .addServiceJourneys(
          serviceJourneys.toArray(Journey_VersionStructure[]::new)
        )
        .addInterchanges(serviceJourneyInterchange)
        .create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
    assertThat(
      validationReport
        .getValidationReportEntries()
        .stream()
        .findFirst()
        .map(ValidationReportEntry::getMessage)
        .orElse(null),
      is(
        MandatoryFieldsError.RuleCode.MISSING_TO_STOP_POINT_IN_INTERCHANGE.getErrorMessage()
      )
    );
  }
  **/
}
