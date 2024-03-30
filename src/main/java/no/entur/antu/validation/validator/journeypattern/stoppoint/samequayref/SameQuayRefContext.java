package no.entur.antu.validation.validator.journeypattern.stoppoint.samequayref;

import java.util.List;
import no.entur.antu.model.QuayId;
import no.entur.antu.model.ScheduledStopPointId;
import no.entur.antu.validation.AntuNetexData;
import org.rutebanken.netex.model.JourneyPattern;

public record SameQuayRefContext(
  String journeyPatternId,
  ScheduledStopPointId scheduledStopPointId,
  QuayId quayId
) {
  public static Builder builder(AntuNetexData.WithCommonData antuNetexData) {
    return new Builder(antuNetexData);
  }

  public static class Builder {

    private final AntuNetexData.WithCommonData antuNetexData;

    private Builder(AntuNetexData.WithCommonData antuNetexData) {
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
            antuNetexData.findQuayIdForScheduledStopPoint(stopPointId)
          )
        )
        .toList();
    }
  }
}
