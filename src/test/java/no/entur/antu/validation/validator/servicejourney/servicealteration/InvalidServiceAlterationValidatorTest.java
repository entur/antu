package no.entur.antu.validation.validator.servicejourney.servicealteration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.stream.IntStream;
import no.entur.antu.netex.test.NetexTestData;
import no.entur.antu.netex.test.ValidatorTestBase;
import no.entur.antu.netex.test.builder.DatedServiceJourneyBuilder;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.ServiceAlterationEnumeration;

class InvalidServiceAlterationValidatorTest extends ValidatorTestBase {

  private ValidationReport runValidation(
    NetexEntitiesIndex netexEntitiesIndex
  ) {
    return runValidationOnLineFile(
      netexEntitiesIndex,
      new InvalidServiceAlterationValidator()
    );
  }

  private DatedServiceJourneyBuilder datedServiceJourneyDraft(
    int id,
    NetexTestData netexEntitiesTestFactory
  ) {
    return netexEntitiesTestFactory.addDatedServiceJourney(
      id,
      netexEntitiesTestFactory.addServiceJourney(
        id,
        netexEntitiesTestFactory.addJourneyPattern(id)
      ),
      netexEntitiesTestFactory.operatingDay(
        id,
        LocalDate.of(2024, Month.DECEMBER, 1)
      )
    );
  }

  @Test
  void testCorrectServiceAlterationExists() {
    NetexTestData netexEntitiesTestFactory = new NetexTestData();

    DatedServiceJourneyBuilder replacedDatedServiceJourney =
      datedServiceJourneyDraft(1, netexEntitiesTestFactory)
        .withServiceAlteration(ServiceAlterationEnumeration.REPLACED);

    datedServiceJourneyDraft(2, netexEntitiesTestFactory)
      .withDatedServiceJourneyRef(replacedDatedServiceJourney); // Reference to replaced;

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.build()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void testCorrectServiceAlterationExistsForMultipleDSJs() {
    NetexTestData netexEntitiesTestFactory = new NetexTestData();

    List<DatedServiceJourneyBuilder> replacedDatedServiceJourneys = IntStream
      .of(1, 2, 3)
      .mapToObj(i ->
        datedServiceJourneyDraft(i, netexEntitiesTestFactory)
          .withServiceAlteration(ServiceAlterationEnumeration.REPLACED)
      )
      .toList();

    IntStream
      .of(1, 2, 3)
      .forEach(i ->
        datedServiceJourneyDraft(i + 3, netexEntitiesTestFactory)
          .withDatedServiceJourneyRef(replacedDatedServiceJourneys.get(i - 1)) // Reference to replaced
      );

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.build()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void testServiceAlterationMissing() {
    NetexTestData netexEntitiesTestFactory = new NetexTestData();

    DatedServiceJourneyBuilder datedServiceJourneyReplaced =
      datedServiceJourneyDraft(1, netexEntitiesTestFactory);

    datedServiceJourneyDraft(2, netexEntitiesTestFactory)
      .withDatedServiceJourneyRef(datedServiceJourneyReplaced); // Reference to replaced

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.build()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
    assertTrue(
      validationReport
        .getValidationReportEntries()
        .stream()
        .map(ValidationReportEntry::getName)
        .allMatch(InvalidServiceAlterationValidator.RULE.name()::equals)
    );
  }

  @Test
  void testServiceAlterationMissingForMultipleDSJs() {
    NetexTestData netexEntitiesTestFactory = new NetexTestData();

    List<DatedServiceJourneyBuilder> datedServiceJourneysWithNoServiceAlteration =
      IntStream
        .of(1, 2, 3)
        .mapToObj(i -> datedServiceJourneyDraft(i, netexEntitiesTestFactory))
        .toList();

    IntStream
      .of(1, 2, 3)
      .forEach(i ->
        datedServiceJourneyDraft(i + 3, netexEntitiesTestFactory)
          .withDatedServiceJourneyRef(
            datedServiceJourneysWithNoServiceAlteration.get(i - 1)
          ) // Reference to replaced
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
        .allMatch(InvalidServiceAlterationValidator.RULE.name()::equals)
    );
  }

  @Test
  void testUnexpectedServiceAlteration() {
    NetexTestData netexEntitiesTestFactory = new NetexTestData();

    DatedServiceJourneyBuilder cancelledDatedServiceJourney =
      datedServiceJourneyDraft(1, netexEntitiesTestFactory)
        .withServiceAlteration(ServiceAlterationEnumeration.CANCELLATION);

    datedServiceJourneyDraft(2, netexEntitiesTestFactory)
      .withDatedServiceJourneyRef(cancelledDatedServiceJourney); // Reference to cancelled

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.build()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(1));
    assertTrue(
      validationReport
        .getValidationReportEntries()
        .stream()
        .map(ValidationReportEntry::getName)
        .allMatch(InvalidServiceAlterationValidator.RULE.name()::equals)
    );
  }
}
