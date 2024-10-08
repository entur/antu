package no.entur.antu.validation.validator.servicejourney.transportmode;

import no.entur.antu.validation.ValidationError;
import org.entur.netex.validation.validator.model.ServiceJourneyId;

public record MismatchedTransportModeError(
  RuleCode ruleCode,
  String actual,
  String expected,
  ServiceJourneyId serviceJourneyId
)
  implements ValidationError {
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
    return String.format(
      "%s. Expected %s, was %s.",
      ruleCode.getErrorMessage(),
      expected,
      actual
    );
  }

  enum RuleCode implements no.entur.antu.validation.RuleCode {
    INVALID_TRANSPORT_MODE("Invalid transport mode"),
    INVALID_TRANSPORT_SUB_MODE("Invalid transport sub mode");

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
