package no.entur.antu.pipeline;

import static no.entur.antu.validation.ValidationProfile.TIMETABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import no.entur.antu.job.ValidationContext;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.junit.jupiter.api.Test;

/**
 * A dataset holds several NeTEx files, and only some of them may declare a NeTEx version Antu does
 * not support. What each of the others reports depends on whether the rejected file was a common
 * file: a rejected line file concerns only itself, while a rejected common file takes the shared
 * data every line file resolves its references against with it.
 */
class UnsupportedNetexVersionDatasetTest extends AntuPipelineTestBase {

  private static final String CODESPACE_FLB = "flb";
  private static final String DATASET = "rb_flb-aggregated-netex.zip";
  private static final String COMMON_FILE = "_FLB_shared_data.xml";
  private static final String LINE_FILE = "FLB_FLB-Line-42_42_Flamsbana.xml";
  private static final String UNSUPPORTED_NETEX_VERSION =
    "UNSUPPORTED_NETEX_VERSION";

  private static final String UNSUPPORTED_VERSION_ATTRIBUTE =
    "2:NO-NeTEx-networktimetable:2";

  /**
   * The version attribute on the root element, as opposed to the {@code version="1.0"} of the XML
   * declaration on the line above it.
   */
  private static final Pattern PUBLICATION_DELIVERY_VERSION = Pattern.compile(
    "(<PublicationDelivery\\b[^>]*?)version=\"[^\"]*\""
  );

  /**
   * The rejection is attributed to the file that declared the unsupported version, and the rest of
   * the dataset is validated as usual - here the common file still reports its own findings.
   */
  @Test
  void aRejectedLineFileDoesNotAffectTheOtherFiles() throws Exception {
    ValidationReport report = validateDatasetWithUnsupportedVersionIn(
      LINE_FILE
    );

    assertEquals(
      List.of(UNSUPPORTED_NETEX_VERSION),
      ruleNamesFor(report, LINE_FILE)
    );
    assertFalse(
      entriesFor(report, COMMON_FILE).isEmpty(),
      "The common file declares a supported version and should have been validated normally"
    );
    assertFalse(
      ruleNamesFor(report, COMMON_FILE).contains(UNSUPPORTED_NETEX_VERSION)
    );
  }

  /**
   * A rejected common file is never parsed, so every reference a line file makes into the shared
   * data would be reported as unresolved. Those findings are consequences of the rejection and say
   * nothing about the line file, so the line files skip the NeTEx validators - the same suppression
   * a common file with a schema error triggers.
   */
  @Test
  void aRejectedCommonFileSuppressesTheLineFileFindings() throws Exception {
    ValidationReport report = validateDatasetWithUnsupportedVersionIn(
      COMMON_FILE
    );

    assertEquals(
      List.of(UNSUPPORTED_NETEX_VERSION),
      ruleNamesFor(report, COMMON_FILE)
    );
    assertEquals(
      List.of(),
      ruleNamesFor(report, LINE_FILE),
      "The line file should report nothing once its common file was rejected"
    );
    assertTrue(report.hasError());
  }

  private ValidationReport validateDatasetWithUnsupportedVersionIn(
    String fileName
  ) throws Exception {
    String datasetBlobName = uploadTestDataset(
      CODESPACE_FLB,
      "rb_flb-unsupported-version-in-" + fileName + ".zip",
      new ByteArrayInputStream(datasetWithUnsupportedVersionIn(fileName))
    );
    ValidationContext context = validate(
      CODESPACE_FLB,
      datasetBlobName,
      TIMETABLE.id()
    );
    return publishedReport(context);
  }

  /**
   * Both variants of this dataset are one attribute away from the dataset every other pipeline test
   * uses, so they are assembled here instead of being checked in as two more near-identical zips.
   * What makes a file unsupported then stays readable, rather than being buried in a binary.
   */
  private static byte[] datasetWithUnsupportedVersionIn(String fileName)
    throws IOException {
    ByteArrayOutputStream rewrittenDataset = new ByteArrayOutputStream();
    InputStream dataset =
      UnsupportedNetexVersionDatasetTest.class.getResourceAsStream(
          '/' + DATASET
        );
    assertNotNull(dataset, "Test dataset file not found: " + DATASET);
    boolean rewroteAFile = false;
    try (
      ZipInputStream source = new ZipInputStream(dataset);
      ZipOutputStream target = new ZipOutputStream(rewrittenDataset)
    ) {
      for (ZipEntry entry; (entry = source.getNextEntry()) != null;) {
        byte[] netexFile = source.readAllBytes();
        boolean rewriteThisFile = fileName.equals(entry.getName());
        rewroteAFile |= rewriteThisFile;
        target.putNextEntry(new ZipEntry(entry.getName()));
        target.write(
          rewriteThisFile ? withUnsupportedVersion(netexFile) : netexFile
        );
        target.closeEntry();
      }
    }
    assertTrue(rewroteAFile, DATASET + " holds no file named " + fileName);
    return rewrittenDataset.toByteArray();
  }

  private static byte[] withUnsupportedVersion(byte[] netexFile) {
    Matcher versionAttribute = PUBLICATION_DELIVERY_VERSION.matcher(
      new String(netexFile, StandardCharsets.UTF_8)
    );
    assertTrue(
      versionAttribute.find(),
      "The PublicationDelivery declares no version attribute to make unsupported"
    );
    return versionAttribute
      .replaceFirst("$1version=\"" + UNSUPPORTED_VERSION_ATTRIBUTE + "\"")
      .getBytes(StandardCharsets.UTF_8);
  }

  private static List<ValidationReportEntry> entriesFor(
    ValidationReport report,
    String fileName
  ) {
    return report
      .getValidationReportEntries()
      .stream()
      .filter(entry -> fileName.equals(entry.getFileName()))
      .toList();
  }

  private static List<String> ruleNamesFor(
    ValidationReport report,
    String fileName
  ) {
    return entriesFor(report, fileName)
      .stream()
      .map(ValidationReportEntry::getName)
      .distinct()
      .toList();
  }
}
