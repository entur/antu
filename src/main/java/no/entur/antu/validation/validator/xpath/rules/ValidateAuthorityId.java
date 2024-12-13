package no.entur.antu.validation.validator.xpath.rules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmValue;
import no.entur.antu.exception.AntuException;
import no.entur.antu.organisation.OrganisationRepository;
import org.entur.netex.validation.validator.DataLocation;
import org.entur.netex.validation.validator.Severity;
import org.entur.netex.validation.validator.ValidationIssue;
import org.entur.netex.validation.validator.ValidationRule;
import org.entur.netex.validation.validator.xpath.AbstractXPathValidationRule;
import org.entur.netex.validation.validator.xpath.XPathRuleValidationContext;

/**
 * Validate the Authority ids against the Organisation Register.
 */
public class ValidateAuthorityId extends AbstractXPathValidationRule {

  public static final String CODE_AUTHORITY_ID = "AUTHORITY_ID";
  static final ValidationRule RULE = new ValidationRule(
    CODE_AUTHORITY_ID,
    "Authority invalid Id",
    "Invalid Authority Id",
    Severity.ERROR
  );

  private final OrganisationRepository organisationRepository;

  public ValidateAuthorityId(OrganisationRepository organisationRepository) {
    this.organisationRepository =
      Objects.requireNonNull(organisationRepository);
  }

  @Override
  public List<ValidationIssue> validate(
    XPathRuleValidationContext validationContext
  ) {
    try {
      Set<String> whitelistedAuthorityIds =
        organisationRepository.getWhitelistedAuthorityIds(
          validationContext.getCodespace()
        );
      if (whitelistedAuthorityIds.isEmpty()) {
        return Collections.emptyList();
      } else {
        String xpath =
          "//ResourceFrame/organisations/Authority[not(@id=('" +
          String.join("','", whitelistedAuthorityIds) +
          "'))]";
        XPathSelector selector = validationContext
          .getNetexXMLParser()
          .getXPathCompiler()
          .compile(xpath)
          .load();
        selector.setContextItem(validationContext.getXmlNode());
        XdmValue nodes = selector.evaluate();
        List<ValidationIssue> validationIssues = new ArrayList<>();
        for (XdmItem item : nodes) {
          XdmNode xdmNode = (XdmNode) item;
          DataLocation dataLocation = getXdmNodeLocation(
            validationContext.getFileName(),
            xdmNode
          );
          validationIssues.add(new ValidationIssue(RULE, dataLocation));
        }
        return validationIssues;
      }
    } catch (SaxonApiException e) {
      throw new AntuException("Exception while validating authority ID", e);
    }
  }

  @Override
  public ValidationRule rule() {
    return RULE;
  }
}
