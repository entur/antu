package no.entur.antu.validation.validator.passengerstopassignment;

import no.entur.antu.validation.ValidationError;

public record MissingPassengerStopAssignmentError(
  String stopPointInJourneyPatternRef,
  String stopPointName,
  RuleCode ruleCode
)
  implements ValidationError {
  public enum RuleCode implements no.entur.antu.validation.RuleCode {
    MISSING_SCHEDULED_STOP_ASSIGNMENT(
      "Missing ScheduledStopAssignment for StopPoint."
    );

    private final String errorMessage;

    RuleCode(String errorMessage) {
      this.errorMessage = errorMessage;
    }

    @Override
    public String getErrorMessage() {
      return errorMessage;
    }
  }

  @Override
  public String getRuleCode() {
    return ruleCode.toString();
  }

  @Override
  public String getEntityId() {
    return stopPointInJourneyPatternRef;
  }

  @Override
  public String validationReportEntryMessage() {
    return (ruleCode.getErrorMessage() + " " + stopPointName);
  }
}
