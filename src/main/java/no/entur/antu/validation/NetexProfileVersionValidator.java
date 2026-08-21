package no.entur.antu.validation;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.entur.netex.validation.validator.DataLocation;
import org.entur.netex.validation.validator.Severity;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.entur.netex.validation.xml.PublicationDeliveryVersionAttributeReader;
import org.rutebanken.netex.validation.NeTExValidator.NetexVersion;

/**
 * Rejects a NeTEx file outright when its PublicationDelivery declares a NeTEx schema version
 * Antu doesn't recognize, before any schema, XPath or JAXB validator runs against it.
 *
 * <p>The known versions are seeded entirely from {@link NetexVersion}, the single source of
 * truth netex-java-model itself uses to select an XML schema.
 */
public class NetexProfileVersionValidator {

  static final String RULE_CODE = "UNSUPPORTED_NETEX_VERSION";

  static final Set<String> SUPPORTED_NETEX_SCHEMA_VERSIONS = Arrays
    .stream(NetexVersion.values())
    .map(NetexVersion::toString)
    .collect(Collectors.toUnmodifiableSet());

  /**
   * The version attribute is read in the {@code netexSchemaVersion:profileName:profileVersion} form
   * the Nordic profile declares, e.g. "1.15:NO-NeTEx-networktimetable:1.5", and only its schema
   * version segment is compared against {@link #SUPPORTED_NETEX_SCHEMA_VERSIONS}.
   *
   * Exactly two things are accepted, and they are not the same case:
   * - a declared schema version that is on that list;
   * - a file that declares <em>no</em> version attribute at all. There is nothing to compare,
   *   and schema validation falls back to its own latest known version, so the file is left to
   *   the other validators rather than reported.
   *
   * @return a CRITICAL report entry for a version Antu does not support, or empty if the file is
   * left to the other validators.
   */
  public Optional<ValidationReportEntry> validate(
    String fileName,
    byte[] fileContent
  ) {
    String rawVersion =
      PublicationDeliveryVersionAttributeReader.findPublicationDeliveryVersion(
        fileContent
      );
    if (rawVersion == null) {
      return Optional.empty();
    }

    Optional<String> netexSchemaVersion = schemaVersionSegment(rawVersion);
    if (
      netexSchemaVersion.isPresent() &&
      SUPPORTED_NETEX_SCHEMA_VERSIONS.contains(netexSchemaVersion.get())
    ) {
      return Optional.empty();
    }

    return Optional.of(
      new ValidationReportEntry(
        "NeTEx profile version " + rawVersion + " is not supported by Antu",
        RULE_CODE,
        Severity.CRITICAL,
        new DataLocation(null, fileName, null, null)
      )
    );
  }

  /**
   * The PublicationDelivery version attribute has the form
   * {netexSchemaVersion}:{profileName}:{profileVersion}, e.g.
   * "1.15:NO-NeTEx-networktimetable:1.5". Only the first segment is the NeTEx schema version.
   * Empty if {@code rawVersion} doesn't match that 3-segment grammar - the same split
   * {@code NetexSchemaRepository.getSchemaVersion} applies, though it treats a value it can't read
   * as a reason to fall back to the latest schema rather than as a reason to reject the file.
   */
  private static Optional<String> schemaVersionSegment(String rawVersion) {
    String[] segments = rawVersion.split(":");
    return segments.length == 3 ? Optional.of(segments[0]) : Optional.empty();
  }
}
