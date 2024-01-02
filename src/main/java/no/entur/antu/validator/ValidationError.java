package no.entur.antu.validator;

public interface ValidationError {
  String getRuleCode();
  String validationReportEntryMessage();
  String getEntityId();
}
