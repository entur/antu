package no.entur.antu.validation.validator.servicejourney.servicealteration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.IntStream;
import no.entur.antu.netextestdata.NetexEntitiesTestFactory;
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
    NetexEntitiesTestFactory testData = new NetexEntitiesTestFactory();

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
        .addDatedServiceJourneys(
          datedServiceJourneyReplaced,
          datedServiceJourneyNew
        )
        .create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void testReplacementDoesNotExists() {
    NetexEntitiesTestFactory testData = new NetexEntitiesTestFactory();

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
        .addDatedServiceJourneys(
          datedServiceJourneyReplaced,
          datedServiceJourneyNew
        )
        .create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
    assertTrue(
      validationReport
        .getValidationReportEntries()
        .stream()
        .map(ValidationReportEntry::getName)
        .allMatch(MissingReplacementValidator.RULE.name()::equals)
    );
  }

  @Test
  void testReplacementMissingForMultipleDSJs() {
    NetexEntitiesTestFactory testData = new NetexEntitiesTestFactory();

    NetexEntitiesTestFactory.CreateNetexEntitiesIndex createNetexEntitiesIndex =
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
      .forEach(createNetexEntitiesIndex::addDatedServiceJourneys);

    ValidationReport validationReport = runValidation(
      createNetexEntitiesIndex.create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(3));
    assertTrue(
      validationReport
        .getValidationReportEntries()
        .stream()
        .map(ValidationReportEntry::getName)
        .allMatch(MissingReplacementValidator.RULE.name()::equals)
    );
  }
}
