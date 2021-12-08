package no.entur.antu.validator.xpath;

import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmNode;

public abstract class AbstractXPathValidationRule implements ValidationRule {

    protected String getXdmNodeLocation(XdmNode xdmNode) {
        int lineNumber = xdmNode.getLineNumber();
        int columnNumber = xdmNode.getColumnNumber();
        String netexId = xdmNode.getAttributeValue(new QName("id"));
        if(netexId == null) {
            netexId = "(N/A)";
        }
        return "[Line " + lineNumber + ", Column " + columnNumber + ", Id " + netexId + "] ";
    }
}
