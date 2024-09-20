package no.entur.antu.validation.validator.servicejourney.servicealteration;

import no.entur.antu.validation.ValidationError;
import org.rutebanken.netex.model.ServiceAlterationEnumeration;

public record InvalidServiceAlterationError(
  String datedServiceJourneyId,
  RuleCode ruleCode,
  ServiceAlterationEnumeration serviceAlteration
)
  implements ValidationError {
  public enum RuleCode implements no.entur.antu.validation.RuleCode {
    INVALID_SERVICE_ALTERATION("Invalid ServiceAlteration");

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
    return (
      ruleCode.getErrorMessage() +
      ". Expected 'replaced', but was " +
      (serviceAlteration == null ? "not provided" : serviceAlteration.value())
    );
  }
}
