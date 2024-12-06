package no.entur.antu.validation.validator.servicejourney.servicealteration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.stream.IntStream;
import no.entur.antu.netextestdata.NetexEntitiesTestFactory;
import no.entur.antu.validation.ValidationTest;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.junit.jupiter.api.Test;
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

  private NetexEntitiesTestFactory.CreateDatedServiceJourney datedServiceJourneyDraft(
    int id,
    NetexEntitiesTestFactory testData
  ) {
    return testData.createDatedServiceJourney(
      id,
      testData.createServiceJourney(id, testData.createJourneyPattern(id)),
      testData.createOperatingDay(id, LocalDate.parse("2024-12-01"))
    );
  }

  @Test
  void testCorrectReplacementExists() {
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

    NetexEntitiesTestFactory.CreateDatedServiceJourney datedServiceJourneyReplaced =
      datedServiceJourneyDraft(1, netexEntitiesTestFactory)
        .withServiceAlteration(ServiceAlterationEnumeration.REPLACED);

    datedServiceJourneyDraft(2, netexEntitiesTestFactory)
      .withDatedServiceJourneyRef(datedServiceJourneyReplaced); // Reference to replaced

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void testReplacementDoesNotExists() {
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

    datedServiceJourneyDraft(1, netexEntitiesTestFactory)
      .withServiceAlteration(ServiceAlterationEnumeration.REPLACED);

    datedServiceJourneyDraft(2, netexEntitiesTestFactory);

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.create()
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
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

    IntStream
      .of(1, 2, 3)
      .forEach(i ->
        datedServiceJourneyDraft(i, netexEntitiesTestFactory)
          .withServiceAlteration(ServiceAlterationEnumeration.REPLACED)
      );

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.create()
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
