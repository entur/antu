package no.entur.antu.pipeline;

import static no.entur.antu.Constants.VALIDATION_CLIENT_KAKKA;
import static no.entur.antu.Constants.VALIDATION_CLIENT_MARDUK;
import static no.entur.antu.validation.ValidationProfile.STOP;
import static no.entur.antu.validation.ValidationProfile.TIMETABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import no.entur.antu.job.ValidationContext;
import no.entur.antu.job.ValidationStatus;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.junit.jupiter.api.Test;

/**
 * Validates the reference datasets from start to finish and checks what the validation client is
 * told and what ends up in the published report.
 */
class DatasetValidationTest extends AntuPipelineTestBase {

  private static final String CODESPACE_FLB = "flb";
  private static final String CODESPACE_AVI = "avi";
  private static final String CODESPACE_NSR = "nsr";

  private static final String DUPLICATED_ID_RULE =
    "NeTEx ID duplicated across files";

  @Test
  void authorityErrorIsReportedAsFailed() throws Exception {
    ValidationContext context = validate(
      CODESPACE_FLB,
      uploadTestDataset(CODESPACE_FLB, "rb_flb-aggregated-netex.zip"),
      TIMETABLE.id()
    );

    assertEquals(
      List.of(ValidationStatus.STARTED, ValidationStatus.FAILED),
      statusNotifier.statuses()
    );
    assertTrue(publishedReport(context).hasError());
  }

  @Test
  void schemaErrorsAreReportedAsFailed() throws Exception {
    ValidationContext context = validate(
      CODESPACE_FLB,
      uploadTestDataset(
        CODESPACE_FLB,
        "rb_flb-aggregated-netex-schema-error.zip"
      ),
      TIMETABLE.id()
    );

    assertEquals(
      List.of(ValidationStatus.STARTED, ValidationStatus.FAILED),
      statusNotifier.statuses()
    );
    assertTrue(publishedReport(context).hasError());
  }

  @Test
  void stopPlaceDatasetIsValidated() throws Exception {
    ValidationContext context = validationInitializer.initValidation(
      ValidationContext
        .builder()
        .referential(CODESPACE_NSR)
        .datasetFileHandle(uploadTestDataset(CODESPACE_NSR, "stopdata.zip"))
        .validationStage(VALIDATION_STAGE_PREVALIDATION)
        .validationClient(VALIDATION_CLIENT_KAKKA)
        .validationProfile(STOP.id())
        .build()
    );

    assertEquals(
      List.of(ValidationStatus.STARTED, ValidationStatus.FAILED),
      statusNotifier.statuses()
    );
    assertTrue(publishedReport(context).hasError());
    assertTrue(
      statusNotifier
        .notifications()
        .stream()
        .allMatch(notification ->
          VALIDATION_CLIENT_KAKKA.equals(
            notification.context().validationClient()
          )
        )
    );
  }

  @Test
  void duplicatedIdAcrossFilesIsDetected() throws Exception {
    ValidationContext context = validate(
      CODESPACE_AVI,
      uploadTestDataset(
        CODESPACE_AVI,
        "rb_avi-aggregated-netex-duplicated-id.zip"
      ),
      TIMETABLE.id()
    );

    assertTrue(hasEntryForRule(publishedReport(context), DUPLICATED_ID_RULE));
  }

  @Test
  void uniqueIdsAcrossFilesAreAccepted() throws Exception {
    ValidationContext context = validate(
      CODESPACE_AVI,
      uploadTestDataset(CODESPACE_AVI, "rb_avi-aggregated-netex.zip"),
      TIMETABLE.id()
    );

    assertFalse(hasEntryForRule(publishedReport(context), DUPLICATED_ID_RULE));
  }

  /**
   * The identity antu derives is what the client uses to fetch the report afterwards, and what marduk
   * matches its pending job against, so every notification has to carry it.
   */
  @Test
  void everyNotificationCarriesTheValidationIdentity() {
    validate(
      CODESPACE_FLB,
      uploadTestDataset(CODESPACE_FLB, "rb_flb-aggregated-netex.zip"),
      TIMETABLE.id()
    );

    assertFalse(statusNotifier.notifications().isEmpty());
    for (var notification : statusNotifier.notifications()) {
      ValidationContext context = notification.context();
      assertEquals(CODESPACE_FLB, context.referential());
      assertEquals(CODESPACE_FLB, context.codespace());
      assertNotNull(context.validationReportId());
      assertNotNull(context.correlationId());
      assertEquals(VALIDATION_STAGE_PREVALIDATION, context.validationStage());
      assertEquals(VALIDATION_CLIENT_MARDUK, context.validationClient());
    }
  }

  /**
   * The referential is what the report is filed under, the codespace is what the NeTEx data declares.
   */
  @Test
  void codespaceDropsTheReferentialPrefix() {
    ValidationContext context = validate(
      "rb_flb",
      uploadTestDataset(CODESPACE_FLB, "rb_flb-aggregated-netex.zip"),
      TIMETABLE.id()
    );

    assertEquals("rb_flb", context.referential());
    assertEquals(CODESPACE_FLB, context.codespace());
    assertTrue(context.validationReportId().startsWith("rb_flb_"));
  }

  private static boolean hasEntryForRule(
    ValidationReport report,
    String ruleName
  ) {
    return report
      .getValidationReportEntries()
      .stream()
      .map(ValidationReportEntry::getName)
      .anyMatch(ruleName::equals);
  }
}
