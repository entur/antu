package no.entur.antu.validator;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ValidationReport {

    private  String codespace;
    private  String validationReportId;
    private  LocalDateTime creationDate;
    private  Collection<ValidationReportEntry> validationReportEntries;

    public ValidationReport() {
    }

    public ValidationReport(String codespace, String validationReportId) {
        this.codespace = codespace;
        this.validationReportId = validationReportId;
        this.creationDate = LocalDateTime.now();
        this.validationReportEntries = new ArrayList<>();
    }

    public ValidationReport(String codespace, String validationReportId, Collection<ValidationReportEntry> validationReportEntries) {
        this.codespace = codespace;
        this.validationReportId = validationReportId;
        this.creationDate = LocalDateTime.now();
        this.validationReportEntries = validationReportEntries;
    }

    public boolean hasError() {
        return validationReportEntries.stream().anyMatch(validationReportEntry -> ValidationReportEntrySeverity.ERROR == validationReportEntry.getSeverity());
    }

    public void addValidationReportEntry(ValidationReportEntry validationReportEntry) {
        validationReportEntries.add(validationReportEntry);
    }

    public void addAllValidationReportEntries(Collection<ValidationReportEntry> validationReportEntries) {
        this.validationReportEntries.addAll(validationReportEntries);
    }

    public Collection<ValidationReportEntry> getValidationReportEntries() {
        return validationReportEntries;
    }

    public String getCodespace() {
        return codespace;
    }

    public String getValidationReportId() {
        return validationReportId;
    }

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    public LocalDateTime getCreationDate() {
        return creationDate;
    }
}
