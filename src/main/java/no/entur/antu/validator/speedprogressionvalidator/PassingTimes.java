package no.entur.antu.validator.speedprogressionvalidator;

import no.entur.antu.stoptime.StopTime;

public record PassingTimes(StopTime from, StopTime to) {
    public boolean isValid() {
        return from.isComplete() && from.isConsistent()
                && (to.isComplete() || to.isConsistent()
                || from.isStopTimesIncreasing(to));
    }

    public boolean hasValidTimeDifference() {
        return getTimeDifference() >= 0;
    }

    /**
     * Returns time difference in seconds
     */
    public int getTimeDifference() {
        return from.getStopTimeDiff(to);
    }

    public boolean hasMinutesResolution() {
        return from.isDepartureInMinutesResolution()
                && to.isArrivalInMinutesResolution();
    }

    public double minimumPossibleTimeDifference(int maxErrorSeconds) {
        int timeDifferenceInSeconds = getTimeDifference();
        return hasMinutesResolution() ? Math.max(timeDifferenceInSeconds - maxErrorSeconds, 1) : timeDifferenceInSeconds;
    }

    public double maximumPossibleTimeDifference(int maxErrorSeconds) {
        int timeDifferenceInSeconds = getTimeDifference();
        return hasMinutesResolution() ? timeDifferenceInSeconds + maxErrorSeconds : timeDifferenceInSeconds;
    }
}
