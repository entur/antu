package no.entur.antu.validator.xpath;

import no.entur.antu.validator.ValidationReportEntry;
import no.entur.antu.validator.ValidationReportEntrySeverity;

import java.util.List;

public interface ValidationRule {
    List<ValidationReportEntry> validate(ValidationContext validationContext) ;

    String getMessage();

    String getName();

    ValidationReportEntrySeverity getSeverity();
}
