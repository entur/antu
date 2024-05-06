package no.entur.antu.validation.validator.servicelink.stoppoints;

import no.entur.antu.model.ServiceLinkId;
import no.entur.antu.validation.ValidationError;
import no.entur.antu.validation.utilities.Comparison;

public record MismatchedStopPointsError(
  RuleCode ruleCode,
  ServiceLinkId serviceLinkId,
  String journeyPatternId,
  Comparison<String> fromStopPointName,
  Comparison<String> toStopPointName
)
  implements ValidationError {
  @Override
  public String getRuleCode() {
    return ruleCode.toString();
  }

  @Override
  public String getEntityId() {
    return serviceLinkId.id();
  }

  @Override
  public String validationReportEntryMessage() {
    return String.format(
      "Journey pattern id = %s, expected = [%s - %s], actual = [%s - %s]",
      journeyPatternId,
      fromStopPointName.expected(),
      toStopPointName.expected(),
      fromStopPointName.actual(),
      toStopPointName.actual()
    );
  }

  enum RuleCode implements no.entur.antu.validation.RuleCode {
    STOP_POINTS_IN_SERVICE_LINK_DOES_NOT_MATCH_THE_JOURNEY_PATTERN(
      "Stop points in service link does not match the journey pattern."
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
