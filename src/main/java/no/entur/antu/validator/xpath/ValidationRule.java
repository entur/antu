package no.entur.antu.validator.xpath;

import no.entur.antu.validator.ValidationReportEntry;

import java.util.List;

public interface ValidationRule {
    List<ValidationReportEntry> validate(ValidationContext validationContext) ;

    String getMessage();
}
