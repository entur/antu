package no.entur.antu.validator.xpath.rules;

import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XdmValue;
import no.entur.antu.exception.AntuException;
import no.entur.antu.validator.ValidationReportEntry;
import no.entur.antu.validator.ValidationReportEntrySeverity;
import no.entur.antu.validator.xpath.ValidationContext;
import no.entur.antu.validator.xpath.ValidationRule;

import java.util.Collections;
import java.util.List;

public class ValidateExactlyOne implements ValidationRule {

    private final String name;
    private final String xpath;
    private final String message;
    private final ValidationReportEntrySeverity severity;

    public ValidateExactlyOne(String xpath, String message, String name, ValidationReportEntrySeverity validationReportEntrySeverity) {
        this.xpath = xpath;
        this.message = message;
        this.name = name;
        this.severity = validationReportEntrySeverity;
    }

    @Override
    public List<ValidationReportEntry> validate(ValidationContext validationContext)  {
        try {
            XPathSelector selector = validationContext.getxPathCompiler().compile(xpath).load();
            selector.setContextItem(validationContext.getXmlNode());
            XdmValue nodes = selector.evaluate();
            if (nodes.size() != 1) {
                return List.of(new ValidationReportEntry(message, name, severity, validationContext.getFileName()));
            }
            return Collections.emptyList();
        } catch (SaxonApiException e) {
            throw new AntuException("Error while validating rule " + xpath, e);
        }
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ValidationReportEntrySeverity getSeverity() {
        return severity;
    }
}
