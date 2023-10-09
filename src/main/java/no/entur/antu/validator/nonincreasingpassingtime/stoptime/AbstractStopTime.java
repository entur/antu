package no.entur.antu.validator.nonincreasingpassingtime.stoptime;

import org.rutebanken.netex.model.TimetabledPassingTime;

import java.math.BigInteger;
import java.time.LocalTime;

/**
 * Wrapper around {@link TimetabledPassingTime} that provides a simpler interface for passing times
 * comparison. Passing times are exposed as seconds since midnight, taking into account the day
 * offset.
 * <p>
 * This class does not take Daylight Saving Time transitions into account, this is an error and
 * should be fixed. See https://github.com/opentripplanner/OpenTripPlanner/issues/5109
 */
abstract sealed class AbstractStopTime
        implements StopTime
        permits FlexibleStopTime, RegularStopTime {

    private final TimetabledPassingTime timetabledPassingTime;

    protected AbstractStopTime(TimetabledPassingTime timetabledPassingTime) {
        this.timetabledPassingTime = timetabledPassingTime;
    }

    @Override
    public final Object timetabledPassingTimeId() {
        return timetabledPassingTime.getId();
    }

    @Override
    public final boolean isStopTimesIncreasing(StopTime next) {
        // This can be replaced with pattern-matching or polymorphic inheritance, BUT as long as we
        // only have 4 cases the "if" keep the rules together and make it easier to read/get the hole
        // picture - so keep it together until more cases are added.
        if (this instanceof RegularStopTime) {
            if (next instanceof RegularStopTime) {
                return isRegularStopFollowedByRegularStopValid(next);
            } else {
                return isRegularStopFollowedByAreaStopValid(next);
            }
        } else {
            if (next instanceof RegularStopTime) {
                return isAreaStopFollowedByRegularStopValid(next);
            } else {
                return isAreaStopFollowedByAreaStopValid(next);
            }
        }
    }

    protected LocalTime arrivalTime() {
        return timetabledPassingTime.getArrivalTime();
    }

    protected BigInteger arrivalDayOffset() {
        return timetabledPassingTime.getArrivalDayOffset();
    }

    protected LocalTime latestArrivalTime() {
        return timetabledPassingTime.getLatestArrivalTime();
    }

    protected BigInteger latestArrivalDayOffset() {
        return timetabledPassingTime.getLatestArrivalDayOffset();
    }

    protected LocalTime departureTime() {
        return timetabledPassingTime.getDepartureTime();
    }

    protected BigInteger departureDayOffset() {
        return timetabledPassingTime.getDepartureDayOffset();
    }

    protected LocalTime earliestDepartureTime() {
        return timetabledPassingTime.getEarliestDepartureTime();
    }

    protected BigInteger earliestDepartureDayOffset() {
        return timetabledPassingTime.getEarliestDepartureDayOffset();
    }

    private boolean isRegularStopFollowedByRegularStopValid(StopTime next) {
        return (
                normalizedDepartureTimeOrElseArrivalTime() <= next.normalizedArrivalTimeOrElseDepartureTime()
        );
    }

    private boolean isAreaStopFollowedByAreaStopValid(StopTime next) {
        int earliestDepartureTime = normalizedEarliestDepartureTime();
        int nextEarliestDepartureTime = next.normalizedEarliestDepartureTime();
        int latestArrivalTime = normalizedLatestArrivalTime();
        int nextLatestArrivalTime = next.normalizedLatestArrivalTime();

        return (
                earliestDepartureTime <= nextEarliestDepartureTime &&
                        latestArrivalTime <= nextLatestArrivalTime
        );
    }

    private boolean isRegularStopFollowedByAreaStopValid(StopTime next) {
        return normalizedDepartureTimeOrElseArrivalTime() <= next.normalizedEarliestDepartureTime();
    }

    private boolean isAreaStopFollowedByRegularStopValid(StopTime next) {
        return normalizedLatestArrivalTime() <= next.normalizedArrivalTimeOrElseDepartureTime();
    }
}
