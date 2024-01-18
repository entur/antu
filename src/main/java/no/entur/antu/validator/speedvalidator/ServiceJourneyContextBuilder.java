package no.entur.antu.validator.speedvalidator;

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
import org.rutebanken.netex.model.*;

public class ServiceJourneyContextBuilder {

  public record ServiceJourneyContext(
    ServiceJourney serviceJourney,
    AllVehicleModesOfTransportEnumeration transportMode,
    Map<String, StopPlaceCoordinates> stopPlaceCoordinatesPerTimetabledPassingTimeId,
    DistanceCalculator distanceCalculator
  ) {
    public ServiceJourneyContext(
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

    public double calculateDistance(PassingTimes passingTimes) {
      return distanceCalculator.calculateDistance(
        passingTimes,
        this::getCoordinates
      );
    }

    private StopPlaceCoordinates getCoordinates(StopTime passingTime) {
      return stopPlaceCoordinatesPerTimetabledPassingTimeId.get(
        passingTime.timetabledPassingTimeId()
      );
    }

    public boolean hasValidCoordinates(PassingTimes passingTimes) {
      return (
        getCoordinates(passingTimes.from()) != null &&
        getCoordinates(passingTimes.to()) != null
      );
    }
  }

  private final String validationReportId;
  private final NetexEntitiesIndex netexEntitiesIndex;
  private final CommonDataRepository commonDataRepository;
  private final StopPlaceRepository stopPlaceRepository;

  public ServiceJourneyContextBuilder(
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

  public ServiceJourneyContext build(
    ServiceJourney serviceJourney
  ) {
    String journeyPatternRef = serviceJourney
      .getJourneyPatternRef()
      .getValue()
      .getRef();
    Map<String, StopPlaceCoordinates> stopPlaceCoordinatesPerTimetabledPassingTimeId =
      serviceJourney
        .getPassingTimes()
        .getTimetabledPassingTime()
        .stream()
        .map(timetabledPassingTime ->
          findStopPlaceCoordinates(
            timetabledPassingTime,
            journeyPatternRef)
        )
        .filter(Objects::nonNull)
        .collect(
          Collectors.toMap(
            Map.Entry::getKey,
            Map.Entry::getValue,
            (previous, latest) -> latest
          )
        );
    return new ServiceJourneyContext(
      serviceJourney,
      findTransportMode(serviceJourney),
      stopPlaceCoordinatesPerTimetabledPassingTimeId
    );
  }

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
      return line.getTransportMode();
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
        journeyPatternRef);
    if (stopPointInJourneyPattern != null) {
      String scheduledStopPointRef = stopPointInJourneyPattern
        .getScheduledStopPointRef()
        .getValue()
        .getRef();
      QuayId quayId = commonDataRepository.findQuayIdForScheduledStopPoint(
        scheduledStopPointRef,
        validationReportId
      );
      StopPlaceCoordinates coordinatesForQuayId =
        stopPlaceRepository.getCoordinatesForQuayId(quayId);
      return coordinatesForQuayId == null
        ? null
        : Map.entry(timetabledPassingTime.getId(), coordinatesForQuayId);
    }
    return null;
  }

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
