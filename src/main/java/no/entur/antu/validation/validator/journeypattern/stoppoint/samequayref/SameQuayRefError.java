package no.entur.antu.validation.validator.journeypattern.stoppoint.samequayref;

import no.entur.antu.validation.ValidationError;

public record SameQuayRefError(
  RuleCode ruleCode,
  String journeyPatternId,
  String firstStopPointName,
  String secondStopPointName
)
  implements ValidationError {
  @Override
  public String getRuleCode() {
    return ruleCode.toString();
  }

  @Override
  public String validationReportEntryMessage() {
    return String.format(
      "Same quay refs in consecutive stop points [%s, %s] in journey pattern %s",
      firstStopPointName,
      secondStopPointName,
      journeyPatternId
    );
  }

  @Override
  public String getEntityId() {
    return journeyPatternId;
  }

  enum RuleCode implements no.entur.antu.validation.RuleCode {
    SAME_QUAY_REF_IN_CONSECUTIVE_STOP_POINTS_IN_JOURNEY_PATTERN(
      "Same quay refs in consecutive stop points in journey pattern."
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
