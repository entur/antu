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
import org.entur.netex.validation.validator.model.ServiceJourneyId;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.ServiceJourneyInterchange;

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
    ScheduledStopPointId scheduledStopPointId1 = new ScheduledStopPointId(
      "TST:ScheduledStopPoint:1"
    );
    ScheduledStopPointId scheduledStopPointId2 = new ScheduledStopPointId(
      "TST:ScheduledStopPoint:2"
    );

    NetexEntitiesTestFactory netexEntitiesFactory =
      new NetexEntitiesTestFactory();
    List<ServiceJourney> serviceJourneys =
      netexEntitiesFactory.createServiceJourneys(
        netexEntitiesFactory.journeyPattern().create(),
        2
      );

    ServiceJourneyInterchange serviceJourneyInterchange = netexEntitiesFactory
      .serviceJourneyInterchange()
      .withId(1)
      .withFromJourneyRef(ServiceJourneyId.ofValidId(serviceJourneys.get(0)))
      .withToJourneyRef(ServiceJourneyId.ofValidId(serviceJourneys.get(1)))
      .withFromPointRef(scheduledStopPointId1)
      .withToPointRef(scheduledStopPointId2)
      .create();

    mockGetQuayId(
      new ScheduledStopPointId(scheduledStopPointId1.id()),
      new QuayId("TST:Quay:1")
    );

    mockGetQuayId(scheduledStopPointId2, new QuayId("TST:Quay:2"));

    ValidationReport validationReport = runValidation(
      netexEntitiesFactory
        .netexEntitiesIndex()
        .addServiceJourneys(serviceJourneys.toArray(ServiceJourney[]::new))
        .addInterchanges(serviceJourneyInterchange)
        .create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void testFromServiceJourneyRefMissing() {
    ScheduledStopPointId scheduledStopPointId1 = new ScheduledStopPointId(
      "TST:ScheduledStopPoint:1"
    );
    ScheduledStopPointId scheduledStopPointId2 = new ScheduledStopPointId(
      "TST:ScheduledStopPoint:2"
    );

    NetexEntitiesTestFactory netexEntitiesFactory =
      new NetexEntitiesTestFactory();

    ServiceJourney serviceJourney = netexEntitiesFactory
      .serviceJourney(netexEntitiesFactory.journeyPattern().create())
      .create();

    ServiceJourneyInterchange serviceJourneyInterchange = netexEntitiesFactory
      .serviceJourneyInterchange()
      .withId(1)
      .withToJourneyRef(ServiceJourneyId.ofValidId(serviceJourney))
      .withFromPointRef(scheduledStopPointId1)
      .withToPointRef(scheduledStopPointId2)
      .create();

    mockGetQuayId(scheduledStopPointId1, new QuayId("TST:Quay:1"));

    mockGetQuayId(scheduledStopPointId2, new QuayId("TST:Quay:2"));

    ValidationReport validationReport = runValidation(
      netexEntitiesFactory
        .netexEntitiesIndex()
        .addServiceJourneys(serviceJourney)
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
        MandatoryFieldsValidator.RULE_MISSING_FROM_SERVICE_JOURNEY_IN_INTERCHANGE.message()
      )
    );
  }

  @Test
  void testToServiceJourneyRefMissing() {
    ScheduledStopPointId scheduledStopPointId1 = new ScheduledStopPointId(
      "TST:ScheduledStopPoint:1"
    );
    ScheduledStopPointId scheduledStopPointId2 = new ScheduledStopPointId(
      "TST:ScheduledStopPoint:2"
    );

    NetexEntitiesTestFactory netexEntitiesFactory =
      new NetexEntitiesTestFactory();

    ServiceJourney serviceJourney = netexEntitiesFactory
      .serviceJourney(netexEntitiesFactory.journeyPattern().create())
      .create();

    ServiceJourneyInterchange serviceJourneyInterchange = netexEntitiesFactory
      .serviceJourneyInterchange()
      .withId(1)
      .withFromJourneyRef(ServiceJourneyId.ofValidId(serviceJourney))
      .withFromPointRef(scheduledStopPointId1)
      .withToPointRef(scheduledStopPointId2)
      .create();

    mockGetQuayId(scheduledStopPointId1, new QuayId("TST:Quay:1"));

    mockGetQuayId(
      new ScheduledStopPointId(scheduledStopPointId2.id()),
      new QuayId("TST:Quay:2")
    );

    ValidationReport validationReport = runValidation(
      netexEntitiesFactory
        .netexEntitiesIndex()
        .addServiceJourneys(serviceJourney)
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
        MandatoryFieldsValidator.RULE_MISSING_TO_SERVICE_JOURNEY_IN_INTERCHANGE.message()
      )
    );
  }

  @Test
  void testFromPointRefMissing() {
    ScheduledStopPointId scheduledStopPointId = new ScheduledStopPointId(
      "TST:ScheduledStopPoint:1"
    );

    NetexEntitiesTestFactory netexEntitiesFactory =
      new NetexEntitiesTestFactory();

    ServiceJourney serviceJourney = netexEntitiesFactory
      .serviceJourney(netexEntitiesFactory.journeyPattern().create())
      .create();

    ServiceJourneyInterchange serviceJourneyInterchange = netexEntitiesFactory
      .serviceJourneyInterchange()
      .withId(1)
      .withFromJourneyRef(ServiceJourneyId.ofValidId(serviceJourney))
      .withToJourneyRef(ServiceJourneyId.ofValidId(serviceJourney))
      .withToPointRef(scheduledStopPointId)
      .create();

    mockGetQuayId(scheduledStopPointId, new QuayId("TST:Quay:1"));

    ValidationReport validationReport = runValidation(
      netexEntitiesFactory
        .netexEntitiesIndex()
        .addServiceJourneys(serviceJourney)
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
        MandatoryFieldsValidator.RULE_MISSING_FROM_STOP_POINT_IN_INTERCHANGE.message()
      )
    );
  }

  @Test
  void testToPointRefMissing() {
    ScheduledStopPointId scheduledStopPointId = new ScheduledStopPointId(
      "TST:ScheduledStopPoint:1"
    );

    NetexEntitiesTestFactory netexEntitiesFactory =
      new NetexEntitiesTestFactory();

    ServiceJourney serviceJourney = netexEntitiesFactory
      .serviceJourney(netexEntitiesFactory.journeyPattern().create())
      .create();

    ServiceJourneyInterchange serviceJourneyInterchange = netexEntitiesFactory
      .serviceJourneyInterchange()
      .withId(1)
      .withFromJourneyRef(ServiceJourneyId.ofValidId(serviceJourney))
      .withToJourneyRef(ServiceJourneyId.ofValidId(serviceJourney))
      .withFromPointRef(scheduledStopPointId)
      .create();

    mockGetQuayId(scheduledStopPointId, new QuayId("TST:Quay:1"));

    ValidationReport validationReport = runValidation(
      netexEntitiesFactory
        .netexEntitiesIndex()
        .addServiceJourneys(serviceJourney)
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
        MandatoryFieldsValidator.RULE_MISSING_TO_STOP_POINT_IN_INTERCHANGE.message()
      )
    );
  }
  /**
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
