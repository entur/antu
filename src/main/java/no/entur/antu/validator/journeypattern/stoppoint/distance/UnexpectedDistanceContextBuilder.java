package no.entur.antu.validator.journeypattern.stoppoint.distance;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import no.entur.antu.model.QuayCoordinates;
import no.entur.antu.model.ScheduledStopPointId;
import no.entur.antu.validator.utilities.AntuNetexData;
import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;
import org.rutebanken.netex.model.JourneyPattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnexpectedDistanceContextBuilder {

  public record UnexpectedDistanceContext(
    String journeyPatternRef,
    AllVehicleModesOfTransportEnumeration transportMode,
    Map<ScheduledStopPointId, QuayCoordinates> quays
  ) {}

  private final AntuNetexData.WithStopPlacesAndCommonData antuNetexData;

  public UnexpectedDistanceContextBuilder(
    AntuNetexData.WithStopPlacesAndCommonData antuNetexData
  ) {
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
        .collect(
          Collectors.toMap(
            Map.Entry::getKey,
            Map.Entry::getValue,
            (previous, latest) -> latest
          )
        )
    );
  }
}
