package no.entur.antu.validation.validator.servicejourney.speed;

import no.entur.antu.validation.ValidationError;
import no.entur.antu.validation.utilities.Comparison;
import org.entur.netex.validation.validator.model.ServiceJourneyId;

public record UnexpectedSpeedError(
  ServiceJourneyId serviceJourneyId,
  String fromStopPointName,
  String toStopPointName,
  RuleCode ruleCode,
  Comparison<String> speedComparison
)
  implements ValidationError {
  public enum RuleCode implements no.entur.antu.validation.RuleCode {
    LOW_SPEED("ServiceJourney has low speed"),
    HIGH_SPEED("ServiceJourney has too high speed"),
    WARNING_SPEED("ServiceJourney has high speed");

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
    return serviceJourneyId.id();
  }

  @Override
  public String validationReportEntryMessage() {
    return (
      ruleCode.getErrorMessage() +
      ", from " +
      fromStopPointName +
      ", to " +
      toStopPointName +
      ", ExpectedSpeed = " +
      speedComparison.expected() +
      ", ActualSpeed = " +
      speedComparison.actual()
    );
  }
}
