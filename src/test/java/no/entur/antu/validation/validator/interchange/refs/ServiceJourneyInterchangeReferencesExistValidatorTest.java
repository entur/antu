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

class ServiceJourneyInterchangeReferencesExistValidatorTest {

  private final ServiceJourneyId fromJourneyRef = ServiceJourneyId.ofValidId(
    "TST:ServiceJourney:1"
  );
  private final ServiceJourneyId toJourneyRef = ServiceJourneyId.ofValidId(
    "TST:ServiceJourney:2"
  );
  private final String fromStopPointRef = "TST:ScheduledStopPoint:1";
  private final String toStopPointRef = "TST:ScheduledStopPoint:2";

  @Test
  void testValidateScheduledStopPointRefsExistsWhenBothExist() {
    ServiceJourneyInterchangeInfo interchange =
      createServiceJourneyInterchangeInfo();

    List<ValidationIssue> validationIssues =
      ServiceJourneyInterchangeReferencesExistValidator.validateStopPointRefsExists(
        interchange,
        Set.of(fromStopPointRef, toStopPointRef)
      );

    Assertions.assertEquals(0, validationIssues.size());
  }

  @Test
  void testValidateScheduledStopPointRefsExistsWhenOnlyFromPointRefExists() {
    ServiceJourneyInterchangeInfo interchange =
      createServiceJourneyInterchangeInfo();

    List<ValidationIssue> validationIssues =
      ServiceJourneyInterchangeReferencesExistValidator.validateStopPointRefsExists(
        interchange,
        Set.of(fromStopPointRef)
      );

    Assertions.assertEquals(1, validationIssues.size());
  }

  @Test
  void testValidateScheduledStopPointRefsExistsWhenOnlyToPointRefExists() {
    ServiceJourneyInterchangeInfo interchange =
      createServiceJourneyInterchangeInfo();

    List<ValidationIssue> validationIssues =
      ServiceJourneyInterchangeReferencesExistValidator.validateStopPointRefsExists(
        interchange,
        Set.of(toStopPointRef)
      );

    Assertions.assertEquals(1, validationIssues.size());
  }

  @Test
  void testValidateScheduledStopPointRefsExistsWhenNeitherPointRefExists() {
    ServiceJourneyInterchangeInfo interchange =
      createServiceJourneyInterchangeInfo();

    List<ValidationIssue> validationIssues =
      ServiceJourneyInterchangeReferencesExistValidator.validateStopPointRefsExists(
        interchange,
        Set.of()
      );

    Assertions.assertEquals(2, validationIssues.size());
  }

  @Test
  void testValidateServiceJourneyRefsExistsWhenBothExist() {
    ServiceJourneyInterchangeInfo interchange =
      createServiceJourneyInterchangeInfo();

    List<ValidationIssue> validationIssues =
      ServiceJourneyInterchangeReferencesExistValidator.validateServiceJourneyRefsExists(
        interchange,
        Set.of(fromJourneyRef, toJourneyRef)
      );

    Assertions.assertEquals(0, validationIssues.size());
  }

  @Test
  void testValidateServiceJourneyRefsExistsWhenOnlyFromJourneyExists() {
    ServiceJourneyInterchangeInfo interchange =
      createServiceJourneyInterchangeInfo();

    List<ValidationIssue> validationIssues =
      ServiceJourneyInterchangeReferencesExistValidator.validateServiceJourneyRefsExists(
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
      createServiceJourneyInterchangeInfo();

    List<ValidationIssue> validationIssues =
      ServiceJourneyInterchangeReferencesExistValidator.validateServiceJourneyRefsExists(
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
      createServiceJourneyInterchangeInfo();

    List<ValidationIssue> validationIssues =
      ServiceJourneyInterchangeReferencesExistValidator.validateServiceJourneyRefsExists(
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
      createServiceJourneyInterchangeInfo()
    );
    testNetexDataRepository.putServiceJourneyStop(validationReportId, Map.of());

    ValidationReport validationReport = new ValidationReport(
      "tst",
      validationReportId
    );
    ServiceJourneyInterchangeReferencesExistValidator validator =
      new ServiceJourneyInterchangeReferencesExistValidator(
        new SimpleValidationEntryFactory(),
        testNetexDataRepository
      );

    validator.validate(validationReport);
    Assertions.assertEquals(
      4,
      validationReport.getValidationReportEntries().size()
    );
  }

  @Test
  void testCreateValidationIssueOnMissingServiceJourney() {
    ServiceJourneyInterchangeInfo interchange =
      createServiceJourneyInterchangeInfo();

    ValidationIssue issue =
      ServiceJourneyInterchangeReferencesExistValidator.createValidationIssueOnMissingServiceJourney(
        interchange,
        fromJourneyRef.id()
      );

    Assertions.assertEquals(
      ServiceJourneyInterchangeReferencesExistValidator.RULE_NON_EXISTING_SERVICE_JOURNEY_REF,
      issue.rule()
    );
  }

  @Test
  void testCreateValidationIssueOnMissingStopPoint() {
    ServiceJourneyInterchangeInfo interchange =
      createServiceJourneyInterchangeInfo();

    ValidationIssue issue =
      ServiceJourneyInterchangeReferencesExistValidator.createValidationIssueOnMissingStopPoint(
        interchange,
        fromStopPointRef
      );

    Assertions.assertEquals(
      ServiceJourneyInterchangeReferencesExistValidator.RULE_NON_EXISTING_STOP_POINT_REF,
      issue.rule()
    );
  }

  private ServiceJourneyInterchangeInfo createServiceJourneyInterchangeInfo() {
    return ServiceJourneyInterchangeInfo.of(
      "",
      NetexTestDataSample.serviceJourneyInterchangeWithServiceJourneyRefs(
        fromJourneyRef.id(),
        toJourneyRef.id(),
        fromStopPointRef,
        toStopPointRef
      )
    );
  }
}
