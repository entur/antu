package no.entur.antu.validator;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ValidationReportEntry {

    private final String message;
    private final String category;
    private final ValidationReportEntrySeverity severity;
    private final String fileName;

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
