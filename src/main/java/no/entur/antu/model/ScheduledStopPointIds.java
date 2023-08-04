package no.entur.antu.model;

import no.entur.antu.exception.AntuException;
import org.rutebanken.netex.model.ServiceLink;

public record ScheduledStopPointIds(
  ScheduledStopPointId from,
  ScheduledStopPointId to
) {
  public static ScheduledStopPointIds of(ServiceLink serviceLink) {
    return new ScheduledStopPointIds(
      ScheduledStopPointId.of(serviceLink.getFromPointRef()),
      ScheduledStopPointId.of(serviceLink.getToPointRef())
    );
  }

  /*
   * Used to encode data to store in redis.
   * Caution: Changes in this method can effect data stored in redis.
   */
  @Override
  public String toString() {
    return from.toString() + "§" + to.toString();
  }

  /*
   * Used to encode data to store in redis.
   * Caution: Changes in this method can effect data stored in redis.
   */
  public static ScheduledStopPointIds fromString(String scheduledStopPointIds) {
    if (scheduledStopPointIds != null) {
      String[] split = scheduledStopPointIds.split("§");
      if (split.length == 2) {
        return new ScheduledStopPointIds(
          new ScheduledStopPointId(split[0]),
          new ScheduledStopPointId(split[1])
        );
      } else {
        throw new AntuException(
          "Invalid scheduledStopPointIds string: " + scheduledStopPointIds
        );
      }
    }
    return null;
  }
}
