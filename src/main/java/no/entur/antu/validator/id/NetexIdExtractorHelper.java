package no.entur.antu.validator.id;

import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmValue;
import no.entur.antu.validator.xpath.ValidationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class NetexIdExtractorHelper {

    private NetexIdExtractorHelper() {
    }

    public static List<IdVersion> collectEntityIdentificators(ValidationContext validationContext, Set<String> ignorableElementNames)
            throws SaxonApiException {
        return collectIdOrRefWithVersion(validationContext, "id", ignorableElementNames);
    }

    public static List<IdVersion> collectEntityReferences(ValidationContext validationContext, Set<String> ignorableElementNames)
            throws SaxonApiException {
        return collectIdOrRefWithVersion(validationContext, "ref", ignorableElementNames);
    }

    public static List<IdVersion> collectIdOrRefWithVersion(ValidationContext validationContext, String attributeName, Set<String> ignorableElementNames)
            throws SaxonApiException {
        StringBuilder filterClause = new StringBuilder();
        filterClause.append("//n:*[");
        if (ignorableElementNames != null) {
            for (String elementName : ignorableElementNames) {
                filterClause.append("not(local-name(.)='").append(elementName).append("') and ");
            }
        }
        filterClause.append("@").append(attributeName).append("]");

        XPathSelector selector = validationContext.getxPathCompiler().compile(filterClause.toString()).load();
        selector.setContextItem(validationContext.getXmlNode());
        XdmValue nodes = selector.evaluate();

        String filename = validationContext.getFileName();
        QName versionQName = new QName("version");
        List<IdVersion> ids = new ArrayList<>();
        for (XdmItem item : nodes) {
            XdmNode n = (XdmNode) item;
            String elementName = n.getNodeName().getLocalName();

            List<String> parentElementNames = new ArrayList<>();
            XdmNode p = n.getParent();
            while (p != null && p.getNodeName() != null) {
                parentElementNames.add(p.getNodeName().getLocalName());
                p = p.getParent();
            }
            String id = n.getAttributeValue(new QName(attributeName));
            String version = n.getAttributeValue(versionQName);

            ids.add(new IdVersion(id, version, elementName, parentElementNames, filename,
                    n.getLineNumber(), n.getColumnNumber()));

        }
        return ids;
    }


}
