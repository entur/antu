package no.entur.antu.validation.flex.validator.flexiblearea;

import no.entur.antu.validation.ValidationError;

public record InvalidFlexibleAreaError(
  RuleCode ruleCode,
  String flexibleAreaId,
  String flexibleStopPlaceId,
  String description
)
  implements ValidationError {
  @Override
  public String getRuleCode() {
    return ruleCode.toString();
  }

  @Override
  public String validationReportEntryMessage() {
    return description();
  }

  @Override
  public String getEntityId() {
    return flexibleAreaId();
  }

  enum RuleCode implements no.entur.antu.validation.RuleCode {
    INVALID_FLEXIBLE_AREA("Invalid flexible area");

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
