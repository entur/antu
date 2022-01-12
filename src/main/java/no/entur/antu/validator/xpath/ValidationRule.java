package no.entur.antu.validator.xpath;

import no.entur.antu.validator.ValidationReportEntry;
import no.entur.antu.validator.ValidationReportEntrySeverity;

import java.util.List;

public interface ValidationRule {
    List<ValidationReportEntry> validate(XPathValidationContext validationContext) ;

    String getMessage();

    String getName();

    ValidationReportEntrySeverity getSeverity();
}
