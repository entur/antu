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

class MissingReplacementValidatorTest extends ValidationTest {

  private ValidationReport runValidation(
    NetexEntitiesIndex netexEntitiesIndex
  ) {
    return runValidationOnLineFile(
      netexEntitiesIndex,
      MissingReplacementValidator.class
    );
  }

  @Test
  void testCorrectReplacementExists() {
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
  void testReplacementDoesNotExists() {
    NetexTestFragment testData = new NetexTestFragment();

    DatedServiceJourney datedServiceJourneyReplaced = testData
      .datedServiceJourney(1)
      .withId(1)
      .withServiceAlteration(ServiceAlterationEnumeration.REPLACED)
      .create();

    DatedServiceJourney datedServiceJourneyNew = testData
      .datedServiceJourney(2)
      .withId(2)
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
          MissingReplacementError.RuleCode.MISSING_REPLACEMENT.name()::equals
        )
    );
  }

  @Test
  void testReplacementMissingForMultipleDSJs() {
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
          MissingReplacementError.RuleCode.MISSING_REPLACEMENT.name()::equals
        )
    );
  }
}
