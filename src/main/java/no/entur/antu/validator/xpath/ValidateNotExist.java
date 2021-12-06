package no.entur.antu.validator.xpath;

import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XPathExecutable;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmValue;
import no.entur.antu.exception.AntuException;
import no.entur.antu.validator.ValidationReportEntry;
import no.entur.antu.validator.ValidationReportEntrySeverity;

import java.util.ArrayList;
import java.util.List;

public class ValidateNotExist implements ValidationRule {

    private final String xpath;
    private final String message;
    private final String category;
    private final ValidationReportEntrySeverity severity;

    public ValidateNotExist(String xpath, String message, String category, ValidationReportEntrySeverity validationReportEntrySeverity) {
        this.xpath = xpath;
        this.message = message;
        this.category = category;
        this.severity = validationReportEntrySeverity;
    }

    @Override
    public List<ValidationReportEntry> validate(ValidationContext validationContext) {
        try {
            XPathExecutable compile = validationContext.getxPathCompiler().compile(xpath);
            XPathSelector selector = compile.load();
            selector.setContextItem(validationContext.getXmlNode());
            XdmValue nodes = selector.evaluate();
            List<ValidationReportEntry> validationReportEntries = new ArrayList<>();
            for (XdmItem item : nodes) {
                XdmNode xdmNode = (XdmNode) item;
                int lineNumber = xdmNode.getLineNumber();
                int columnNumber = xdmNode.getColumnNumber();
                String netexId = xdmNode.getAttributeValue(new QName("id"));
                String validationReportEntryMessage = "[Line " + lineNumber + ", Column " + columnNumber + ", Id " + netexId + "] " + message;
                validationReportEntries.add(new ValidationReportEntry(validationReportEntryMessage, category, severity, validationContext.getFileName()));
            }
            return validationReportEntries;
        } catch (SaxonApiException e) {
            throw new AntuException("Error while validating rule " + xpath, e);
        }
    }

    @Override
    public String getMessage() {
        return message;
    }
}
