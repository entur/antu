package no.entur.antu.validation.validator.interchange.waittime;

import java.util.Optional;
import no.entur.antu.model.ScheduledStopPointId;
import no.entur.antu.model.ServiceJourneyStop;
import no.entur.antu.validation.AntuNetexData;
import org.rutebanken.netex.model.ServiceJourneyInterchange;
import org.rutebanken.netex.model.VehicleJourneyRefStructure;
import org.rutebanken.netex.model.VersionOfObjectRefStructure;

public record UnexpectedWaitTimeContext(
  String interchangeId,
  ScheduledStopPointId fromStopPoint,
  ScheduledStopPointId toStopPoint,
  String fromJourneyRef,
  String toJourneyRef,

  // ServiceJourneyStop at the fromStopPoint in fromJourneyRef from Cache
  ServiceJourneyStop fromServiceJourneyStop,

  // ServiceJourneySStop at the toStopPoint in toJourneyRef from Cache
  ServiceJourneyStop toServiceJourneyStop
) {
  public static UnexpectedWaitTimeContext of(
    AntuNetexData antuNetexData,
    ServiceJourneyInterchange serviceJourneyInterchange
  ) {
    ScheduledStopPointId fromStopPoint = ScheduledStopPointId.ofNullable(
      serviceJourneyInterchange.getFromPointRef()
    );

    ScheduledStopPointId toStopPoint = ScheduledStopPointId.ofNullable(
      serviceJourneyInterchange.getToPointRef()
    );

    Optional<VehicleJourneyRefStructure> fromJourneyRef = Optional.ofNullable(
      serviceJourneyInterchange.getFromJourneyRef()
    );

    Optional<VehicleJourneyRefStructure> toJourneyRef = Optional.ofNullable(
      serviceJourneyInterchange.getToJourneyRef()
    );

    return new UnexpectedWaitTimeContext(
      serviceJourneyInterchange.getId(),
      fromStopPoint,
      toStopPoint,
      fromJourneyRef.map(VersionOfObjectRefStructure::getRef).orElse(null),
      toJourneyRef.map(VersionOfObjectRefStructure::getRef).orElse(null),
      fromJourneyRef
        .map(journeyRef ->
          antuNetexData.serviceJourneyStopAtScheduleStopPoint(
            journeyRef,
            fromStopPoint
          )
        )
        .orElse(null),
      toJourneyRef
        .map(journeyRef ->
          antuNetexData.serviceJourneyStopAtScheduleStopPoint(
            journeyRef,
            toStopPoint
          )
        )
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
      fromServiceJourneyStop != null &&
      toServiceJourneyStop != null
    );
  }
}
