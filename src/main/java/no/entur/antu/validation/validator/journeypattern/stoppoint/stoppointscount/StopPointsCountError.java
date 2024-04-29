package no.entur.antu.validation.validator.journeypattern.stoppoint.stoppointscount;

import no.entur.antu.validation.ValidationError;

public record StopPointsCountError(
  RuleCode ruleCode,
  String journeyPatternId,
  int numberOfStopPoints,
  int numberOfLinks
)
  implements ValidationError {
  @Override
  public String getRuleCode() {
    return ruleCode.toString();
  }

  @Override
  public String validationReportEntryMessage() {
    return (
      ruleCode.getErrorMessage() +
      ", Number of stop points = " +
      numberOfStopPoints +
      ", Number of links = " +
      numberOfLinks
    );
  }

  @Override
  public String getEntityId() {
    return journeyPatternId;
  }

  enum RuleCode implements no.entur.antu.validation.RuleCode {
    INVALID_NUMBER_OF_STOP_POINTS_OR_LINKS_IN_JOURNEY_PATTERN(
      "Invalid number of Stop points or links in JourneyPattern"
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
