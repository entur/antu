package no.entur.antu.validation.validator.xpath.rules;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmValue;
import no.entur.antu.validation.validator.organisation.OrganisationAliasRepository;
import org.entur.netex.validation.validator.Severity;
import org.entur.netex.validation.validator.ValidationIssue;
import org.entur.netex.validation.validator.ValidationRule;
import org.entur.netex.validation.validator.xpath.AbstractXPathValidationRule;
import org.entur.netex.validation.validator.xpath.XPathRuleValidationContext;

/**
 * Validates that the value of AuthorityRef.ref in the NeTEx file being validated
 * exists in the repository of organisation aliases.
 * */
public class ValidateAuthorityRef extends AbstractXPathValidationRule {

  public static final String CODE_AUTHORITY_REF = "AUTHORITY_REF";
  public static final String CODE_AUTHORITY_REF_IN_ADDITIONAL_NETWORKS =
    "AUTHORITY_REF_IN_ADDITIONAL_NETWORKS";

  static final ValidationRule INVALID_AUTHORITY_REF_RULE = new ValidationRule(
    CODE_AUTHORITY_REF,
    "Invalid Authority Ref",
    "Authority Ref %s does not exist in the organisation registry",
    Severity.ERROR
  );
  static final ValidationRule INVALID_AUTHORITY_REF_IN_ADDITIONAL_NETWORKS_RULE =
    new ValidationRule(
      CODE_AUTHORITY_REF_IN_ADDITIONAL_NETWORKS,
      "Invalid Authority Ref in Additional Networks",
      "Authority Ref %s does not exist in the organisation registry",
      Severity.WARNING
    );

  private static final String XPATH_NETWORK =
    "//ServiceFrame/Network/AuthorityRef";
  private static final String XPATH_ADDITIONAL_NETWORKS =
    "//ServiceFrame/additionalNetworks/Network/AuthorityRef";

  private final OrganisationAliasRepository organisationAliasRepository;
  private final Set<String> additionalAllowedOrganisations;

  public ValidateAuthorityRef(
    OrganisationAliasRepository organisationAliasRepository,
    Set<String> additionalAllowedOrganisations
  ) {
    this.organisationAliasRepository =
      Objects.requireNonNull(organisationAliasRepository);
    this.additionalAllowedOrganisations =
      additionalAllowedOrganisations != null
        ? additionalAllowedOrganisations
        : Set.of();
  }

  private boolean organisationExists(String authorityRef) {
    return (
      additionalAllowedOrganisations.contains(authorityRef) ||
      organisationAliasRepository.hasOrganisationWithAlias(authorityRef)
    );
  }

  @Override
  public List<ValidationIssue> validate(
    XPathRuleValidationContext validationContext
  ) {
    List<ValidationIssue> validationIssues = new ArrayList<>();
    try {
      validateAuthorityRefs(
        validationContext,
        XPATH_NETWORK,
        INVALID_AUTHORITY_REF_RULE,
        validationIssues
      );
      validateAuthorityRefs(
        validationContext,
        XPATH_ADDITIONAL_NETWORKS,
        INVALID_AUTHORITY_REF_IN_ADDITIONAL_NETWORKS_RULE,
        validationIssues
      );
    } catch (SaxonApiException e) {
      throw new RuntimeException(e);
    }
    return validationIssues;
  }

  private void validateAuthorityRefs(
    XPathRuleValidationContext validationContext,
    String xpath,
    ValidationRule rule,
    List<ValidationIssue> validationIssues
  ) throws SaxonApiException {
    XPathSelector selector = validationContext
      .getNetexXMLParser()
      .getXPathCompiler()
      .compile(xpath)
      .load();
    selector.setContextItem(validationContext.getXmlNode());
    XdmValue nodes = selector.evaluate();
    for (XdmValue node : nodes) {
      XdmNode xdmNode = (XdmNode) node;
      String authorityRef = xdmNode.attribute("ref");
      if (!organisationExists(authorityRef)) {
        validationIssues.add(
          new ValidationIssue(
            rule,
            getXdmNodeLocation(validationContext.getFileName(), xdmNode),
            authorityRef
          )
        );
      }
    }
  }

  @Override
  public ValidationRule rule() {
    return INVALID_AUTHORITY_REF_RULE;
  }
}
