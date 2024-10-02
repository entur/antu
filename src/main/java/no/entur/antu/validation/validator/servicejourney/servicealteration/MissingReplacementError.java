package no.entur.antu.validation.validator.servicejourney.servicealteration;

import no.entur.antu.validation.ValidationError;

public record MissingReplacementError(
  String datedServiceJourneyId,
  RuleCode ruleCode
)
  implements ValidationError {
  public enum RuleCode implements no.entur.antu.validation.RuleCode {
    MISSING_REPLACEMENT("Missing replacement");

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
    return datedServiceJourneyId;
  }

  @Override
  public String validationReportEntryMessage() {
    return "No replacement found";
  }
}
