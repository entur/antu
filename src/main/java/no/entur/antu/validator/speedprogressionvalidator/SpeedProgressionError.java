package no.entur.antu.validator.speedprogressionvalidator;

public record SpeedProgressionError(
        PassingTimes passingTimes,
        RuleCode ruleCode,
        String expectedSpeed,
        String calculatedSpeed) {

    public enum RuleCode {
        LOW_SPEED_PROGRESSION("ServiceJourney has low speed progression"),
        HIGH_SPEED_PROGRESSION("ServiceJourney has too high speed progression"),
        WARNING_SPEED_PROGRESSION("ServiceJourney has high speed progression");

    private final String errorMessage;

    RuleCode(String errorMessage) {
      this.errorMessage = errorMessage;
    }

    public String getErrorMessage() {
      return errorMessage;
    }
  }

  public String validationReportEntryMessage(String serviceJourneyId) {
    return String.format(
      "%s, " +
      "ServiceJourneyId = %s, " +
      "ExpectedSpeed = %s, " +
      "ActualSpeed = %s, " +
      "from TimetabledPassingTime = %s, " +
      "to TimetabledPassingTime = %s",
      ruleCode().getErrorMessage(),
      serviceJourneyId,
      expectedSpeed(),
      calculatedSpeed(),
      passingTimes().from().timetabledPassingTimeId(),
      passingTimes().to().timetabledPassingTimeId()
    );
  }
}
