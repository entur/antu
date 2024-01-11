package no.entur.antu.validator.nonincreasingpassingtime;

import no.entur.antu.stoptime.StopTime;
import no.entur.antu.validator.ValidationError;

public record NonIncreasingPassingTimeError(
        RuleCode ruleCode,
        StopTime stopTime,
        String serviceJourneyId

) implements ValidationError {

    @Override
    public String getRuleCode() {
        return ruleCode.toString();
    }

    @Override
    public String validationReportEntryMessage() {
        return String.format(
                "%s. ServiceJourney = %s, TimetabledPassingTime = %s",
                ruleCode.getErrorMessage(),
                serviceJourneyId(),
                stopTime.timetabledPassingTimeId()
        );
    }

    enum RuleCode implements no.entur.antu.validator.RuleCode {
        TIMETABLED_PASSING_TIME_INCOMPLETE_TIME("ServiceJourney has incomplete TimetabledPassingTime"),
        TIMETABLED_PASSING_TIME_INCONSISTENT_TIME("ServiceJourney has inconsistent TimetabledPassingTime"),
        TIMETABLED_PASSING_TIME_NON_INCREASING_TIME("ServiceJourney has non-increasing TimetabledPassingTime");

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
