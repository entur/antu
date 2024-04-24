package no.entur.antu.validation.validator.journeypattern.stoppoint.distance;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import no.entur.antu.model.QuayCoordinates;
import no.entur.antu.model.ScheduledStopPointId;
import no.entur.antu.validation.AntuNetexData;
import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;
import org.rutebanken.netex.model.JourneyPattern;

public class UnexpectedDistanceContextBuilder {

  public record UnexpectedDistanceContext(
    String journeyPatternRef,
    AllVehicleModesOfTransportEnumeration transportMode,
    List<UnexpectedDistanceContextBuilder.ScheduledStopPointCoordinates> scheduledStopPointCoordinates
  ) {
    public boolean isValid() {
      return transportMode != null && !scheduledStopPointCoordinates.isEmpty();
    }
  }

  private final AntuNetexData antuNetexData;

  public UnexpectedDistanceContextBuilder(AntuNetexData antuNetexData) {
    this.antuNetexData = antuNetexData;
  }

  public UnexpectedDistanceContext build(JourneyPattern journeyPattern) {
    return new UnexpectedDistanceContext(
      journeyPattern.getId(),
      antuNetexData.findTransportMode(journeyPattern),
      AntuNetexData
        .stopPointsInJourneyPattern(journeyPattern)
        .map(antuNetexData::findCoordinatesPerQuayId)
        .filter(Objects::nonNull)
        .map(ScheduledStopPointCoordinates::of)
        .toList()
    );
  }

  public record ScheduledStopPointCoordinates(
    ScheduledStopPointId scheduledStopPointId,
    QuayCoordinates quayCoordinates
  ) {
    public static ScheduledStopPointCoordinates of(
      Map.Entry<ScheduledStopPointId, QuayCoordinates> scheduledStopPointIdQuayCoordinatesEntry
    ) {
      return new ScheduledStopPointCoordinates(
        scheduledStopPointIdQuayCoordinatesEntry.getKey(),
        scheduledStopPointIdQuayCoordinatesEntry.getValue()
      );
    }
  }
}
