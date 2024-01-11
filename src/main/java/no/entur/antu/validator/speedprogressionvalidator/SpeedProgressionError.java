package no.entur.antu.validator.speedprogressionvalidator;

import no.entur.antu.validator.ValidationError;

public record SpeedProgressionError (
        String serviceJourneyId,
        PassingTimes passingTimes,
        RuleCode ruleCode,
        String expectedSpeed,
        String calculatedSpeed) implements ValidationError {

    public enum RuleCode implements no.entur.antu.validator.RuleCode {
        LOW_SPEED_PROGRESSION("ServiceJourney has low speed progression"),
        HIGH_SPEED_PROGRESSION("ServiceJourney has too high speed progression"),
        WARNING_SPEED_PROGRESSION("ServiceJourney has high speed progression");

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
        return String.format("%s, " +
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
