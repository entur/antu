package no.entur.antu.validator.xpath;

import net.sf.saxon.s9api.SaxonApiException;
import no.entur.antu.validator.ValidationReportEntry;

import java.util.List;

public interface ValidationRule {
    List<ValidationReportEntry> validate(ValidationContext validationContext) throws SaxonApiException;

    String getMessage();
}
