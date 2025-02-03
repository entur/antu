package no.entur.antu.validation.validator.xpath.rules;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmValue;
import no.entur.antu.organisation.OrganisationRepository;
import org.entur.netex.validation.validator.DataLocation;
import org.entur.netex.validation.validator.Severity;
import org.entur.netex.validation.validator.ValidationIssue;
import org.entur.netex.validation.validator.ValidationRule;
import org.entur.netex.validation.validator.xpath.AbstractXPathValidationRule;
import org.entur.netex.validation.validator.xpath.XPathRuleValidationContext;

public class ValidateAuthorityRef extends AbstractXPathValidationRule {

  public static final String CODE_AUTHORITY_REF = "AUTHORITY_REF";
  static final ValidationRule RULE = new ValidationRule(
          CODE_AUTHORITY_REF,
    "Invalid Authority Ref",
    "Invalid Authority Ref",
    Severity.ERROR
  );

  private final OrganisationRepository organisationV3Repository;

  public ValidateAuthorityRef(OrganisationRepository organisationV3Repository) {
    this.organisationV3Repository = Objects.requireNonNull(organisationV3Repository);
  }

  @Override
  public List<ValidationIssue> validate(
    XPathRuleValidationContext validationContext
  ) {
    String xpath = "//ServiceFrame/Network/AuthorityRef";
    XPathSelector selector = null;
    List<ValidationIssue> validationIssues = new ArrayList<>();
    try {
      selector =
        validationContext
          .getNetexXMLParser()
          .getXPathCompiler()
          .compile(xpath)
          .load();
      selector.setContextItem(validationContext.getXmlNode());
      XdmValue nodes = selector.evaluate();
      for (XdmValue node : nodes) {
        XdmNode xdmNode = (XdmNode) node;
        String authorityRef = ((XdmNode) node).attribute("ref");
        Boolean organisationExists = organisationV3Repository.organisationExists(authorityRef);
        DataLocation dataLocation = getXdmNodeLocation(
          validationContext.getFileName(),
          xdmNode
        );
        if (!organisationExists) {
          validationIssues.add(new ValidationIssue(RULE, dataLocation));
        }
      }
      return validationIssues;
    } catch (SaxonApiException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public ValidationRule rule() {
    return RULE;
  }
}
