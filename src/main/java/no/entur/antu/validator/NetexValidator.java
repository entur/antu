package no.entur.antu.validator;

import no.entur.antu.validator.xpath.ValidationContext;

public interface NetexValidator {
    void validate(ValidationReport validationReport, ValidationContext validationContext);
}
