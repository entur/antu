package no.entur.antu.stoptime;

import org.rutebanken.netex.model.TimetabledPassingTime;

/**
 * Wrapper around {@link TimetabledPassingTime} that provides a simpler interface
 * for passing times comparison.
 * Passing times are exposed as seconds since midnight, taking into account the day offset.
 */
final class RegularStopTime extends AbstractStopTime {

  RegularStopTime(TimetabledPassingTime timetabledPassingTime) {
    super(timetabledPassingTime);
  }

  @Override
  public boolean isComplete() {
    return hasArrivalTime() || hasDepartureTime();
  }

  @Override
  public boolean isConsistent() {
    return (
      arrivalTime() == null ||
      departureTime() == null ||
      normalizedDepartureTime() >= normalizedArrivalTime()
    );
  }

  @Override
  public boolean isStopTimesIncreasing(StopTime next) {
    if (next instanceof RegularStopTime regularStopTime) {
      return isRegularStopFollowedByRegularStopValid(regularStopTime);
    }
    return isRegularStopFollowedByAreaStopValid((FlexibleStopTime) next);
  }

  @Override
  public int getStopTimeDiff(StopTime given) {
    // TODO: This should be fixed. We need to take into account the type of given.
    //  Is it the same type as this, or not. See how we have done in
    //  isRegularStopFollowedByRegularStopValid, isAreaStopFollowedByAreaStopValid,
    //  isRegularStopFollowedByAreaStopValid, isAreaStopFollowedByRegularStopValid

    if (given instanceof RegularStopTime) {
      return (
        given.normalizedArrivalTimeOrElseDepartureTime() -
        normalizedDepartureTimeOrElseArrivalTime()
      );
    }
    return (
      given.normalizedLatestArrivalTime() -
      normalizedDepartureTimeOrElseArrivalTime()
    );
  }

  @Override
  public int normalizedEarliestDepartureTime() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int normalizedLatestArrivalTime() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int normalizedDepartureTimeOrElseArrivalTime() {
    return hasDepartureTime()
      ? normalizedDepartureTime()
      : normalizedArrivalTime();
  }

  @Override
  public int normalizedArrivalTimeOrElseDepartureTime() {
    return hasArrivalTime()
      ? normalizedArrivalTime()
      : normalizedDepartureTime();
  }

  /**
   * Return the elapsed time in second between midnight and the departure time, taking into account
   * the day offset.
   */
  private int normalizedDepartureTime() {
    return elapsedTimeSinceMidnight(departureTime(), departureDayOffset());
  }

  /**
   * Return the elapsed time in second between midnight and the arrival time, taking into account
   * the day offset.
   */
  private int normalizedArrivalTime() {
    return elapsedTimeSinceMidnight(arrivalTime(), arrivalDayOffset());
  }

  private boolean hasArrivalTime() {
    return arrivalTime() != null;
  }

  private boolean hasDepartureTime() {
    return departureTime() != null;
  }

  private boolean isRegularStopFollowedByRegularStopValid(
    RegularStopTime next
  ) {
    return (
      normalizedDepartureTimeOrElseArrivalTime() <=
      next.normalizedArrivalTimeOrElseDepartureTime()
    );
  }

  @Override
  public boolean isArrivalInMinutesResolution() {
    return hasArrivalTime()
      ? arrivalTime().getSecond() == 0
      : departureTime().getSecond() == 0;
  }

  @Override
  public boolean isDepartureInMinutesResolution() {
    return hasDepartureTime()
      ? departureTime().getSecond() == 0
      : arrivalTime().getSecond() == 0;
  }
}
