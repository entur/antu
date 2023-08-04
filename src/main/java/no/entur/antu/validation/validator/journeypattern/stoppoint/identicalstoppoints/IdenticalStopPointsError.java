package no.entur.antu.validation.validator.journeypattern.stoppoint.identicalstoppoints;

import java.util.List;
import no.entur.antu.validation.ValidationError;

public record IdenticalStopPointsError(
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
    IDENTICAL_JOURNEY_PATTERNS("Identical journey patterns");

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
