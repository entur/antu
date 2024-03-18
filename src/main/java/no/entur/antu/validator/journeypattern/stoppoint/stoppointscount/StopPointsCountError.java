package no.entur.antu.validator.journeypattern.stoppoint.stoppointscount;

import no.entur.antu.validator.ValidationError;

public record StopPointsCountError(RuleCode ruleCode, String journeyPatternId)
  implements ValidationError {
  @Override
  public String getRuleCode() {
    return ruleCode.toString();
  }

  @Override
  public String validationReportEntryMessage() {
    return ruleCode.getErrorMessage();
  }

  @Override
  public String getEntityId() {
    return journeyPatternId;
  }

  enum RuleCode implements no.entur.antu.validator.RuleCode {
    INVALID_NUMBER_OF_STOP_POINTS_IN_JOURNEY_PATTERN(
      "Invalid number of Stop points or service links in JourneyPattern"
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
}
