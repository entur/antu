package no.entur.antu.validation.validator.journeypattern.stoppoint.samestoppoints;

import java.util.List;
import no.entur.antu.validation.ValidationError;

public record SameStopPointsError(
  RuleCode ruleCode,
  List<String> journeyPatternIds
)
  implements ValidationError {
  @Override
  public String getRuleCode() {
    return ruleCode.toString();
  }

  @Override
  public String validationReportEntryMessage() {
    return String.format(
      "%s. [%s]",
      ruleCode.getErrorMessage(),
      String.join(", ", journeyPatternIds())
    );
  }

  @Override
  public String getEntityId() {
    return journeyPatternIds.get(0);
  }

  enum RuleCode implements no.entur.antu.validation.RuleCode {
    SAME_STOP_POINT_IN_JOURNEY_PATTERNS("JourneyPatterns have same StopPoints");

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
