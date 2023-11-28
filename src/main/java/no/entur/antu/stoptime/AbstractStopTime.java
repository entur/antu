package no.entur.antu.stoptime;

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
    public final String timetabledPassingTimeId() {
        return timetabledPassingTime.getId();
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

    protected boolean isRegularStopFollowedByAreaStopValid(FlexibleStopTime next) {
        return normalizedDepartureTimeOrElseArrivalTime() <= next.normalizedEarliestDepartureTime();
    }

    protected boolean isAreaStopFollowedByRegularStopValid(RegularStopTime next) {
        return normalizedLatestArrivalTime() <= next.normalizedArrivalTimeOrElseDepartureTime();
    }
}
