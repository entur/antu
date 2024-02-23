package no.entur.antu.validator.servicejourney.transportmode;

import no.entur.antu.validator.ValidationError;
import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;

public record TransportModeError(
  RuleCode ruleCode,
  AllVehicleModesOfTransportEnumeration mode,
  String serviceJourneyId
)
  implements ValidationError {
  @Override
  public String getRuleCode() {
    return ruleCode.toString();
  }

  @Override
  public String getEntityId() {
    return serviceJourneyId();
  }

  @Override
  public String validationReportEntryMessage() {
    return String.format(
      "Invalid transport mode %s found in service journey with id %s",
      mode(),
      serviceJourneyId()
    );
  }

  enum RuleCode implements no.entur.antu.validator.RuleCode {
    INVALID_TRANSPORT_MODE("Invalid transport mode");

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
