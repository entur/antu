package no.entur.antu.validation.validator.interchange.distance;

import no.entur.antu.validation.ValidationError;
import no.entur.antu.validation.utilities.Comparison;

public record UnexpectedInterchangeDistanceError(
  RuleCode ruleCode,
  String interchangeId,
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
    return interchangeId;
  }

  enum RuleCode implements no.entur.antu.validation.RuleCode {
    DISTANCE_BETWEEN_STOP_POINTS_IN_INTERCHANGE_IS_MORE_THAN_MAX(
      "Distance between stop points in interchange is more than maximum limit."
    ),
    DISTANCE_BETWEEN_STOP_POINTS_IN_INTERCHANGE_IS_MORE_THAN_WARN(
      "Distance between stop points in interchange is more than warning limit."
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
