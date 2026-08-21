package no.entur.antu.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.entur.netex.validation.validator.Severity;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.junit.jupiter.api.Test;

class NetexProfileVersionValidatorTest {

  private final NetexProfileVersionValidator validator =
    new NetexProfileVersionValidator();

  @Test
  void aVersionFromTheNetexJavaModelEnumIsAccepted() {
    byte[] content = publicationDelivery("1.08:NO-NeTEx-networktimetable:1.1");

    assertEquals(Optional.empty(), validator.validate("netex.xml", content));
  }

  @Test
  void theCurrentNordicProfileVersionIsAccepted() {
    byte[] content = publicationDelivery("1.15:NO-NeTEx-networktimetable:1.5");

    assertEquals(Optional.empty(), validator.validate("netex.xml", content));
  }

  @Test
  void anUnknownMajorVersionIsRejected() {
    byte[] content = publicationDelivery("2:NO-NeTEx-networktimetable:2");

    Optional<ValidationReportEntry> entry = validator.validate(
      "netex.xml",
      content
    );

    assertTrue(entry.isPresent());
    assertEquals("UNSUPPORTED_NETEX_VERSION", entry.get().getName());
    assertEquals(Severity.CRITICAL, entry.get().getSeverity());
    assertEquals("netex.xml", entry.get().getFileName());
    assertTrue(
      entry.get().getMessage().contains("2:NO-NeTEx-networktimetable:2")
    );
  }

  /**
   * "1.5" was never a published NeTEx schema version (versions go ...1.04, 1.07, 1.08 ... 1.15).
   * It isn't in the allowlist, so it's rejected — unlike under a numeric "greater than 1.16"
   * ceiling, which would have accepted it because 1.5 sorts below 1.16 as a decimal.
   */
  @Test
  void aNeverPublishedVersionIsRejectedEvenThoughItWouldSortBelowTheHighestKnownVersionNumerically() {
    byte[] content = publicationDelivery("1.5:NO-NeTEx-networktimetable:1.0");

    Optional<ValidationReportEntry> entry = validator.validate(
      "netex.xml",
      content
    );

    assertTrue(entry.isPresent());
    assertEquals("UNSUPPORTED_NETEX_VERSION", entry.get().getName());
  }

  @Test
  void aFileWithNoRecognizableVersionAttributeIsLeftToOtherValidators() {
    byte[] content = "<PublicationDelivery/>".getBytes();

    assertEquals(Optional.empty(), validator.validate("netex.xml", content));
  }

  /**
   * A version attribute is present but doesn't match the
   * netexSchemaVersion:profileName:profileVersion grammar, so there's no schema-version segment to
   * look up — treated the same as an unsupported version, not left to other validators. A version
   * that can't be read is not a version Antu can claim to support, even when the string happens to
   * name a supported one. Only a file with no version attribute at all is accepted.
   */
  @Test
  void aVersionAttributeWithoutTheThreeColonSegmentsIsRejected() {
    byte[] content = publicationDelivery("1.15");

    Optional<ValidationReportEntry> entry = validator.validate(
      "netex.xml",
      content
    );

    assertTrue(entry.isPresent());
    assertEquals("UNSUPPORTED_NETEX_VERSION", entry.get().getName());
  }

  /**
   * The same rejection, for a value that names no NeTEx version at all rather than one in the wrong
   * shape.
   */
  @Test
  void aVersionAttributeThatNamesNoNetexVersionIsRejected() {
    byte[] content = publicationDelivery("any");

    Optional<ValidationReportEntry> entry = validator.validate(
      "netex.xml",
      content
    );

    assertTrue(entry.isPresent());
    assertEquals("UNSUPPORTED_NETEX_VERSION", entry.get().getName());
  }

  private static byte[] publicationDelivery(String version) {
    return (
      "<PublicationDelivery version=\"" + version + "\"></PublicationDelivery>"
    ).getBytes();
  }
}
