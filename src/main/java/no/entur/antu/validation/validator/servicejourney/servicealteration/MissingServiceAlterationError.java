package no.entur.antu.validation.validator.servicejourney.servicealteration;

import no.entur.antu.validation.ValidationError;

public record MissingServiceAlterationError(
  String serviceJourneyId,
  RuleCode ruleCode
)
  implements ValidationError {
  public enum RuleCode implements no.entur.antu.validation.RuleCode {
    MISSING_SERVICE_ALTERATION("Missing service alteration");

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
    return serviceJourneyId;
  }

  @Override
  public String validationReportEntryMessage() {
    return (
      ruleCode.getErrorMessage()
    );
  }
}
