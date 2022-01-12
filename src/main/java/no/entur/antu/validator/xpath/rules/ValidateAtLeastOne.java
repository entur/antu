package no.entur.antu.validator.xpath.rules;

import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XdmValue;
import no.entur.antu.exception.AntuException;
import no.entur.antu.validator.ValidationReportEntry;
import no.entur.antu.validator.ValidationReportEntrySeverity;
import no.entur.antu.validator.xpath.ValidationRule;
import no.entur.antu.validator.xpath.XPathValidationContext;

import java.util.Collections;
import java.util.List;

public class ValidateAtLeastOne implements ValidationRule {

    private final String xpath;
    private final String message;
    private final String name;
    private final ValidationReportEntrySeverity severity;

    public ValidateAtLeastOne(String xpath, String message, String name, ValidationReportEntrySeverity validationReportEntrySeverity) {
        this.xpath = xpath;
        this.message = message;
        this.name = name;
        this.severity = validationReportEntrySeverity;
    }

    @Override
    public List<ValidationReportEntry> validate(XPathValidationContext validationContext)  {
        try {
            XPathSelector selector = validationContext.getxPathCompiler().compile(xpath).load();
            selector.setContextItem(validationContext.getXmlNode());
            XdmValue nodes = selector.evaluate();
            if (nodes.isEmpty()) {
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
