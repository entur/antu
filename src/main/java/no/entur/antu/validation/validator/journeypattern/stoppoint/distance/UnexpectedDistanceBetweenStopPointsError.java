package no.entur.antu.validation.validator.journeypattern.stoppoint.distance;

import no.entur.antu.validation.ValidationError;
import no.entur.antu.validation.utilities.Comparison;

public record UnexpectedDistanceBetweenStopPointsError(
  RuleCode ruleCode,
  String journeyPatternId,
  String fromStopPointName,
  String toStopPointName,
  Comparison<Double> distanceComparison
)
  implements ValidationError {
  @Override
  public String getRuleCode() {
    return ruleCode.toString();
  }

  @Override
  public String validationReportEntryMessage() {
    return (
      String.format(
        "Distance between stop points (%s - %s) is more than expected. Expected: %s, actual: %s.",
        fromStopPointName,
        toStopPointName,
        distanceComparison.expected(),
        distanceComparison.actual()
      )
    );
  }

  @Override
  public String getEntityId() {
    return journeyPatternId;
  }

  enum RuleCode implements no.entur.antu.validation.RuleCode {
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
