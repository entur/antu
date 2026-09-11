package no.entur.antu.validation.validator.servicejourney.servicealteration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.stream.IntStream;
import no.entur.antu.netex.test.NetexTestData;
import no.entur.antu.netex.test.ValidatorTestBase;
import no.entur.antu.netex.test.builder.DatedServiceJourneyBuilder;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.ServiceAlterationEnumeration;

class MissingReplacementValidatorTest extends ValidatorTestBase {

  private ValidationReport runValidation(
    NetexEntitiesIndex netexEntitiesIndex
  ) {
    return runValidationOnLineFile(
      netexEntitiesIndex,
      new MissingReplacementValidator()
    );
  }

  private DatedServiceJourneyBuilder datedServiceJourneyDraft(
    int id,
    NetexTestData testData
  ) {
    return testData.addDatedServiceJourney(
      id,
      testData.addServiceJourney(id, testData.addJourneyPattern(id)),
      testData.operatingDay(id, LocalDate.parse("2024-12-01"))
    );
  }

  @Test
  void testCorrectReplacementExists() {
    NetexTestData netexEntitiesTestFactory = new NetexTestData();

    DatedServiceJourneyBuilder datedServiceJourneyReplaced =
      datedServiceJourneyDraft(1, netexEntitiesTestFactory)
        .withServiceAlteration(ServiceAlterationEnumeration.REPLACED);

    datedServiceJourneyDraft(2, netexEntitiesTestFactory)
      .withDatedServiceJourneyRef(datedServiceJourneyReplaced); // Reference to replaced

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.build()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void testReplacementDoesNotExists() {
    NetexTestData netexEntitiesTestFactory = new NetexTestData();

    datedServiceJourneyDraft(1, netexEntitiesTestFactory)
      .withServiceAlteration(ServiceAlterationEnumeration.REPLACED);

    datedServiceJourneyDraft(2, netexEntitiesTestFactory);

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.build()
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
    NetexTestData netexEntitiesTestFactory = new NetexTestData();

    IntStream
      .of(1, 2, 3)
      .forEach(i ->
        datedServiceJourneyDraft(i, netexEntitiesTestFactory)
          .withServiceAlteration(ServiceAlterationEnumeration.REPLACED)
      );

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.build()
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
