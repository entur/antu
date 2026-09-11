package no.entur.antu.validation.validator.xpath.rules;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmNode;
import org.entur.netex.validation.validator.Severity;
import org.entur.netex.validation.validator.ValidationIssue;
import org.entur.netex.validation.validator.ValidationRule;
import org.entur.netex.validation.validator.xpath.AbstractXPathValidationRule;
import org.entur.netex.validation.validator.xpath.XPathRuleValidationContext;
import org.rutebanken.netex.validation.NeTExValidator;

/**
 * Validate that the NeTEx schema version the PublicationDelivery declares is one there is a schema
 * for. A file declaring a version no schema exists for is still validated: {@code
 * NetexSchemaValidator} falls back to {@link NeTExValidator#LATEST}.
 */
public class ValidateSupportedNetexVersion extends AbstractXPathValidationRule {

  static final ValidationRule RULE = new ValidationRule(
    "UNSUPPORTED_NETEX_VERSION",
    "Unsupported NeTEx version",
    "NeTEx version %s is not supported. The most recent supported NeTEx version is %s",
    Severity.CRITICAL
  );

  private static final QName VERSION_ATTRIBUTE = new QName("version");

  /**
   * The NeTEx schema versions a file may declare, taken from the schemas netex-java-model ships.
   * The enum value names the schema folder rather than the version a file declares, and the two
   * differ for the oldest one - the {@code 1.04beta} schema is declared as {@code 1.04} - so only
   * the numeric part is kept.
   */
  private static final Set<String> AVAILABLE_SCHEMA_VERSIONS = Arrays
    .stream(NeTExValidator.NetexVersion.values())
    .map(netexVersion -> netexVersion.toString().replaceAll("[^0-9.]", ""))
    .collect(Collectors.toUnmodifiableSet());

  @Override
  public List<ValidationIssue> validate(
    XPathRuleValidationContext validationContext
  ) {
    Objects.requireNonNull(validationContext);
    XdmNode publicationDelivery = validationContext.getXmlNode();
    String declaredSchemaVersion = declaredSchemaVersion(
      publicationDelivery.getAttributeValue(VERSION_ATTRIBUTE)
    );
    if (
      declaredSchemaVersion == null ||
      AVAILABLE_SCHEMA_VERSIONS.contains(declaredSchemaVersion)
    ) {
      return List.of();
    }
    return List.of(
      new ValidationIssue(
        RULE,
        getXdmNodeLocation(
          validationContext.getFileName(),
          publicationDelivery
        ),
        declaredSchemaVersion,
        NeTExValidator.LATEST
      )
    );
  }

  /**
   * The version attribute has the form {netexSchemaVersion}:{profileName}:{profileVersion}, e.g.
   * "1.15:NO-NeTEx-networktimetable:1.5", and only the first segment is the NeTEx schema version.
   *
   * @return the declared NeTEx schema version, or null if the attribute is absent or is not in that
   * three-segment form. This is the same split {@code NetexSchemaRepository.getSchemaVersion}
   * applies when it picks the schema.
   */
  private static String declaredSchemaVersion(String versionAttribute) {
    if (versionAttribute == null) {
      return null;
    }
    String[] segments = versionAttribute.split(":");
    return segments.length == 3 ? segments[0] : null;
  }

  @Override
  public ValidationRule rule() {
    return RULE;
  }
}
