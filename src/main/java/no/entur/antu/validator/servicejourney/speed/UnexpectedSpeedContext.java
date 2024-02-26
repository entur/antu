package no.entur.antu.validator.servicejourney.speed;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import no.entur.antu.commondata.CommonDataRepository;
import no.entur.antu.model.QuayId;
import no.entur.antu.model.StopPlaceCoordinates;
import no.entur.antu.stop.StopPlaceRepository;
import no.entur.antu.stoptime.PassingTimes;
import no.entur.antu.stoptime.StopTime;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;
import org.rutebanken.netex.model.FlexibleLine;
import org.rutebanken.netex.model.FlexibleLineTypeEnumeration;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.Route;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.StopPointInJourneyPattern;
import org.rutebanken.netex.model.TimetabledPassingTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record UnexpectedSpeedContext(
  ServiceJourney serviceJourney,
  AllVehicleModesOfTransportEnumeration transportMode,
  Map<String, StopPlaceCoordinates> stopPlaceCoordinatesPerTimetabledPassingTimeId,
  DistanceCalculator distanceCalculator
) {
  public UnexpectedSpeedContext(
    ServiceJourney serviceJourney,
    AllVehicleModesOfTransportEnumeration transportMode,
    Map<String, StopPlaceCoordinates> stopPlaceCoordinatesPerTimetabledPassingTimeId
  ) {
    this(
      serviceJourney,
      transportMode,
      stopPlaceCoordinatesPerTimetabledPassingTimeId,
      new DistanceCalculator()
    );
  }

  public boolean isValid() {
    return (
      stopPlaceCoordinatesPerTimetabledPassingTimeId != null &&
      !stopPlaceCoordinatesPerTimetabledPassingTimeId.isEmpty() &&
      transportMode != null
    );
  }

  public double calculateDistance(PassingTimes passingTimes) {
    return distanceCalculator.calculateDistance(
      passingTimes,
      this::getCoordinates
    );
  }

  public boolean hasValidCoordinates(PassingTimes passingTimes) {
    return (
      getCoordinates(passingTimes.from()) != null &&
      getCoordinates(passingTimes.to()) != null
    );
  }

  private StopPlaceCoordinates getCoordinates(StopTime passingTime) {
    return stopPlaceCoordinatesPerTimetabledPassingTimeId.get(
      passingTime.timetabledPassingTimeId()
    );
  }

  public static final class Builder {

    private static final Logger LOGGER = LoggerFactory.getLogger(Builder.class);

    private final String validationReportId;
    private final NetexEntitiesIndex netexEntitiesIndex;
    private final CommonDataRepository commonDataRepository;
    private final StopPlaceRepository stopPlaceRepository;

    public Builder(
      String validationReportId,
      NetexEntitiesIndex netexEntitiesIndex,
      CommonDataRepository commonDataRepository,
      StopPlaceRepository stopPlaceRepository
    ) {
      this.validationReportId = validationReportId;
      this.netexEntitiesIndex = netexEntitiesIndex;
      this.commonDataRepository = commonDataRepository;
      this.stopPlaceRepository = stopPlaceRepository;
    }

    public UnexpectedSpeedContext build(ServiceJourney serviceJourney) {
      String journeyPatternRef = serviceJourney
        .getJourneyPatternRef()
        .getValue()
        .getRef();
      Map<String, StopPlaceCoordinates> stopPlaceCoordinatesPerTimetabledPassingTimeId =
        serviceJourney
          .getPassingTimes()
          .getTimetabledPassingTime()
          .stream()
          .filter(timetabledPassingTime -> timetabledPassingTime.getId() != null
          )
          .map(timetabledPassingTime ->
            findStopPlaceCoordinates(timetabledPassingTime, journeyPatternRef)
          )
          .filter(Objects::nonNull)
          .collect(
            Collectors.toMap(
              Map.Entry::getKey,
              Map.Entry::getValue,
              (previous, latest) -> latest
            )
          );
      return new UnexpectedSpeedContext(
        serviceJourney,
        findTransportMode(serviceJourney),
        stopPlaceCoordinatesPerTimetabledPassingTimeId
      );
    }

    /**
     * Find the transport mode for the given service journey.
     * If the transport mode is not set on the service journey,
     * it will be looked up from the line or flexible line.
     */
    private AllVehicleModesOfTransportEnumeration findTransportMode(
      ServiceJourney serviceJourney
    ) {
      AllVehicleModesOfTransportEnumeration transportMode =
        serviceJourney.getTransportMode();
      if (transportMode == null) {
        JourneyPattern journeyPattern = netexEntitiesIndex
          .getJourneyPatternIndex()
          .get(serviceJourney.getJourneyPatternRef().getValue().getRef());
        Route route = netexEntitiesIndex
          .getRouteIndex()
          .get(journeyPattern.getRouteRef().getRef());
        Line line = netexEntitiesIndex
          .getLineIndex()
          .get(route.getLineRef().getValue().getRef());

        if (line != null) {
          return line.getTransportMode();
        }

        FlexibleLine flexibleLine = netexEntitiesIndex
          .getFlexibleLineIndex()
          .get(route.getLineRef().getValue().getRef());

        if (
          flexibleLine != null &&
          flexibleLine.getFlexibleLineType() ==
          FlexibleLineTypeEnumeration.FIXED
        ) {
          return flexibleLine.getTransportMode();
        }

        return null;
      }
      return transportMode;
    }

    private Map.Entry<String, StopPlaceCoordinates> findStopPlaceCoordinates(
      TimetabledPassingTime timetabledPassingTime,
      String journeyPatternRef
    ) {
      String stopPointInJourneyPatternRef = timetabledPassingTime
        .getPointInJourneyPatternRef()
        .getValue()
        .getRef();
      StopPointInJourneyPattern stopPointInJourneyPattern =
        getStopPointInJourneyPattern(
          stopPointInJourneyPatternRef,
          journeyPatternRef
        );
      if (stopPointInJourneyPattern != null) {
        String scheduledStopPointRef = stopPointInJourneyPattern
          .getScheduledStopPointRef()
          .getValue()
          .getRef();

        // If the quay id is not found in the common data repository, it will be looked up from the netex entities index.
        // Which basically means that if the PassengerStopAssignment are not in Common file, it will be looked up from the line file.
        QuayId quayId = commonDataRepository.hasQuayIds(validationReportId)
          ? commonDataRepository.findQuayIdForScheduledStopPoint(
            scheduledStopPointRef,
            validationReportId
          )
          : QuayId.ofNullable(
            netexEntitiesIndex
              .getQuayIdByStopPointRefIndex()
              .get(scheduledStopPointRef)
          );

        if (quayId == null) {
          LOGGER.warn(
            "Quay id not found for scheduled stop point with id: {}",
            scheduledStopPointRef
          );
          return null;
        }

        StopPlaceCoordinates coordinatesForQuayId =
          stopPlaceRepository.getCoordinatesForQuayId(quayId);
        return coordinatesForQuayId == null
          ? null
          : Map.entry(timetabledPassingTime.getId(), coordinatesForQuayId);
      }
      return null;
    }

    /**
     * Find the stop point in journey pattern for the given stop point in journey pattern reference.
     */
    private StopPointInJourneyPattern getStopPointInJourneyPattern(
      String stopPointInJourneyPatternRef,
      String journeyPatternRef
    ) {
      return netexEntitiesIndex
        .getJourneyPatternIndex()
        .get(journeyPatternRef)
        .getPointsInSequence()
        .getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern()
        .stream()
        .filter(StopPointInJourneyPattern.class::isInstance)
        .map(StopPointInJourneyPattern.class::cast)
        .filter(stopPointInJourneyPattern ->
          stopPointInJourneyPattern.getId().equals(stopPointInJourneyPatternRef)
        )
        .findFirst()
        .orElse(null);
    }
  }
}
