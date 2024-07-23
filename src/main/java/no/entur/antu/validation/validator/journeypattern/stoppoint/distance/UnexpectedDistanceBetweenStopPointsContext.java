package no.entur.antu.validation.validator.journeypattern.stoppoint.distance;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import no.entur.antu.model.QuayCoordinates;
import no.entur.antu.model.ScheduledStopPointId;
import no.entur.antu.validation.AntuNetexData;
import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;
import org.rutebanken.netex.model.JourneyPattern;

public record UnexpectedDistanceBetweenStopPointsContext(
  String journeyPatternRef,
  AllVehicleModesOfTransportEnumeration transportMode,
  List<ScheduledStopPointCoordinates> scheduledStopPointCoordinates
) {
  public boolean isValid() {
    return transportMode != null && !scheduledStopPointCoordinates.isEmpty();
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

  public static class Builder {

    private final AntuNetexData antuNetexData;

    public Builder(AntuNetexData antuNetexData) {
      this.antuNetexData = antuNetexData;
    }

    public UnexpectedDistanceBetweenStopPointsContext build(
      JourneyPattern journeyPattern
    ) {
      return new UnexpectedDistanceBetweenStopPointsContext(
        journeyPattern.getId(),
        antuNetexData.transportMode(journeyPattern),
        AntuNetexData
          .stopPointsInJourneyPattern(journeyPattern)
          .map(antuNetexData::coordinatesPerQuayId)
          .filter(Objects::nonNull)
          .map(ScheduledStopPointCoordinates::of)
          .toList()
      );
    }
  }
}
