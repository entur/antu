package no.entur.antu.validation.validator.xpath.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import no.entur.antu.config.ValidationParametersConfig;
import no.entur.antu.organisation.SimpleOrganisationAliasRepository;
import no.entur.antu.validation.validator.xpath.EnturTimetableDataValidationTreeFactory;
import org.entur.netex.validation.test.xpath.support.TestValidationContextBuilder;
import org.entur.netex.validation.validator.Severity;
import org.entur.netex.validation.validator.ValidationIssue;
import org.entur.netex.validation.validator.xpath.XPathRuleValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ValidateSupportedNetexVersionTest {

  private static final String TEST_CODESPACE = "FLB";

  private final ValidateSupportedNetexVersion validateSupportedNetexVersion =
    new ValidateSupportedNetexVersion();

  /**
   * Versions netex-java-model ships a schema for, in the three-segment form the Nordic profile
   * declares them in. Two are worth naming: {@code 1.04} is what a file declares for the schema the
   * enum calls {@code 1.04beta}, which is why only the numeric part of the enum name is compared,
   * and {@code 1.16.1} is a schema that exists even though {@code NetexSchemaRepository} has no case
   * selecting it - a file declaring it falls back to the latest schema, which is that same one, so
   * there is nothing to report. Patch versions have never been advertised as schema versions a
   * dataset may declare, so neither is expected in the wild; the point is that the rule reports on
   * the schemas that exist, not on a list maintained here.
   */
  @ParameterizedTest
  @ValueSource(strings = { "1.04", "1.08", "1.13", "1.15", "1.16", "1.16.1" })
  void aVersionThereIsASchemaForIsAccepted(String schemaVersion) {
    assertEquals(
      List.of(),
      validate(schemaVersion + ":NO-NeTEx-networktimetable:1.5")
    );
  }

  /**
   * The ways a declared version ends up unsupported: the next NeTEx release, which is the case this
   * rule exists for, a major version that will not exist for years, and a version number that was
   * never published at all.
   */
  @ParameterizedTest
  @ValueSource(strings = { "1.17", "2", "1.5" })
  void aVersionThereIsNoSchemaForIsReported(String schemaVersion) {
    List<ValidationIssue> issues = validate(
      schemaVersion + ":NO-NeTEx-networktimetable:1.5"
    );

    assertEquals(1, issues.size());
    ValidationIssue issue = issues.getFirst();
    assertEquals("UNSUPPORTED_NETEX_VERSION", issue.rule().code());
    assertEquals(Severity.CRITICAL, issue.rule().severity());
    assertTrue(
      issue.message().contains(schemaVersion),
      "The message should name the version that is not supported, was: " +
      issue.message()
    );
  }

  /**
   * A version attribute the NeTEx schema version cannot be read out of is left alone: the schema
   * validation has always fallen back to the latest schema for these, and the checked-in stop place
   * dataset is one of them. Reporting them would reject datasets that validate today.
   */
  @ParameterizedTest
  @ValueSource(
    strings = {
      // What the checked-in stop place dataset declares.
      "205",
      // Names a supported schema version, but with no profile segments after it, so there is no
      // schema version segment to read.
      "1.15",
      "any",
      "1.15:NO-NeTEx-stops",
    }
  )
  void aVersionThatCannotBeReadIsLeftToTheSchemaFallback(
    String versionAttribute
  ) {
    assertEquals(List.of(), validate(versionAttribute));
  }

  @Test
  void aPublicationDeliveryWithNoVersionAttributeIsLeftToTheSchemaFallback() {
    assertEquals(
      List.of(),
      validateFragment(
        "<PublicationDelivery xmlns=\"http://www.netex.org.uk/netex\"></PublicationDelivery>"
      )
    );
  }

  /**
   * Being registered on the tree the timetable profiles validate against is what gets the rule into
   * a report at all, and is a separate thing from the rule working. The flexible transport, Sweden
   * and Finland profiles inherit that tree; the stop place profile has an empty one and so does not
   * run this rule.
   */
  @Test
  void theRuleIsRegisteredOnTheTimetableValidationTree() {
    XPathRuleValidator timetableValidationTree = new XPathRuleValidator(
      new EnturTimetableDataValidationTreeFactory(
        new SimpleOrganisationAliasRepository(Set.of()),
        new ValidationParametersConfig()
      )
    );

    assertTrue(
      timetableValidationTree
        .getRules()
        .contains(ValidateSupportedNetexVersion.RULE)
    );
  }

  private List<ValidationIssue> validate(String versionAttribute) {
    return validateFragment(
      "<PublicationDelivery xmlns=\"http://www.netex.org.uk/netex\" version=\"%s\"></PublicationDelivery>".formatted(
          versionAttribute
        )
    );
  }

  private List<ValidationIssue> validateFragment(String netexFragment) {
    return validateSupportedNetexVersion.validate(
      TestValidationContextBuilder
        .ofNetexFragment(netexFragment)
        .withCodespace(TEST_CODESPACE)
        .build()
    );
  }
}
