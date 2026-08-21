package no.entur.antu.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.entur.netex.validation.validator.Severity;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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

  @Test
  void aFileWithNoRecognizableVersionAttributeIsLeftToOtherValidators() {
    byte[] content = "<PublicationDelivery/>".getBytes();

    assertEquals(Optional.empty(), validator.validate("netex.xml", content));
  }

  /**
   * The ways a declared version fails, which all end in the same rejection: a schema version that
   * isn't in the allowlist, and a value the schema version can't be read out of at all. What the
   * entry itself looks like is asserted once, in
   * {@link #anUnknownMajorVersionIsRejected()}.
   */
  @ParameterizedTest
  @ValueSource(
    strings = {
      // Never a published NeTEx schema version (they go ...1.04, 1.07, 1.08 ... 1.15). It sorts
      // below the highest known version as a decimal, so a numeric ceiling would have let it
      // through; the allowlist doesn't.
      "1.5:NO-NeTEx-networktimetable:1.0",
      // A supported schema version, but with no profile segments to make it the three-segment form,
      // so there is no schema version segment to look up. A version that can't be read out is not
      // one Antu can claim to support, even when the string happens to name one that is.
      "1.15",
      // Names no NeTEx version at all.
      "any",
    }
  )
  void anUnsupportedOrUnreadableVersionIsRejected(String versionAttribute) {
    byte[] content = publicationDelivery(versionAttribute);

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
