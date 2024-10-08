package no.entur.antu.validation.validator.servicejourney.speed;

import no.entur.antu.validation.ValidationError;
import org.entur.netex.validation.validator.model.ServiceJourneyId;

public record SameDepartureArrivalTimeError(
  ServiceJourneyId serviceJourneyId,
  String fromStopPointName,
  String toStopPointName,
  RuleCode ruleCode
)
  implements ValidationError {
  public enum RuleCode implements no.entur.antu.validation.RuleCode {
    SAME_DEPARTURE_ARRIVAL_TIME(
      "Same departure/arrival time for consecutive stops"
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
      "%s, " + "from %s, " + "to %s",
      ruleCode().getErrorMessage(),
      fromStopPointName,
      toStopPointName
    );
  }
}
