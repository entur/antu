package no.entur.antu.validation.validator.servicejourney.servicealteration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;
import no.entur.antu.netextestdata.NetexEntitiesTestFactory;
import no.entur.antu.validation.ValidationTest;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.junit.jupiter.api.Test;
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

  private NetexEntitiesTestFactory.CreateDatedServiceJourney datedServiceJourneyDraft(
    int id,
    NetexEntitiesTestFactory netexEntitiesTestFactory
  ) {
    return netexEntitiesTestFactory.createDatedServiceJourney(
      id,
      netexEntitiesTestFactory.createServiceJourney(
        id,
        netexEntitiesTestFactory.createJourneyPattern(id)
      ),
      netexEntitiesTestFactory.createOperatingDay(id, LocalDate.of(2024, 12, 1))
    );
  }

  @Test
  void testCorrectServiceAlterationExists() {
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

    NetexEntitiesTestFactory.CreateDatedServiceJourney replacedDatedServiceJourney =
      datedServiceJourneyDraft(1, netexEntitiesTestFactory)
        .withServiceAlteration(ServiceAlterationEnumeration.REPLACED);

    datedServiceJourneyDraft(2, netexEntitiesTestFactory)
      .withDatedServiceJourneyRef(replacedDatedServiceJourney); // Reference to replaced;

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void testCorrectServiceAlterationExistsForMultipleDSJs() {
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

    List<NetexEntitiesTestFactory.CreateDatedServiceJourney> replacedDatedServiceJourneys =
      IntStream
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
      netexEntitiesTestFactory.create()
    );

    assertThat(validationReport.getValidationReportEntries().size(), is(0));
  }

  @Test
  void testServiceAlterationMissing() {
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

    NetexEntitiesTestFactory.CreateDatedServiceJourney datedServiceJourneyReplaced =
      datedServiceJourneyDraft(1, netexEntitiesTestFactory);

    datedServiceJourneyDraft(2, netexEntitiesTestFactory)
      .withDatedServiceJourneyRef(datedServiceJourneyReplaced); // Reference to replaced

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.create()
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
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

    List<NetexEntitiesTestFactory.CreateDatedServiceJourney> datedServiceJourneysWithNoServiceAlteration =
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
      netexEntitiesTestFactory.create()
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
    NetexEntitiesTestFactory netexEntitiesTestFactory =
      new NetexEntitiesTestFactory();

    NetexEntitiesTestFactory.CreateDatedServiceJourney cancelledDatedServiceJourney =
      datedServiceJourneyDraft(1, netexEntitiesTestFactory)
        .withServiceAlteration(ServiceAlterationEnumeration.CANCELLATION);

    datedServiceJourneyDraft(2, netexEntitiesTestFactory)
      .withDatedServiceJourneyRef(cancelledDatedServiceJourney); // Reference to cancelled

    ValidationReport validationReport = runValidation(
      netexEntitiesTestFactory.create()
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
