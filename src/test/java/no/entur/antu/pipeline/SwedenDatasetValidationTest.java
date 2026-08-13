package no.entur.antu.pipeline;

import static no.entur.antu.validation.ValidationProfile.TIMETABLE_SWEDEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import no.entur.antu.job.ValidationContext;
import no.entur.antu.job.ValidationStatus;
import org.entur.netex.validation.validator.ValidationReport;
import org.junit.jupiter.api.Test;

class SwedenDatasetValidationTest extends AntuPipelineTestBase {

  private static final String CODESPACE_SAM = "SAM";

  @Test
  void swedishDatasetPassesValidation() throws Exception {
    ValidationContext context = validate(
      CODESPACE_SAM,
      uploadTestDataset(CODESPACE_SAM, "varmland.zip"),
      TIMETABLE_SWEDEN.id()
    );

    ValidationReport report = publishedReport(context);
    assertEquals(
      List.of(ValidationStatus.STARTED, ValidationStatus.OK),
      statusNotifier.statuses(),
      () -> "findings: " + describe(report)
    );
    assertFalse(report.hasError(), () -> "findings: " + describe(report));
  }

  private static String describe(ValidationReport report) {
    return report
      .getValidationReportEntries()
      .stream()
      .map(entry ->
        entry.getSeverity() + " " + entry.getName() + " " + entry.getMessage()
      )
      .distinct()
      .collect(java.util.stream.Collectors.joining("; "));
  }
}
