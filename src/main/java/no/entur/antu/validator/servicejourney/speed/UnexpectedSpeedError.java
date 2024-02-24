package no.entur.antu.validator.servicejourney.speed;

import no.entur.antu.stoptime.PassingTimes;
import no.entur.antu.validator.ValidationError;

public record UnexpectedSpeedError(
  String serviceJourneyId,
  PassingTimes passingTimes,
  RuleCode ruleCode,
  String expectedSpeed,
  String calculatedSpeed
)
  implements ValidationError {
  public enum RuleCode implements no.entur.antu.validator.RuleCode {
    LOW_SPEED("ServiceJourney has low speed"),
    HIGH_SPEED("ServiceJourney has too high speed"),
    WARNING_SPEED("ServiceJourney has high speed");

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
    return serviceJourneyId;
  }

  @Override
  public String validationReportEntryMessage() {
    return (
      ruleCode.getErrorMessage() +
      ", from TimetabledPassingTime = " +
      passingTimes().from().timetabledPassingTimeId() +
      ", to TimetabledPassingTime = " +
      passingTimes().to().timetabledPassingTimeId() +
      ", ExpectedSpeed = " +
      expectedSpeed() +
      ", ActualSpeed = " +
      calculatedSpeed()
    );
  }
}
