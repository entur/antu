package no.entur.antu.validator;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ValidationReportEntry {

    private String message;
    private String category;
    private ValidationReportEntrySeverity severity;
    private String fileName;

    public ValidationReportEntry() {
    }

    public ValidationReportEntry(String message, String category, ValidationReportEntrySeverity severity) {
        this(message, category, severity, "");
    }

    public ValidationReportEntry(String message, String category, ValidationReportEntrySeverity severity, String fileName) {
        this.message = message;
        this.category = category;
        this.severity = severity;
        this.fileName = fileName;
    }

    public String getMessage() {
        return message;
    }

    public String getCategory() {
        return category;
    }

    public ValidationReportEntrySeverity getSeverity() {
        return severity;
    }

    public String getFileName() {
        return fileName;
    }
}
