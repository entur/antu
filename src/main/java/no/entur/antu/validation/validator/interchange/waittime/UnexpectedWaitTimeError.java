package no.entur.antu.validation.validator.interchange.waittime;

import no.entur.antu.validation.ValidationError;
import no.entur.antu.validation.utilities.Comparison;

public record UnexpectedWaitTimeError(
  RuleCode ruleCode,
  String interchangeId,
  String fromStopPointName,
  String toStopPointName,
  Comparison<Double> waitTimeComparison
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
        "Wait time between stop points (%s - %s) is more than expected. Expected: %s, actual: %s.",
        fromStopPointName,
        toStopPointName,
        waitTimeComparison.expected(),
        waitTimeComparison.actual()
      )
    );
  }

  @Override
  public String getEntityId() {
    return interchangeId;
  }

  enum RuleCode implements no.entur.antu.validation.RuleCode {
    WAIT_TIME_ABOVE_THE_CONFIGURED_THRESHOLD(
      "Wait time is above the configured threshold."
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
