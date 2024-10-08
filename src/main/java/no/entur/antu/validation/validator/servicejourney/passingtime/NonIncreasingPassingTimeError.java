package no.entur.antu.validation.validator.servicejourney.passingtime;

import no.entur.antu.validation.ValidationError;
import org.entur.netex.validation.validator.model.ServiceJourneyId;

public record NonIncreasingPassingTimeError(
  RuleCode ruleCode,
  String stopPointName,
  ServiceJourneyId serviceJourneyId
)
  implements ValidationError {
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
    return (ruleCode.getErrorMessage() + "at: " + stopPointName);
  }

  enum RuleCode implements no.entur.antu.validation.RuleCode {
    TIMETABLED_PASSING_TIME_INCOMPLETE_TIME(
      "ServiceJourney has incomplete TimetabledPassingTime"
    ),
    TIMETABLED_PASSING_TIME_INCONSISTENT_TIME(
      "ServiceJourney has inconsistent TimetabledPassingTime"
    ),
    TIMETABLED_PASSING_TIME_NON_INCREASING_TIME(
      "ServiceJourney has non-increasing TimetabledPassingTime"
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
