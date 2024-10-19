package no.entur.antu.validation.validator.interchange.distance;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import no.entur.antu.validation.AntuNetexData;
import no.entur.antu.validation.validator.journeypattern.stoppoint.distance.UnexpectedDistanceBetweenStopPointsContext;
import org.entur.netex.validation.validator.model.QuayCoordinates;
import org.entur.netex.validation.validator.model.ScheduledStopPointId;
import org.rutebanken.netex.model.ServiceJourneyInterchange;

public record UnexpectedInterchangeDistanceContext(
  String interchangeId,
  ScheduledStopPointCoordinates fromStopPointCoordinates,
  ScheduledStopPointCoordinates toStopPointCoordinates
) {
  public static UnexpectedInterchangeDistanceContext of(
    AntuNetexData antuNetexData,
    ServiceJourneyInterchange serviceJourneyInterchange
  ) {
    Function<ScheduledStopPointId, ScheduledStopPointCoordinates> scheduledStopPointCoordinates =
      scheduledStopPointId ->
        new ScheduledStopPointCoordinates(
          scheduledStopPointId,
          antuNetexData.coordinatesForScheduledStopPoint(scheduledStopPointId)
        );

    return new UnexpectedInterchangeDistanceContext(
      serviceJourneyInterchange.getId(),
      Optional
        .ofNullable(
          ScheduledStopPointId.ofNullable(
            serviceJourneyInterchange.getFromPointRef()
          )
        )
        .map(scheduledStopPointCoordinates)
        .filter(ScheduledStopPointCoordinates::isValid)
        .orElse(null),
      Optional
        .ofNullable(
          ScheduledStopPointId.ofNullable(
            serviceJourneyInterchange.getToPointRef()
          )
        )
        .map(scheduledStopPointCoordinates)
        .filter(ScheduledStopPointCoordinates::isValid)
        .orElse(null)
    );
  }

  public boolean isValid() {
    return (
      interchangeId != null &&
      Optional
        .ofNullable(fromStopPointCoordinates)
        .map(ScheduledStopPointCoordinates::isValid)
        .orElse(false) &&
      Optional
        .ofNullable(toStopPointCoordinates)
        .map(ScheduledStopPointCoordinates::isValid)
        .orElse(false)
    );
  }

  public record ScheduledStopPointCoordinates(
    ScheduledStopPointId scheduledStopPointId,
    QuayCoordinates quayCoordinates
  ) {
    public static UnexpectedDistanceBetweenStopPointsContext.ScheduledStopPointCoordinates of(
      Map.Entry<ScheduledStopPointId, QuayCoordinates> scheduledStopPointIdQuayCoordinatesEntry
    ) {
      return new UnexpectedDistanceBetweenStopPointsContext.ScheduledStopPointCoordinates(
        scheduledStopPointIdQuayCoordinatesEntry.getKey(),
        scheduledStopPointIdQuayCoordinatesEntry.getValue()
      );
    }

    public boolean isValid() {
      return scheduledStopPointId != null && quayCoordinates != null;
    }
  }
}
