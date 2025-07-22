package no.entur.antu.validation.validator.interchange.refs;

import java.util.List;
import java.util.Map;
import java.util.Set;
import no.entur.antu.common.netex.NetexTestDataSample;
import no.entur.antu.common.repository.TestNetexDataRepository;
import org.entur.netex.validation.validator.SimpleValidationEntryFactory;
import org.entur.netex.validation.validator.ValidationIssue;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.model.ServiceJourneyId;
import org.entur.netex.validation.validator.model.ServiceJourneyInterchangeInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class InterchangeServiceJourneyReferencesExistValidatorTest {

  private final ServiceJourneyId fromJourneyRef = ServiceJourneyId.ofValidId(
    "TST:ServiceJourney:1"
  );
  private final ServiceJourneyId toJourneyRef = ServiceJourneyId.ofValidId(
    "TST:ServiceJourney:2"
  );

  @Test
  void testValidateServiceJourneyRefsExistsWhenBothExist() {
    ServiceJourneyInterchangeInfo interchange =
      createServiceJourneyInterchangeInfo(fromJourneyRef, toJourneyRef);

    List<ValidationIssue> validationIssues =
      InterchangeServiceJourneyReferencesExistValidator.validateServiceJourneyRefsExists(
        interchange,
        Set.of(fromJourneyRef, toJourneyRef)
      );

    Assertions.assertEquals(0, validationIssues.size());
  }

  @Test
  void testValidateServiceJourneyRefsExistsWhenOnlyFromJourneyExists() {
    ServiceJourneyInterchangeInfo interchange =
      createServiceJourneyInterchangeInfo(fromJourneyRef, toJourneyRef);

    List<ValidationIssue> validationIssues =
      InterchangeServiceJourneyReferencesExistValidator.validateServiceJourneyRefsExists(
        interchange,
        Set.of(fromJourneyRef)
      );

    Assertions.assertEquals(1, validationIssues.size());
    ValidationIssue validationIssue = validationIssues.get(0);
    Assertions.assertEquals(toJourneyRef.id(), validationIssue.arguments()[1]);
  }

  @Test
  void testValidateServiceJourneyRefsExistsWhenOnlyToJourneyExists() {
    ServiceJourneyInterchangeInfo interchange =
      createServiceJourneyInterchangeInfo(fromJourneyRef, toJourneyRef);

    List<ValidationIssue> validationIssues =
      InterchangeServiceJourneyReferencesExistValidator.validateServiceJourneyRefsExists(
        interchange,
        Set.of(toJourneyRef)
      );

    Assertions.assertEquals(1, validationIssues.size());
    ValidationIssue validationIssue = validationIssues.get(0);
    Assertions.assertEquals(
      fromJourneyRef.id(),
      validationIssue.arguments()[1]
    );
  }

  @Test
  void testValidateServiceJourneyRefsExistsWhenNeitherExist() {
    ServiceJourneyInterchangeInfo interchange =
      createServiceJourneyInterchangeInfo(fromJourneyRef, toJourneyRef);

    List<ValidationIssue> validationIssues =
      InterchangeServiceJourneyReferencesExistValidator.validateServiceJourneyRefsExists(
        interchange,
        Set.of()
      );

    Assertions.assertEquals(2, validationIssues.size());
  }

  @Test
  void testValidateAppendsValidationIssuesToReport() {
    String validationReportId = "test";

    TestNetexDataRepository testNetexDataRepository =
      new TestNetexDataRepository();
    testNetexDataRepository.addServiceJourneyInterchangeInfo(
      validationReportId,
      createServiceJourneyInterchangeInfo(fromJourneyRef, toJourneyRef)
    );
    testNetexDataRepository.putServiceJourneyStop(validationReportId, Map.of());

    ValidationReport validationReport = new ValidationReport(
      "tst",
      validationReportId
    );
    InterchangeServiceJourneyReferencesExistValidator validator =
      new InterchangeServiceJourneyReferencesExistValidator(
        new SimpleValidationEntryFactory(),
        testNetexDataRepository
      );

    validator.validate(validationReport);
    Assertions.assertEquals(
      2,
      validationReport.getValidationReportEntries().size()
    );
  }

  @Test
  void testCreateValidationIssue() {
    ServiceJourneyInterchangeInfo interchange =
      createServiceJourneyInterchangeInfo(fromJourneyRef, toJourneyRef);

    ValidationIssue issue =
      InterchangeServiceJourneyReferencesExistValidator.createValidationIssue(
        interchange,
        fromJourneyRef.id()
      );

    Assertions.assertEquals(
      InterchangeServiceJourneyReferencesExistValidator.RULE_NON_EXISTING_SERVICE_JOURNEY_REF,
      issue.rule()
    );
  }

  private ServiceJourneyInterchangeInfo createServiceJourneyInterchangeInfo(
    ServiceJourneyId fromJourneyRef,
    ServiceJourneyId toJourneyRef
  ) {
    return ServiceJourneyInterchangeInfo.of(
      "",
      NetexTestDataSample.serviceJourneyInterchangeWithServiceJourneyRefs(
        fromJourneyRef.id(),
        toJourneyRef.id()
      )
    );
  }
}
