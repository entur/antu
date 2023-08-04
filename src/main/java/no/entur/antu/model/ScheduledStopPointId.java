package no.entur.antu.model;

import no.entur.antu.exception.AntuException;
import org.rutebanken.netex.model.StopPointInJourneyPattern;

public record ScheduledStopPointId(String id) {
  public ScheduledStopPointId {
    if (!isValid(id)) {
      throw new AntuException("Invalid scheduled stop point id: " + id);
    }
  }

  public static ScheduledStopPointId of(
    StopPointInJourneyPattern stopPointInJourneyPattern
  ) {
    return new ScheduledStopPointId(
      stopPointInJourneyPattern.getScheduledStopPointRef().getValue().getRef()
    );
  }

  public static boolean isValid(String id) {
    return id != null && id.contains(":ScheduledStopPoint:");
  }

  /*
   * Used to encode data to store in redis.
   * Caution: Changes in this method can effect data stored in redis.
   */
  @Override
  public String toString() {
    return id();
  }
}
