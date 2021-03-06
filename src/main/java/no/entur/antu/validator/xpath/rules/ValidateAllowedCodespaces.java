package no.entur.antu.validator.xpath.rules;

import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmSequenceIterator;
import no.entur.antu.validator.codespace.NetexCodespace;
import org.entur.netex.validation.Constants;
import org.entur.netex.validation.exception.NetexValidationException;
import org.entur.netex.validation.validator.DataLocation;
import org.entur.netex.validation.validator.xpath.AbstractXPathValidationRule;
import org.entur.netex.validation.validator.xpath.XPathValidationContext;
import org.entur.netex.validation.validator.xpath.XPathValidationReportEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validate that the dataset references only the codespace it has access to.
 */
public class ValidateAllowedCodespaces extends AbstractXPathValidationRule {

    private static final String MESSAGE_FORMAT = "Codespace %s is not in the list of valid codespaces for this data space. Valid codespaces are %s";
    public static final String RULE_CODE = "CODESPACE";

    @Override
    public List<XPathValidationReportEntry> validate(XPathValidationContext validationContext) {
        List<XPathValidationReportEntry> validationReportEntries = new ArrayList<>();
        Set<NetexCodespace> validCodespaces = NetexCodespace.getValidNetexCodespacesFor(validationContext.getCodespace());
        try {
            XPathSelector selector = validationContext.getNetexXMLParser().getXPathCompiler().compile("PublicationDelivery/dataObjects/*/codespaces/Codespace | PublicationDelivery/dataObjects/CompositeFrame/frames/*/codespaces/Codespace").load();
            selector.setContextItem(validationContext.getXmlNode());
            for (XdmItem item : selector) {
                XdmNode codespaceNode = (XdmNode) item;
                String xmlns = null;
                String xmlnsUrl = null;
                XdmNode codespaceNamespaceNode = getChild(codespaceNode, new QName("n", Constants.NETEX_NAMESPACE, "Xmlns"));
                if (codespaceNamespaceNode != null) {
                    xmlns = codespaceNamespaceNode.getStringValue();
                }
                XdmNode codespaceNamespaceUrlNode = getChild(codespaceNode, new QName("n", Constants.NETEX_NAMESPACE, "XmlnsUrl"));
                if (codespaceNamespaceUrlNode != null) {
                    xmlnsUrl = codespaceNamespaceUrlNode.getStringValue();
                }
                NetexCodespace netexCodespace = new NetexCodespace(xmlns, xmlnsUrl);
                if (!validCodespaces.contains(netexCodespace)) {
                    DataLocation dataLocation = getXdmNodeLocation(validationContext.getFileName(), codespaceNode);
                    String message = String.format(MESSAGE_FORMAT, netexCodespace, validCodespaces.stream().map(NetexCodespace::toString).collect(Collectors.joining()));
                    validationReportEntries.add(new XPathValidationReportEntry(message, RULE_CODE, dataLocation));
                }
            }
            return validationReportEntries;
        } catch (SaxonApiException e) {
            throw new NetexValidationException("Error while validating rule ", e);
        }
    }

    @Override
    public String getMessage() {
        return MESSAGE_FORMAT;
    }

    @Override
    public String getCode() {
        return RULE_CODE;
    }

    private static XdmNode getChild(XdmNode parent, QName childName) {
        XdmSequenceIterator<XdmNode> iter = parent.axisIterator(Axis.CHILD, childName);
        if (iter.hasNext()) {
            return iter.next();
        } else {
            return null;
        }
    }
}
