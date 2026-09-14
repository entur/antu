package no.entur.antu.validation.validator.interchange.mandatoryfields;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.List;
import no.entur.antu.netex.test.NetexTestData;
import no.entur.antu.netex.test.ValidatorTestBase;
import no.entur.antu.netex.test.builder.NetexRefs;
import no.entur.antu.netex.test.builder.ServiceJourneyBuilder;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.entur.netex.validation.validator.model.QuayId;
import org.entur.netex.validation.validator.model.ScheduledStopPointId;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.ScheduledStopPointRefStructure;

class MandatoryFieldsValidatorTest extends ValidatorTestBase {

  private ValidationReport runValidation(
    NetexEntitiesIndex netexEntitiesIndex
  ) {
    return runValidationOnLineFile(
      netexEntitiesIndex,
      new MandatoryFieldsValidator()
    );
  }

  @Test
  void testAllMandatoryFieldArePresent() {
    ScheduledStopPointRefStructure scheduledStopPointId1 =
      NetexRefs.scheduledStopPointRef(1);
    ScheduledStopPointRefStructure scheduledStopPointId2 =
      NetexRefs.scheduledStopPointRef(2);

    NetexTestData netexEntitiesTestFactory = new NetexTestData();

    List<ServiceJourneyBuilder> serviceJourneys =
      netexEntitiesTestFactory.addServiceJourneys(
        netexEntitiesTestFactory.addJourneyPattern(),
        2
      );

    netexEntitiesTestFactory
      .addServiceJourneyInterchange()
      .withFromJourneyRef(serviceJourneys.get(0).refObject())
      .withToJourneyRef(serviceJourneys.get(1).refObject())
      .withFromPointRef(scheduledStopPointId1)
      .withToPointRef(scheduledStopPointId2)
      .build();

    withQuayId(
      ScheduledStopPointId.of(scheduledStopPointId1),
      new QuayId("TST:Quay:1")
    );

    withQuayId(
      ScheduledStopPointId.of(scheduledStopPointId2),
      new QuayId("TST:Quay:2")
    );

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.build()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void testFromServiceJourneyRefMissing() {
    ScheduledStopPointRefStructure scheduledStopPointId1 =
      NetexRefs.scheduledStopPointRef(1);
    ScheduledStopPointRefStructure scheduledStopPointId2 =
      NetexRefs.scheduledStopPointRef(2);

    NetexTestData netexEntitiesTestFactory = new NetexTestData();

    ServiceJourneyBuilder serviceJourney =
      netexEntitiesTestFactory.addServiceJourney(
        netexEntitiesTestFactory.addJourneyPattern()
      );

    netexEntitiesTestFactory
      .addServiceJourneyInterchange()
      .withToJourneyRef(serviceJourney.refObject())
      .withFromPointRef(scheduledStopPointId1)
      .withToPointRef(scheduledStopPointId2)
      .build();

    withQuayId(
      ScheduledStopPointId.of(scheduledStopPointId1),
      new QuayId("TST:Quay:1")
    );

    withQuayId(
      ScheduledStopPointId.of(scheduledStopPointId2),
      new QuayId("TST:Quay:2")
    );

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.build()
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
      NetexRefs.scheduledStopPointRef(1);
    ScheduledStopPointRefStructure scheduledStopPointId2 =
      NetexRefs.scheduledStopPointRef(2);

    NetexTestData netexEntitiesTestFactory = new NetexTestData();

    ServiceJourneyBuilder serviceJourney =
      netexEntitiesTestFactory.addServiceJourney(
        netexEntitiesTestFactory.addJourneyPattern()
      );

    netexEntitiesTestFactory
      .addServiceJourneyInterchange()
      .withFromJourneyRef(serviceJourney.refObject())
      .withFromPointRef(scheduledStopPointId1)
      .withToPointRef(scheduledStopPointId2)
      .build();

    withQuayId(
      ScheduledStopPointId.of(scheduledStopPointId1),
      new QuayId("TST:Quay:1")
    );

    withQuayId(
      ScheduledStopPointId.of(scheduledStopPointId2),
      new QuayId("TST:Quay:2")
    );

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.build()
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
      NetexRefs.scheduledStopPointRef(1);

    NetexTestData netexEntitiesTestFactory = new NetexTestData();

    ServiceJourneyBuilder serviceJourney =
      netexEntitiesTestFactory.addServiceJourney(
        netexEntitiesTestFactory.addJourneyPattern()
      );

    netexEntitiesTestFactory
      .addServiceJourneyInterchange()
      .withFromJourneyRef(serviceJourney.refObject())
      .withToJourneyRef(serviceJourney.refObject())
      .withToPointRef(scheduledStopPointId)
      .build();

    withQuayId(
      ScheduledStopPointId.of(scheduledStopPointId),
      new QuayId("TST:Quay:1")
    );

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.build()
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
      NetexRefs.scheduledStopPointRef(1);

    NetexTestData netexEntitiesFactory = new NetexTestData();

    ServiceJourneyBuilder serviceJourney =
      netexEntitiesFactory.addServiceJourney(
        netexEntitiesFactory.addJourneyPattern()
      );

    netexEntitiesFactory
      .addServiceJourneyInterchange()
      .withFromJourneyRef(serviceJourney.refObject())
      .withToJourneyRef(serviceJourney.refObject())
      .withFromPointRef(scheduledStopPointId)
      .build();

    withQuayId(
      ScheduledStopPointId.of(scheduledStopPointId),
      new QuayId("TST:Quay:1")
    );

    ValidationReport validationReport = runValidation(
      netexEntitiesFactory.build()
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
    List<ServiceJourney> serviceJourneys = netexEntitiesFactory.addServiceJourneys(
      netexEntitiesFactory.journeyPattern().build(),
      2
    );

    ServiceJourneyInterchange serviceJourneyInterchange = netexEntitiesFactory
      .serviceJourneyInterchange()
      .withId(1)
      .withFromJourneyRef(serviceJourneys.get(0).getId())
      .withToJourneyRef(serviceJourneys.get(1).getId())
      .withFromPointRef(scheduledStopPointId1.id())
      .withToPointRef(scheduledStopPointId2.id())
      .build();

    withQuayId(
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
        .build()
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
    List<ServiceJourney> serviceJourneys = netexEntitiesFactory.addServiceJourneys(
      netexEntitiesFactory.journeyPattern().build(),
      2
    );

    ServiceJourneyInterchange serviceJourneyInterchange = netexEntitiesFactory
      .serviceJourneyInterchange()
      .withId(1)
      .withFromJourneyRef(serviceJourneys.get(0).getId())
      .withToJourneyRef(serviceJourneys.get(1).getId())
      .withFromPointRef(scheduledStopPointId1.id())
      .withToPointRef(scheduledStopPointId2.id())
      .build();

    withQuayId(
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
        .build()
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
