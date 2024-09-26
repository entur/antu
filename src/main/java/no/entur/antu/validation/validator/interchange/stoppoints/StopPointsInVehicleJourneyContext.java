package no.entur.antu.validation.validator.interchange.stoppoints;

import java.util.List;
import java.util.Optional;
import no.entur.antu.validation.AntuNetexData;
import org.entur.netex.validation.validator.model.ScheduledStopPointId;
import org.entur.netex.validation.validator.model.ServiceJourneyStop;
import org.rutebanken.netex.model.ServiceJourneyInterchange;
import org.rutebanken.netex.model.VersionOfObjectRefStructure;

public record StopPointsInVehicleJourneyContext(
  String interchangeId,
  ScheduledStopPointId fromStopPoint,
  ScheduledStopPointId toStopPoint,
  String fromJourneyRef,
  String toJourneyRef,
  List<ServiceJourneyStop> serviceJourneyStopsForFromJourneyRef,
  List<ServiceJourneyStop> serviceJourneyStopsForToJourneyRef
) {
  public static StopPointsInVehicleJourneyContext of(
    AntuNetexData antuNetexData,
    ServiceJourneyInterchange serviceJourneyInterchange
  ) {
    return new StopPointsInVehicleJourneyContext(
      serviceJourneyInterchange.getId(),
      ScheduledStopPointId.ofNullable(
        serviceJourneyInterchange.getFromPointRef()
      ),
      ScheduledStopPointId.ofNullable(
        serviceJourneyInterchange.getToPointRef()
      ),
      Optional
        .ofNullable(serviceJourneyInterchange.getFromJourneyRef())
        .map(VersionOfObjectRefStructure::getRef)
        .orElse(null),
      Optional
        .ofNullable(serviceJourneyInterchange.getToJourneyRef())
        .map(VersionOfObjectRefStructure::getRef)
        .orElse(null),
      Optional
        .ofNullable(serviceJourneyInterchange.getFromJourneyRef())
        .map(antuNetexData::serviceJourneyStops)
        .orElse(null),
      Optional
        .ofNullable(serviceJourneyInterchange.getToJourneyRef())
        .map(antuNetexData::serviceJourneyStops)
        .orElse(null)
    );
  }

  public boolean isValid() {
    return (
      interchangeId != null &&
      Optional
        .ofNullable(fromStopPoint)
        .map(ScheduledStopPointId::id)
        .map(ScheduledStopPointId::isValid)
        .orElse(false) &&
      Optional
        .ofNullable(toStopPoint)
        .map(ScheduledStopPointId::id)
        .map(ScheduledStopPointId::isValid)
        .orElse(false) &&
      fromJourneyRef != null &&
      toJourneyRef != null &&
      serviceJourneyStopsForFromJourneyRef != null &&
      serviceJourneyStopsForToJourneyRef != null
    );
  }
}
