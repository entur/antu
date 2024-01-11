package no.entur.antu.validator.speedprogressionvalidator;

import no.entur.antu.validator.ValidationError;

public record SameDepartureArrivalTimeError (
        String serviceJourneyId,
        PassingTimes passingTimes,
        RuleCode ruleCode) implements ValidationError {

    public enum RuleCode {
        SAME_DEPARTURE_ARRIVAL_TIME("Same departure/arrival time for consecutive stops");

        private final String errorMessage;

        RuleCode(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    @Override
    public String getRuleCode() {
        return ruleCode.toString();
    }

    @Override
    public String validationReportEntryMessage() {
        return String.format("%s, " +
                        "ServiceJourneyId = %s, " +
                        "from TimetabledPassingTime = %s, " +
                        "to TimetabledPassingTime = %s",
                ruleCode().getErrorMessage(),
                serviceJourneyId,
                passingTimes().from().timetabledPassingTimeId(),
                passingTimes().to().timetabledPassingTimeId()
        );
    }
}
