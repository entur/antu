package no.entur.antu.validation.validator.interchange.mandatoryfields;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.List;
import no.entur.antu.model.QuayId;
import no.entur.antu.model.ScheduledStopPointId;
import no.entur.antu.netextestdata.NetexTestFragment;
import no.entur.antu.validation.ValidationTest;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.Journey_VersionStructure;
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

    NetexTestFragment netexFragment = new NetexTestFragment();
    List<ServiceJourney> serviceJourneys = netexFragment.createServiceJourneys(
      netexFragment.journeyPattern().create(),
      2
    );

    ServiceJourneyInterchange serviceJourneyInterchange = netexFragment
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

    mockGetQuayId(
      new ScheduledStopPointId(scheduledStopPointId2.id()),
      new QuayId("TST:Quay:2")
    );

    ValidationReport validationReport = runValidation(
      netexFragment
        .netexEntitiesIndex()
        .addServiceJourneys(
          serviceJourneys.toArray(Journey_VersionStructure[]::new)
        )
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

    NetexTestFragment netexFragment = new NetexTestFragment();

    ServiceJourney serviceJourney = netexFragment
      .serviceJourney(netexFragment.journeyPattern().create())
      .create();

    ServiceJourneyInterchange serviceJourneyInterchange = netexFragment
      .serviceJourneyInterchange()
      .withId(1)
      .withToJourneyRef(serviceJourney.getId())
      .withFromPointRef(scheduledStopPointId1.id())
      .withToPointRef(scheduledStopPointId2.id())
      .create();

    mockGetQuayId(
      new ScheduledStopPointId(scheduledStopPointId1.id()),
      new QuayId("TST:Quay:1")
    );

    mockGetQuayId(
      new ScheduledStopPointId(scheduledStopPointId2.id()),
      new QuayId("TST:Quay:2")
    );

    ValidationReport validationReport = runValidation(
      netexFragment
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
        MandatoryFieldsError.RuleCode.MISSING_FROM_SERVICE_JOURNEY_IN_INTERCHANGE.getErrorMessage()
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

    NetexTestFragment netexFragment = new NetexTestFragment();

    ServiceJourney serviceJourney = netexFragment
      .serviceJourney(netexFragment.journeyPattern().create())
      .create();

    ServiceJourneyInterchange serviceJourneyInterchange = netexFragment
      .serviceJourneyInterchange()
      .withId(1)
      .withFromJourneyRef(serviceJourney.getId())
      .withFromPointRef(scheduledStopPointId1.id())
      .withToPointRef(scheduledStopPointId2.id())
      .create();

    mockGetQuayId(
      new ScheduledStopPointId(scheduledStopPointId1.id()),
      new QuayId("TST:Quay:1")
    );

    mockGetQuayId(
      new ScheduledStopPointId(scheduledStopPointId2.id()),
      new QuayId("TST:Quay:2")
    );

    ValidationReport validationReport = runValidation(
      netexFragment
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
        MandatoryFieldsError.RuleCode.MISSING_TO_SERVICE_JOURNEY_IN_INTERCHANGE.getErrorMessage()
      )
    );
  }

  @Test
  void testFromPointRefMissing() {
    ScheduledStopPointId scheduledStopPointId = new ScheduledStopPointId(
      "TST:ScheduledStopPoint:1"
    );

    NetexTestFragment netexFragment = new NetexTestFragment();

    ServiceJourney serviceJourney = netexFragment
      .serviceJourney(netexFragment.journeyPattern().create())
      .create();

    ServiceJourneyInterchange serviceJourneyInterchange = netexFragment
      .serviceJourneyInterchange()
      .withId(1)
      .withFromJourneyRef(serviceJourney.getId())
      .withToJourneyRef(serviceJourney.getId())
      .withToPointRef(scheduledStopPointId.id())
      .create();

    mockGetQuayId(
      new ScheduledStopPointId(scheduledStopPointId.id()),
      new QuayId("TST:Quay:1")
    );

    ValidationReport validationReport = runValidation(
      netexFragment
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
        MandatoryFieldsError.RuleCode.MISSING_FROM_STOP_POINT_IN_INTERCHANGE.getErrorMessage()
      )
    );
  }

  @Test
  void testToPointRefMissing() {
    ScheduledStopPointId scheduledStopPointId = new ScheduledStopPointId(
      "TST:ScheduledStopPoint:1"
    );

    NetexTestFragment netexFragment = new NetexTestFragment();

    ServiceJourney serviceJourney = netexFragment
      .serviceJourney(netexFragment.journeyPattern().create())
      .create();

    ServiceJourneyInterchange serviceJourneyInterchange = netexFragment
      .serviceJourneyInterchange()
      .withId(1)
      .withFromJourneyRef(serviceJourney.getId())
      .withToJourneyRef(serviceJourney.getId())
      .withFromPointRef(scheduledStopPointId.id())
      .create();

    mockGetQuayId(
      new ScheduledStopPointId(scheduledStopPointId.id()),
      new QuayId("TST:Quay:1")
    );

    ValidationReport validationReport = runValidation(
      netexFragment
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
        MandatoryFieldsError.RuleCode.MISSING_TO_STOP_POINT_IN_INTERCHANGE.getErrorMessage()
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

    NetexTestFragment netexFragment = new NetexTestFragment();
    List<ServiceJourney> serviceJourneys = netexFragment.createServiceJourneys(
      netexFragment.journeyPattern().create(),
      2
    );

    ServiceJourneyInterchange serviceJourneyInterchange = netexFragment
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
      netexFragment
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

    NetexTestFragment netexFragment = new NetexTestFragment();
    List<ServiceJourney> serviceJourneys = netexFragment.createServiceJourneys(
      netexFragment.journeyPattern().create(),
      2
    );

    ServiceJourneyInterchange serviceJourneyInterchange = netexFragment
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
      netexFragment
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
