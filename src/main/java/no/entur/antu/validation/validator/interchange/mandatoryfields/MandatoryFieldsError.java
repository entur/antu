package no.entur.antu.validation.validator.interchange.mandatoryfields;

import no.entur.antu.validation.ValidationError;

public record MandatoryFieldsError(RuleCode ruleCode, String interchangeId)
  implements ValidationError {
  @Override
  public String getRuleCode() {
    return ruleCode.toString();
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
      "From stop point is missing in interchange."
    ),
    MISSING_TO_STOP_POINT_IN_INTERCHANGE(
      "To stop point is missing in interchange."
    ),
    MISSING_FROM_SERVICE_JOURNEY_IN_INTERCHANGE(
      "From service journey is missing in interchange."
    ),
    MISSING_TO_SERVICE_JOURNEY_IN_INTERCHANGE(
      "To service journey is missing in interchange."
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