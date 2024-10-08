package no.entur.antu.validation.validator.servicejourney.speed;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import no.entur.antu.stoptime.PassingTimes;
import no.entur.antu.stoptime.StopTime;
import no.entur.antu.validation.AntuNetexData;
import org.entur.netex.validation.validator.model.QuayCoordinates;
import org.entur.netex.validation.validator.model.ScheduledStopPointId;
import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.StopPointInJourneyPattern;
import org.rutebanken.netex.model.TimetabledPassingTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record UnexpectedSpeedContext(
  ServiceJourney serviceJourney,
  AllVehicleModesOfTransportEnumeration transportMode,
  Map<ScheduledStopPointId, QuayCoordinates> quayCoordinatesPerScheduledStopPointId,
  Distances distances
) {
  public UnexpectedSpeedContext(
    ServiceJourney serviceJourney,
    AllVehicleModesOfTransportEnumeration transportMode,
    Map<ScheduledStopPointId, QuayCoordinates> quayCoordinatesPerScheduledStopPointId
  ) {
    this(
      serviceJourney,
      transportMode,
      quayCoordinatesPerScheduledStopPointId,
      // TODO: Creating the new Distance for each ServiceJourney, is this correct?
      //  Is this even helpful, even if we fix it. Needs profiling.
      new Distances()
    );
  }

  public boolean isValid() {
    return (
      quayCoordinatesPerScheduledStopPointId != null &&
      !quayCoordinatesPerScheduledStopPointId.isEmpty() &&
      transportMode != null
    );
  }

  public double calculateDistance(PassingTimes passingTimes) {
    return distances.findDistance(passingTimes, this::getCoordinates);
  }

  public boolean hasValidCoordinates(PassingTimes passingTimes) {
    return (
      getCoordinates(passingTimes.from()) != null &&
      getCoordinates(passingTimes.to()) != null
    );
  }

  private QuayCoordinates getCoordinates(StopTime passingTime) {
    return quayCoordinatesPerScheduledStopPointId.get(
      passingTime.scheduledStopPointId()
    );
  }

  public static final class Builder {

    private static final Logger LOGGER = LoggerFactory.getLogger(Builder.class);
    private final AntuNetexData antuNetexData;

    public Builder(AntuNetexData antuNetexData) {
      this.antuNetexData = antuNetexData;
    }

    public UnexpectedSpeedContext build(ServiceJourney serviceJourney) {
      JourneyPattern journeyPattern = antuNetexData.journeyPattern(
        serviceJourney
      );

      if (journeyPattern == null) {
        LOGGER.warn(
          "No journey pattern ref found on service journey with id: {}",
          serviceJourney.getId()
        );
        return new UnexpectedSpeedContext(serviceJourney, null, Map.of());
      }

      return new UnexpectedSpeedContext(
        serviceJourney,
        antuNetexData.transportMode(serviceJourney),
        antuNetexData
          .timetabledPassingTimes(serviceJourney)
          .map(timetabledPassingTime ->
            findQuayCoordinates(timetabledPassingTime, journeyPattern)
          )
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

    private Map.Entry<ScheduledStopPointId, QuayCoordinates> findQuayCoordinates(
      TimetabledPassingTime timetabledPassingTime,
      JourneyPattern journeyPattern
    ) {
      String stopPointInJourneyPatternRef = AntuNetexData.stopPointRef(
        timetabledPassingTime
      );
      StopPointInJourneyPattern stopPointInJourneyPattern =
        AntuNetexData.stopPointInJourneyPattern(
          stopPointInJourneyPatternRef,
          journeyPattern
        );
      if (stopPointInJourneyPattern == null) {
        LOGGER.warn(
          "Stop point in journey pattern not found for timetabled passing time with id: {}",
          timetabledPassingTime.getId()
        );
        return null;
      }

      Map.Entry<ScheduledStopPointId, QuayCoordinates> coordinatesPerQuayId =
        antuNetexData.coordinatesPerQuayId(stopPointInJourneyPattern);

      if (coordinatesPerQuayId == null) {
        LOGGER.warn(
          "Quay id not found for stop point in journey pattern with id: {}",
          stopPointInJourneyPatternRef
        );
      }
      return coordinatesPerQuayId;
    }
  }
}
