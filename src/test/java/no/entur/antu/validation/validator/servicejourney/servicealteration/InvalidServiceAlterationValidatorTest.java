package no.entur.antu.validation.validator.servicejourney.servicealteration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.IntStream;
import no.entur.antu.netextestdata.NetexTestFragment;
import no.entur.antu.validation.ValidationTest;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.DatedServiceJourney;
import org.rutebanken.netex.model.ServiceAlterationEnumeration;

class InvalidServiceAlterationValidatorTest extends ValidationTest {

  private ValidationReport runValidation(
    NetexEntitiesIndex netexEntitiesIndex
  ) {
    return runValidationOnLineFile(
      netexEntitiesIndex,
      InvalidServiceAlterationValidator.class
    );
  }

  @Test
  void testCorrectServiceAlterationExists() {
    NetexTestFragment testData = new NetexTestFragment();

    DatedServiceJourney datedServiceJourneyReplaced = testData
      .datedServiceJourney(1)
      .withId(1)
      .withServiceAlteration(ServiceAlterationEnumeration.REPLACED)
      .create();

    DatedServiceJourney datedServiceJourneyNew = testData
      .datedServiceJourney(2)
      .withId(2)
      .withDatedServiceJourneyRef(1) // Reference to replaced
      .create();

    ValidationReport validationReport = runValidation(
      testData
        .netexEntitiesIndex()
        .addJourneys(datedServiceJourneyReplaced, datedServiceJourneyNew)
        .create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void testCorrectServiceAlterationExistsForMultipleDSJs() {
    NetexTestFragment testData = new NetexTestFragment();

    NetexTestFragment.CreateNetexEntitiesIndex createNetexEntitiesIndex =
      testData.netexEntitiesIndex();

    IntStream
      .of(1, 2, 3)
      .mapToObj(i ->
        testData
          .datedServiceJourney(i)
          .withId(i)
          .withServiceAlteration(ServiceAlterationEnumeration.REPLACED)
          .create()
      )
      .forEach(createNetexEntitiesIndex::addJourneys);

    IntStream
      .of(1, 2, 3)
      .mapToObj(i ->
        testData
          .datedServiceJourney(i + 3)
          .withId(i + 3)
          .withDatedServiceJourneyRef(i) // Reference to replaced
          .create()
      )
      .forEach(createNetexEntitiesIndex::addJourneys);

    ValidationReport validationReport = runValidation(
      createNetexEntitiesIndex.create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void testServiceAlterationMissing() {
    NetexTestFragment testData = new NetexTestFragment();

    DatedServiceJourney datedServiceJourneyReplaced = testData
      .datedServiceJourney(1)
      .withId(1)
      .create();

    DatedServiceJourney datedServiceJourneyNew = testData
      .datedServiceJourney(2)
      .withId(2)
      .withDatedServiceJourneyRef(1) // Reference to replaced
      .create();

    ValidationReport validationReport = runValidation(
      testData
        .netexEntitiesIndex()
        .addJourneys(datedServiceJourneyReplaced, datedServiceJourneyNew)
        .create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
    assertTrue(
      validationReport
        .getValidationReportEntries()
        .stream()
        .map(ValidationReportEntry::getName)
        .allMatch(
          InvalidServiceAlterationError.RuleCode.INVALID_SERVICE_ALTERATION.name()::equals
        )
    );
  }

  @Test
  void testServiceAlterationMissingForMultipleDSJs() {
    NetexTestFragment testData = new NetexTestFragment();

    NetexTestFragment.CreateNetexEntitiesIndex createNetexEntitiesIndex =
      testData.netexEntitiesIndex();

    IntStream
      .of(1, 2, 3)
      .mapToObj(i -> testData.datedServiceJourney(i).withId(i).create())
      .forEach(createNetexEntitiesIndex::addJourneys);

    IntStream
      .of(1, 2, 3)
      .mapToObj(i ->
        testData
          .datedServiceJourney(i + 3)
          .withId(i + 3)
          .withDatedServiceJourneyRef(i) // Reference to replaced
          .create()
      )
      .forEach(createNetexEntitiesIndex::addJourneys);

    ValidationReport validationReport = runValidation(
      createNetexEntitiesIndex.create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(3));
    assertTrue(
      validationReport
        .getValidationReportEntries()
        .stream()
        .map(ValidationReportEntry::getName)
        .allMatch(
          InvalidServiceAlterationError.RuleCode.INVALID_SERVICE_ALTERATION.name()::equals
        )
    );
  }

  @Test
  void testUnexpectedServiceAlteration() {
    NetexTestFragment testData = new NetexTestFragment();

    DatedServiceJourney datedServiceJourneyReplaced = testData
      .datedServiceJourney(1)
      .withId(1)
      .withServiceAlteration(ServiceAlterationEnumeration.CANCELLATION)
      .create();

    DatedServiceJourney datedServiceJourneyNew = testData
      .datedServiceJourney(2)
      .withId(2)
      .withDatedServiceJourneyRef(1) // Reference to replaced
      .create();

    ValidationReport validationReport = runValidation(
      testData
        .netexEntitiesIndex()
        .addJourneys(datedServiceJourneyReplaced, datedServiceJourneyNew)
        .create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
    assertTrue(
      validationReport
        .getValidationReportEntries()
        .stream()
        .map(ValidationReportEntry::getName)
        .allMatch(
          InvalidServiceAlterationError.RuleCode.INVALID_SERVICE_ALTERATION.name()::equals
        )
    );
  }
}
