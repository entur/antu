package no.entur.antu.validator;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ValidationReportEntry {

    private final String message;
    private final String category;
    private final String severity;
    private final String fileName;

    public ValidationReportEntry(String message, String category, String severity) {
        this(message, category, severity, "");
    }

    public ValidationReportEntry(String message, String category, String severity, String fileName) {
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

    public String getSeverity() {
        return severity;
    }

    public String getFileName() {
        return fileName;
    }
}
