package no.entur.antu.validation;

/**
 * Interface for validation errors.
 * This interface is used to represent a validation error.
 */
public interface ValidationError {
  String getRuleCode();
  String validationReportEntryMessage();
  String getEntityId();
}
