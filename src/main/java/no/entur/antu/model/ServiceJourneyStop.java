package no.entur.antu.model;

import java.math.BigInteger;
import java.time.LocalTime;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import no.entur.antu.exception.AntuException;
import org.rutebanken.netex.model.TimetabledPassingTime;

public record ServiceJourneyStop(
  ScheduledStopPointId scheduledStopPointId,
  LocalTime arrivalTime,
  LocalTime departureTime,
  int arrivalDayOffset,
  int departureDayOffset
) {
  public static ServiceJourneyStop of(
    ScheduledStopPointId scheduledStopPointId,
    TimetabledPassingTime timetabledPassingTime
  ) {
    return new ServiceJourneyStop(
      scheduledStopPointId,
      timetabledPassingTime.getArrivalTime(),
      timetabledPassingTime.getDepartureTime(),
      Optional
        .ofNullable(timetabledPassingTime.getArrivalDayOffset())
        .map(BigInteger::intValue)
        .orElse(0),
      Optional
        .ofNullable(timetabledPassingTime.getDepartureDayOffset())
        .map(BigInteger::intValue)
        .orElse(0)
    );
  }

  public boolean isValid() {
    return (
      scheduledStopPointId != null &&
      (arrivalTime != null || departureTime != null) &&
      arrivalDayOffset >= 0 &&
      departureDayOffset >= 0
    );
  }

  /*
   * Used to encode data to store in redis.
   * Caution: Changes in this method can effect data stored in redis.
   */
  @Override
  public String toString() {
    String toReturn = "scheduledStopPointId(" + scheduledStopPointId + ")";

    if (arrivalTime != null) {
      toReturn = "arrival(" + arrivalTime + "§" + arrivalDayOffset + "),";
    }

    if (departureTime != null) {
      toReturn += "departure(" + departureTime + "§" + departureDayOffset + ")";
    }

    return toReturn;
  }

  /*
   * Used to encode data to store in redis.
   * Caution: Changes in this method can effect data stored in redis.
   */
  public static ServiceJourneyStop fromString(String passingTimeInfo) {
    if (passingTimeInfo != null) {
      /*
       * Here's the breakdown of the regex:
       * <p>
       * scheduledStopPointId\(([^)]+)\):
       * This matches scheduledStopPointId followed by anything inside parentheses.
       * ([^)]+) captures the content inside the parentheses.
       * <p>
       * (?:,arrival\(([^§]+)§([^)]+)\))?:
       * This matches ,arrival followed by two values inside parentheses separated by §.
       * ([^§]+) captures the first value and ([^)]+) captures the second value.
       * The (?: ... )? makes this whole part optional.
       * <p>
       * (?:,departure\(([^§]+)§([^)]+)\))?:
       * This matches ,departure followed by two values inside parentheses separated by §.
       * ([^§]+) captures the first value and ([^)]+) captures the second value.
       * The (?: ... )? makes this whole part optional.
       */
      Pattern pattern = Pattern.compile(
        "scheduledStopPointId\\(([^)]+)\\)(?:,arrival\\(([^§]+)§([^)]+)\\))?(?:,departure\\(([^§]+)§([^)]+)\\))?"
      );

      Matcher matcher = pattern.matcher(passingTimeInfo);

      if (matcher.find()) {
        String scheduledStopPointId = matcher.group(1);
        String arrivalTime = matcher.group(2);
        String arrivalDayOffset = matcher.group(3);
        String departureTime = matcher.group(4);
        String departureDayOffset = matcher.group(5);

        return new ServiceJourneyStop(
          new ScheduledStopPointId(scheduledStopPointId),
          arrivalTime != null ? LocalTime.parse(arrivalTime) : null,
          departureTime != null ? LocalTime.parse(departureTime) : null,
          arrivalDayOffset != null ? Integer.parseInt(arrivalDayOffset) : 0,
          departureDayOffset != null ? Integer.parseInt(departureDayOffset) : 0
        );
      } else {
        throw new AntuException(
          "Invalid passing time info: " + passingTimeInfo
        );
      }
    }
    return null;
  }
}
