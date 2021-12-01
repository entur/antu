package no.entur.antu.validator.xpath;

import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XPathExecutable;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XdmValue;
import no.entur.antu.validator.ValidationReportEntry;
import no.entur.antu.validator.ValidationReportEntrySeverity;

import java.util.Collections;
import java.util.List;

public class ValidateExist implements ValidationRule {

    private final String xpath;
    private final String message;
    private final String category;
    private final ValidationReportEntrySeverity severity;

    public ValidateExist(String xpath, String message, String category, ValidationReportEntrySeverity validationReportEntrySeverity) {
        this.xpath = xpath;
        this.message = message;
        this.category = category;
        this.severity = validationReportEntrySeverity;
    }

    @Override
    public List<ValidationReportEntry> validate(ValidationContext validationContext) throws SaxonApiException {
        XPathExecutable compile = validationContext.getxPathCompiler().compile(xpath);
        XPathSelector selector = compile.load();
        selector.setContextItem(validationContext.getXmlNode());
        XdmValue nodes = selector.evaluate();
        if (nodes.isEmpty()) {
            return List.of(new ValidationReportEntry(message, category, severity, validationContext.getFileName()));
        }
        return Collections.emptyList();
    }

    @Override
    public String getMessage() {
        return message;
    }
}
