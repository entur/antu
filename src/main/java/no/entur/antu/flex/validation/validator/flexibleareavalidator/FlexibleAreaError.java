package no.entur.antu.flex.validation.validator.flexibleareavalidator;

import no.entur.antu.validator.ValidationError;

public record FlexibleAreaError(
  RuleCode ruleCode,
  String flexibleAreaId,
  String flexibleStopPlaceId
)
  implements ValidationError {

  @Override
  public String getRuleCode() {
    return ruleCode.toString();
  }

  @Override
  public String validationReportEntryMessage() {
    return String.format(
      "Invalid flexible area found with id %s",
      flexibleAreaId()
    );
  }

  @Override
  public String getEntityId() {
    return flexibleAreaId();
  }

  enum RuleCode implements no.entur.antu.validator.RuleCode {
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
