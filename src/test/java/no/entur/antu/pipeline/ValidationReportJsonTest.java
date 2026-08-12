package no.entur.antu.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.format.DateTimeFormatter;
import no.entur.antu.config.JsonConfig;
import org.entur.netex.validation.validator.DataLocation;
import org.entur.netex.validation.validator.Severity;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.junit.jupiter.api.Test;

/**
 * The published report is parsed by kakka, marduk and the operator portal, so its JSON shape is a
 * contract. It used to be produced by Camel's Jackson data format; these tests pin the shape to the
 * mapper that replaced it.
 */
class ValidationReportJsonTest {

  private final ObjectMapper objectMapper = new JsonConfig()
    .validationReportObjectMapper();

  @Test
  void creationDateIsAnIsoLocalDateTimeString() throws Exception {
    ValidationReport report = new ValidationReport("codespace", "reportId");

    JsonNode creationDate = objectMapper
      .readTree(objectMapper.writeValueAsString(report))
      .get("creationDate");

    assertNotNull(creationDate);
    assertTrue(
      creationDate.isTextual(),
      "expected a string, got " + creationDate
    );
    assertEquals(
      DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(report.getCreationDate()),
      creationDate.asText()
    );
  }

  @Test
  void nullFieldsAreLeftOut() throws Exception {
    ValidationReport report = new ValidationReport(null, "reportId");

    String json = objectMapper.writeValueAsString(report);

    assertFalse(json.contains("\"codespace\""), json);
  }

  @Test
  void aReportWithFindingsRoundTrips() throws Exception {
    ValidationReport report = new ValidationReport("codespace", "reportId");
    report.addValidationReportEntry(
      new ValidationReportEntry(
        "something is wrong",
        "RULE_CODE",
        Severity.ERROR,
        new DataLocation("NSR:Quay:1", "line.xml", 12, 3)
      )
    );

    ValidationReport parsed = objectMapper.readValue(
      objectMapper.writeValueAsBytes(report),
      ValidationReport.class
    );

    assertEquals("codespace", parsed.getCodespace());
    assertEquals("reportId", parsed.getValidationReportId());
    assertEquals(report.getCreationDate(), parsed.getCreationDate());
    assertTrue(parsed.hasError());
    assertEquals(1, parsed.getValidationReportEntries().size());
    ValidationReportEntry entry = parsed
      .getValidationReportEntries()
      .iterator()
      .next();
    assertEquals("RULE_CODE", entry.getName());
    assertEquals(Severity.ERROR, entry.getSeverity());
    assertEquals("line.xml", entry.getFileName());
    assertEquals(
      java.util.Map.of("RULE_CODE", 1L),
      parsed.getNumberOfValidationEntriesPerRule()
    );
  }
}
