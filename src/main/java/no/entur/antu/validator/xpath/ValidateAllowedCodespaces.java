package no.entur.antu.validator.xpath;

import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmSequenceIterator;
import no.entur.antu.exception.AntuException;
import no.entur.antu.validator.ValidationReportEntry;
import no.entur.antu.validator.ValidationReportEntrySeverity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static no.entur.antu.Constants.NETEX_NAMESPACE;
import static no.entur.antu.Constants.NSR_XMLNS;
import static no.entur.antu.Constants.NSR_XMLNSURL;
import static no.entur.antu.Constants.PEN_XMLNS;
import static no.entur.antu.Constants.PEN_XMLNSURL;

public class ValidateAllowedCodespaces implements ValidationRule {

    private static final NetexCodespace NSR_NETEX_CODESPACE = new NetexCodespace(NSR_XMLNS, NSR_XMLNSURL);
    private static final NetexCodespace PEN_NETEX_CODESPACE = new NetexCodespace(PEN_XMLNS, PEN_XMLNSURL);
    private static final String MESSAGE_FORMAT = " Codespace %s is not in the list of valid codespaces for this data space. Valid codespaces are %s";

    @Override
    public List<ValidationReportEntry> validate(ValidationContext validationContext) {
        List<ValidationReportEntry> validationReportEntries = new ArrayList<>();
        NetexCodespace currentCodespace = getCurrentNetexCodespace(validationContext.getCodespace());
        Set<NetexCodespace> validCodespaces = Set.of(NSR_NETEX_CODESPACE, PEN_NETEX_CODESPACE, currentCodespace);
        try {
            XPathSelector selector = validationContext.getxPathCompiler().compile("PublicationDelivery/dataObjects/*/codespaces/Codespace | PublicationDelivery/dataObjects/CompositeFrame/frames/*/codespaces/Codespace").load();
            selector.setContextItem(validationContext.getXmlNode());
            for (XdmItem item : selector) {
                String xmlns = null;
                String xmlnsUrl = null;
                XdmNode codespaceNamespaceNode = getChild((XdmNode) item, new QName("n", NETEX_NAMESPACE, "Xmlns"));
                if (codespaceNamespaceNode != null) {
                    xmlns = codespaceNamespaceNode.getStringValue();
                }
                XdmNode codespaceNamespaceUrlNode = getChild((XdmNode) item, new QName("n", NETEX_NAMESPACE, "XmlnsUrl"));
                if (codespaceNamespaceUrlNode != null) {
                    xmlnsUrl = codespaceNamespaceUrlNode.getStringValue();
                }
                NetexCodespace netexCodespace = new NetexCodespace(xmlns, xmlnsUrl);
                if (!validCodespaces.contains(netexCodespace)) {
                    String message = String.format(MESSAGE_FORMAT, netexCodespace, validCodespaces.stream().map(NetexCodespace::toString).collect(Collectors.joining()));
                    validationReportEntries.add(new ValidationReportEntry(message, "Codespace", ValidationReportEntrySeverity.ERROR, validationContext.getFileName()));
                }
            }
            return validationReportEntries;
        } catch (SaxonApiException e) {
            throw new AntuException("Error while validating rule ", e);
        }
    }


    private NetexCodespace getCurrentNetexCodespace(String codespace) {
        return new NetexCodespace(codespace.toUpperCase(Locale.ROOT), "http://www.rutebanken.org/ns/" + codespace.toLowerCase(Locale.ROOT));
    }

    @Override
    public String getMessage() {
        return MESSAGE_FORMAT;
    }


    private static class NetexCodespace {

        private final String xmlns;
        private final String xmlnsUrl;

        private NetexCodespace(String xmlns, String xmlnsUrl) {
            this.xmlns = xmlns;
            this.xmlnsUrl = xmlnsUrl;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            NetexCodespace netexCodespace = (NetexCodespace) o;
            return Objects.equals(xmlns, netexCodespace.xmlns) && Objects.equals(xmlnsUrl, netexCodespace.xmlnsUrl);
        }

        @Override
        public int hashCode() {
            return Objects.hash(xmlns, xmlnsUrl);
        }

        @Override
        public String toString() {
            return "{" +
                    "xmlns='" + xmlns + '\'' +
                    ", xmlnsUrl='" + xmlnsUrl + '\'' +
                    '}';
        }

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
