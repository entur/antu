package no.entur.antu.validation.validator.interchange.mandatoryfields;

import no.entur.antu.validation.ValidationError;

public record MandatoryFieldsError(RuleCode ruleCode, String interchangeId)
  implements ValidationError {
  @Override
  public String getRuleCode() {
    return "MISSING_MANDATORY_FIELDS_IN_INTERCHANGE";
  }

  @Override
  public String validationReportEntryMessage() {
    return ruleCode.getErrorMessage();
  }

  @Override
  public String getEntityId() {
    return interchangeId;
  }

  enum RuleCode implements no.entur.antu.validation.RuleCode {
    MISSING_FROM_STOP_POINT_IN_INTERCHANGE(
      "FromPointRef or stop point is missing"
    ),
    MISSING_TO_STOP_POINT_IN_INTERCHANGE("ToPointRef or stop point is missing"),
    MISSING_FROM_SERVICE_JOURNEY_IN_INTERCHANGE(
      "FromJourneyRef or service journey is missing"
    ),
    MISSING_TO_SERVICE_JOURNEY_IN_INTERCHANGE(
      "ToJourneyRef or service journey is missing"
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
