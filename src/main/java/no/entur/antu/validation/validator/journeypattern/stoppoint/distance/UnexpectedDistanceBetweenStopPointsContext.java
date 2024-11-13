package no.entur.antu.validation.validator.journeypattern.stoppoint.distance;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import no.entur.antu.validation.validator.support.JourneyPatternUtils;
import no.entur.antu.validation.validator.support.NetexUtils;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
import org.entur.netex.validation.validator.model.QuayCoordinates;
import org.entur.netex.validation.validator.model.ScheduledStopPointId;
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

    private final JAXBValidationContext validationContext;

    public Builder(JAXBValidationContext validationContext) {
      this.validationContext = validationContext;
    }

    public UnexpectedDistanceBetweenStopPointsContext build(
      JourneyPattern journeyPattern
    ) {
      return new UnexpectedDistanceBetweenStopPointsContext(
        journeyPattern.getId(),
        validationContext.transportMode(journeyPattern),
        NetexUtils
          .stopPointsInJourneyPattern(journeyPattern)
          .stream()
          .map(stopPointInJourneyPattern ->
            JourneyPatternUtils.coordinatesPerQuayId(
              stopPointInJourneyPattern,
              validationContext
            )
          )
          .filter(Objects::nonNull)
          .map(ScheduledStopPointCoordinates::of)
          .toList()
      );
    }
  }
}
