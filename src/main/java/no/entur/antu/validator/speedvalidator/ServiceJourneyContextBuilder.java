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

  private final CommonDataRepository commonDataRepository;
  private final StopPlaceRepository stopPlaceRepository;

  public ServiceJourneyContextBuilder(
    CommonDataRepository commonDataRepository,
    StopPlaceRepository stopPlaceRepository
  ) {
    this.commonDataRepository = commonDataRepository;
    this.stopPlaceRepository = stopPlaceRepository;
  }

  public ServiceJourneyContext build(
    NetexEntitiesIndex index,
    ServiceJourney serviceJourney,
    String validationReportId
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
            index,
            timetabledPassingTime,
            journeyPatternRef,
            validationReportId
          )
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
      findTransportMode(index, serviceJourney),
      stopPlaceCoordinatesPerTimetabledPassingTimeId
    );
  }

  private AllVehicleModesOfTransportEnumeration findTransportMode(
    NetexEntitiesIndex index,
    ServiceJourney serviceJourney
  ) {
    AllVehicleModesOfTransportEnumeration transportMode =
      serviceJourney.getTransportMode();
    if (transportMode == null) {
      JourneyPattern journeyPattern = index
        .getJourneyPatternIndex()
        .get(serviceJourney.getJourneyPatternRef().getValue().getRef());
      Route route = index
        .getRouteIndex()
        .get(journeyPattern.getRouteRef().getRef());
      Line line = index
        .getLineIndex()
        .get(route.getLineRef().getValue().getRef());
      return line.getTransportMode();
    }
    return transportMode;
  }

  private Map.Entry<String, StopPlaceCoordinates> findStopPlaceCoordinates(
    NetexEntitiesIndex index,
    TimetabledPassingTime timetabledPassingTime,
    String journeyPatternRef,
    String validationReportId
  ) {
    String stopPointInJourneyPatternRef = timetabledPassingTime
      .getPointInJourneyPatternRef()
      .getValue()
      .getRef();
    StopPointInJourneyPattern stopPointInJourneyPattern =
      getStopPointInJourneyPattern(
        stopPointInJourneyPatternRef,
        journeyPatternRef,
        index
      );
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
    String journeyPatternRef,
    NetexEntitiesIndex index
  ) {
    return index
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
