package no.entur.antu.validator.stoppointinjourneypatternvalidator;

import no.entur.antu.stoptime.PassingTimes;
import no.entur.antu.validator.ValidationError;

public record StopPointInJourneyPatternError(
  String stopPointInJourneyPatternRef,
  String scheduleStoPointRef,
  RuleCode ruleCode
)
  implements ValidationError {
  public enum RuleCode implements no.entur.antu.validator.RuleCode {
    MISSING_SCHEDULED_STOP_ASSIGNMENT(
      "Missing ScheduledStopAssignment for StopPointInJourneyPattern, while the ServiceJourney exists"
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
    return ruleCode.getErrorMessage();
  }
}
