package no.entur.antu.validator.journeypattern.stoppoint.distance;

import no.entur.antu.validator.ValidationError;

public record UnexpectedDistanceError(
  RuleCode ruleCode,
  String journeyPatternId,
  String fromStopPointId,
  String toStopPointId
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
      "from: " +
      fromStopPointId +
      " to: " +
      toStopPointId
    );
  }

  @Override
  public String getEntityId() {
    return journeyPatternId;
  }

  enum RuleCode implements no.entur.antu.validator.RuleCode {
    DISTANCE_BETWEEN_STOP_POINTS_MORE_THAN_EXPECTED(
      "Distance between stop points is more than expected."
    ),
    DISTANCE_BETWEEN_STOP_POINTS_LESS_THAN_EXPECTED(
      "Distance between stop points is less than expected."
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
