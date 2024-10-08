package no.entur.antu.validation.validator.journeypattern.stoppoint.samequayref;

import java.util.List;
import no.entur.antu.validation.AntuNetexData;
import org.entur.netex.validation.validator.model.QuayId;
import org.entur.netex.validation.validator.model.ScheduledStopPointId;
import org.rutebanken.netex.model.JourneyPattern;

public record SameQuayRefContext(
  String journeyPatternId,
  ScheduledStopPointId scheduledStopPointId,
  QuayId quayId
) {
  public boolean isValid() {
    return scheduledStopPointId != null && quayId != null;
  }

  public static Builder builder(AntuNetexData antuNetexData) {
    return new Builder(antuNetexData);
  }

  public static class Builder {

    private final AntuNetexData antuNetexData;

    private Builder(AntuNetexData antuNetexData) {
      this.antuNetexData = antuNetexData;
    }

    public List<SameQuayRefContext> build(JourneyPattern journeyPattern) {
      return AntuNetexData
        .stopPointsInJourneyPattern(journeyPattern)
        .map(ScheduledStopPointId::of)
        .map(stopPointId ->
          new SameQuayRefContext(
            journeyPattern.getId(),
            stopPointId,
            antuNetexData.quayIdForScheduledStopPoint(stopPointId)
          )
        )
        .toList();
    }
  }
}
