package no.entur.antu.stoptime;

import org.rutebanken.netex.model.TimetabledPassingTime;

/**
 * Wrapper around {@link TimetabledPassingTime} that provides a simpler interface
 * for passing times comparison.
 * Passing times are exposed as seconds since midnight, taking into account the day offset.
 */
final class FlexibleStopTime extends AbstractStopTime {

  FlexibleStopTime(TimetabledPassingTime timetabledPassingTime) {
    super(timetabledPassingTime);
  }

  @Override
  public boolean isComplete() {
    return hasLatestArrivalTime() && hasEarliestDepartureTime();
  }

  @Override
  public boolean isConsistent() {
    return (
      !isComplete() ||
      normalizedLatestArrivalTime() >= normalizedEarliestDepartureTime()
    );
  }

  @Override
  public boolean isStopTimesIncreasing(StopTime next) {
    if (next instanceof RegularStopTime regularStopTime) {
      return isAreaStopFollowedByRegularStopValid(regularStopTime);
    }
    return isAreaStopFollowedByAreaStopValid((FlexibleStopTime) next);
  }

  @Override
  public int getStopTimeDiff(StopTime given) {
    // TODO: This should be fixed. We need to take into account the type of given.
    //  Is it the same type as this, or not. See how we have done in
    //  isRegularStopFollowedByRegularStopValid, isAreaStopFollowedByAreaStopValid,
    //  isRegularStopFollowedByAreaStopValid, isAreaStopFollowedByRegularStopValid

    if (given instanceof FlexibleStopTime) {
      return isComplete()
        ? normalizedEarliestDepartureTime() - normalizedLatestArrivalTime()
        : 0;
    }
    return (
      given.normalizedEarliestDepartureTime() -
      normalizedArrivalTimeOrElseDepartureTime()
    );
  }

  @Override
  public int normalizedEarliestDepartureTime() {
    return elapsedTimeSinceMidnight(
      earliestDepartureTime(),
      earliestDepartureDayOffset()
    );
  }

  @Override
  public int normalizedLatestArrivalTime() {
    return elapsedTimeSinceMidnight(
      latestArrivalTime(),
      latestArrivalDayOffset()
    );
  }

  @Override
  public int normalizedDepartureTimeOrElseArrivalTime() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int normalizedArrivalTimeOrElseDepartureTime() {
    throw new UnsupportedOperationException();
  }

  private boolean hasLatestArrivalTime() {
    return latestArrivalTime() != null;
  }

  private boolean hasEarliestDepartureTime() {
    return earliestDepartureTime() != null;
  }

  private boolean isAreaStopFollowedByAreaStopValid(FlexibleStopTime next) {
    int earliestDepartureTime = normalizedEarliestDepartureTime();
    int nextEarliestDepartureTime = next.normalizedEarliestDepartureTime();
    int latestArrivalTime = normalizedLatestArrivalTime();
    int nextLatestArrivalTime = next.normalizedLatestArrivalTime();

    return (
      earliestDepartureTime <= nextEarliestDepartureTime &&
      latestArrivalTime <= nextLatestArrivalTime
    );
  }

  @Override
  public boolean isArrivalInMinutesResolution() {
    return latestArrivalTime().getSecond() == 0;
  }

  @Override
  public boolean isDepartureInMinutesResolution() {
    return earliestDepartureTime().getSecond() == 0;
  }
}
