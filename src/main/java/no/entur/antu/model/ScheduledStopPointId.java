package no.entur.antu.model;

import java.util.Optional;
import no.entur.antu.exception.AntuException;
import org.rutebanken.netex.model.ScheduledStopPointRefStructure;
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
    return of(stopPointInJourneyPattern.getScheduledStopPointRef().getValue());
  }

  public static ScheduledStopPointId of(
    ScheduledStopPointRefStructure scheduledStopPointRef
  ) {
    return new ScheduledStopPointId(scheduledStopPointRef.getRef());
  }

  public static ScheduledStopPointId ofNullable(
    ScheduledStopPointRefStructure scheduledStopPointRef
  ) {
    return Optional
      .ofNullable(scheduledStopPointRef)
      .map(ScheduledStopPointRefStructure::getRef)
      .map(ScheduledStopPointId::new)
      .orElse(null);
  }

  public static ScheduledStopPointId ofValidId(
    ScheduledStopPointRefStructure scheduledStopPointRef
  ) {
    return Optional
      .ofNullable(scheduledStopPointRef)
      .map(ScheduledStopPointRefStructure::getRef)
      .filter(ScheduledStopPointId::isValid)
      .map(ScheduledStopPointId::new)
      .orElse(null);
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
