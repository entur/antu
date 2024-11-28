package no.entur.antu.validation.validator.xpath.rules;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import no.entur.antu.validation.NetexCodespace;
import org.entur.netex.validation.Constants;
import org.entur.netex.validation.exception.NetexValidationException;
import org.entur.netex.validation.validator.DataLocation;
import org.entur.netex.validation.validator.Severity;
import org.entur.netex.validation.validator.ValidationIssue;
import org.entur.netex.validation.validator.ValidationRule;
import org.entur.netex.validation.validator.xpath.AbstractXPathValidationRule;
import org.entur.netex.validation.validator.xpath.XPathRuleValidationContext;

/**
 * Validate that the dataset references only the codespace it has access to.
 */
public class ValidateAllowedCodespaces extends AbstractXPathValidationRule {

  static final ValidationRule RULE = new ValidationRule(
    "CODESPACE",
    "Codespace unknown",
    "Codespace %s is not in the list of valid codespaces for this data space. Valid codespaces are %s",
    Severity.ERROR
  );

  @Override
  public List<ValidationIssue> validate(
    XPathRuleValidationContext validationContext
  ) {
    Objects.requireNonNull(validationContext);
    List<ValidationIssue> validationReportEntries = new ArrayList<>();
    Set<NetexCodespace> validCodespaces =
      NetexCodespace.getValidNetexCodespacesFor(
        validationContext.getCodespace()
      );
    try {
      XPathSelector selector = validationContext
        .getNetexXMLParser()
        .getXPathCompiler()
        .compile(
          "PublicationDelivery/dataObjects/*/codespaces/Codespace | PublicationDelivery/dataObjects/CompositeFrame/frames/*/codespaces/Codespace"
        )
        .load();
      selector.setContextItem(validationContext.getXmlNode());
      for (XdmItem item : selector) {
        XdmNode codespaceNode = (XdmNode) item;
        String xmlns = null;
        String xmlnsUrl = null;
        XdmNode codespaceNamespaceNode = getChild(
          codespaceNode,
          new QName("n", Constants.NETEX_NAMESPACE, "Xmlns")
        );
        if (codespaceNamespaceNode != null) {
          xmlns = codespaceNamespaceNode.getStringValue();
        }
        XdmNode codespaceNamespaceUrlNode = getChild(
          codespaceNode,
          new QName("n", Constants.NETEX_NAMESPACE, "XmlnsUrl")
        );
        if (codespaceNamespaceUrlNode != null) {
          xmlnsUrl = codespaceNamespaceUrlNode.getStringValue();
        }
        NetexCodespace netexCodespace = new NetexCodespace(xmlns, xmlnsUrl);
        if (!validCodespaces.contains(netexCodespace)) {
          DataLocation dataLocation = getXdmNodeLocation(
            validationContext.getFileName(),
            codespaceNode
          );
          validationReportEntries.add(
            new ValidationIssue(
              RULE,
              dataLocation,
              netexCodespace,
              validCodespaces
                .stream()
                .map(NetexCodespace::toString)
                .collect(Collectors.joining())
            )
          );
        }
      }
      return validationReportEntries;
    } catch (SaxonApiException e) {
      throw new NetexValidationException("Error while validating rule ", e);
    }
  }

  @Override
  public ValidationRule rule() {
    return RULE;
  }
}
