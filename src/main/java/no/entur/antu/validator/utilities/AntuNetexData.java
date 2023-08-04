package no.entur.antu.validator.utilities;

import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.entur.antu.commondata.CommonDataRepository;
import no.entur.antu.model.QuayCoordinates;
import no.entur.antu.model.QuayId;
import no.entur.antu.model.ScheduledStopPointId;
import no.entur.antu.stop.StopPlaceRepository;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;
import org.rutebanken.netex.model.FlexibleLine;
import org.rutebanken.netex.model.FlexibleLineTypeEnumeration;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.PointInLinkSequence_VersionedChildStructure;
import org.rutebanken.netex.model.Route;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.StopPointInJourneyPattern;
import org.rutebanken.netex.model.TimetabledPassingTime;

public class AntuNetexData {

  private final NetexEntitiesIndex entitiesIndex;
  private final String validationReportId;

  public AntuNetexData(
    NetexEntitiesIndex entitiesIndex,
    String validationReportId
  ) {
    this.entitiesIndex = entitiesIndex;
    this.validationReportId = validationReportId;
  }

  public NetexEntitiesIndex entitiesIndex() {
    return entitiesIndex;
  }

  public String validationReportId() {
    return validationReportId;
  }

  public WithCommonData withCommonData(
    CommonDataRepository commonDataRepository
  ) {
    return new AntuNetexData.WithCommonData(
      entitiesIndex,
      validationReportId,
      commonDataRepository
    );
  }

  public WithStopPlacesAndCommonData withStopPlacesAndCommonData(
    CommonDataRepository commonDataRepository,
    StopPlaceRepository stopPlaceRepository
  ) {
    return new WithStopPlacesAndCommonData(
      entitiesIndex,
      validationReportId,
      commonDataRepository,
      stopPlaceRepository
    );
  }

  public static class WithCommonData extends AntuNetexData {

    protected final CommonDataRepository commonDataRepository;

    private WithCommonData(
      NetexEntitiesIndex index,
      String validationReportId,
      CommonDataRepository commonDataRepository
    ) {
      super(index, validationReportId);
      this.commonDataRepository = commonDataRepository;
    }

    /**
     * Find the quay id for the given scheduled stop point.
     * If the quay id is not found in the common data repository,
     * it will be looked up from the netex entities index.
     * <br>
     * Which basically means that if the PassengerStopAssignment is
     * not in Common file, it will be looked up from the line file.
     */
    public QuayId findQuayIdForScheduledStopPoint(
      ScheduledStopPointId scheduledStopPointId
    ) {
      return commonDataRepository.hasQuayIds(validationReportId())
        ? commonDataRepository.findQuayIdForScheduledStopPoint(
          scheduledStopPointId,
          validationReportId()
        )
        : QuayId.ofNullable(
          entitiesIndex()
            .getQuayIdByStopPointRefIndex()
            .get(scheduledStopPointId.id())
        );
    }
  }

  public static class WithStopPlacesAndCommonData extends WithCommonData {

    private final StopPlaceRepository stopPlaceRepository;

    private WithStopPlacesAndCommonData(
      NetexEntitiesIndex index,
      String validationReportId,
      CommonDataRepository commonDataRepository,
      StopPlaceRepository stopPlaceRepository
    ) {
      super(index, validationReportId, commonDataRepository);
      this.stopPlaceRepository = stopPlaceRepository;
    }

    public Map.Entry<ScheduledStopPointId, QuayCoordinates> findCoordinatesPerQuayId(
      StopPointInJourneyPattern stopPointInJourneyPattern
    ) {
      String scheduledStopPointRef = stopPointInJourneyPattern
        .getScheduledStopPointRef()
        .getValue()
        .getRef();

      if (scheduledStopPointRef == null) {
        return null;
      }

      ScheduledStopPointId scheduledStopPointId = new ScheduledStopPointId(
        scheduledStopPointRef
      );

      QuayId quayId = findQuayIdForScheduledStopPoint(scheduledStopPointId);

      if (quayId == null) {
        return null;
      }

      QuayCoordinates coordinatesForQuayId =
        stopPlaceRepository.getCoordinatesForQuayId(quayId);
      return coordinatesForQuayId == null
        ? null
        : Map.entry(scheduledStopPointId, coordinatesForQuayId);
    }
  }

  /**
   * Find the transport mode for the given service journey.
   * If the transport mode is not set on the service journey,
   * it will be looked up from the line or flexible line.
   */
  public AllVehicleModesOfTransportEnumeration findTransportMode(
    ServiceJourney serviceJourney
  ) {
    AllVehicleModesOfTransportEnumeration transportMode =
      serviceJourney.getTransportMode();
    if (transportMode == null) {
      JourneyPattern journeyPattern = entitiesIndex
        .getJourneyPatternIndex()
        .get(serviceJourney.getJourneyPatternRef().getValue().getRef());

      return findTransportMode(journeyPattern);
    }
    return transportMode;
  }

  /**
   * Find the transport mode for the given journey pattern.
   * it will be looked up from the line or flexible line with FIXED Type
   */
  public AllVehicleModesOfTransportEnumeration findTransportMode(
    JourneyPattern journeyPattern
  ) {
    Route route = entitiesIndex
      .getRouteIndex()
      .get(journeyPattern.getRouteRef().getRef());
    Line line = entitiesIndex
      .getLineIndex()
      .get(route.getLineRef().getValue().getRef());

    if (line != null) {
      return line.getTransportMode();
    }

    FlexibleLine flexibleLine = entitiesIndex
      .getFlexibleLineIndex()
      .get(route.getLineRef().getValue().getRef());

    if (
      flexibleLine != null &&
      flexibleLine.getFlexibleLineType() == FlexibleLineTypeEnumeration.FIXED
    ) {
      return flexibleLine.getTransportMode();
    }
    return null;
  }

  public String getStopPointName(ScheduledStopPointId scheduledStopPointId) {
    return entitiesIndex
      .getScheduledStopPointIndex()
      .getLatestVersion(scheduledStopPointId.id())
      .getName()
      .getValue();
  }

  /**
   * Returns the Stream of all ServiceJourneys in all the TimeTableFrames.
   */
  public Stream<ServiceJourney> serviceJourneys() {
    return entitiesIndex
      .getTimetableFrames()
      .stream()
      .flatMap(timetableFrame ->
        timetableFrame
          .getVehicleJourneys()
          .getVehicleJourneyOrDatedVehicleJourneyOrNormalDatedVehicleJourney()
          .stream()
      )
      .filter(ServiceJourney.class::isInstance)
      .map(ServiceJourney.class::cast);
  }

  /**
   * Return the JourneyPattern for the given ServiceJourney.
   */
  public JourneyPattern getJourneyPattern(ServiceJourney serviceJourney) {
    return entitiesIndex
      .getJourneyPatternIndex()
      .get(serviceJourney.getJourneyPatternRef().getValue().getRef());
  }

  /**
   * Return the StopPointInJourneyPattern ID of a given TimeTabledPassingTime.
   */
  public static String getStopPointRef(
    TimetabledPassingTime timetabledPassingTime
  ) {
    return timetabledPassingTime
      .getPointInJourneyPatternRef()
      .getValue()
      .getRef();
  }

  /**
   * Return the mapping between stop point id and scheduled stop point id for the journey
   * pattern.
   */
  public static Map<String, ScheduledStopPointId> scheduledStopPointIdByStopPointId(
    JourneyPattern journeyPattern
  ) {
    return stopPointsInJourneyPattern(journeyPattern)
      .collect(
        Collectors.toMap(
          StopPointInJourneyPattern::getId,
          ScheduledStopPointId::of
        )
      );
  }

  /**
   * Find the stop points in journey pattern for the given journey pattern, sorted by order.
   */
  public static Stream<StopPointInJourneyPattern> stopPointsInJourneyPattern(
    JourneyPattern journeyPattern
  ) {
    return journeyPattern
      .getPointsInSequence()
      .getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern()
      .stream()
      .filter(StopPointInJourneyPattern.class::isInstance)
      .map(StopPointInJourneyPattern.class::cast)
      .sorted(
        Comparator.comparing(
          PointInLinkSequence_VersionedChildStructure::getOrder
        )
      );
  }

  /**
   * Find the stop point in journey pattern for the
   * given stop point in journey pattern reference.
   */
  public static StopPointInJourneyPattern findStopPointInJourneyPattern(
    String stopPointInJourneyPatternRef,
    JourneyPattern journeyPattern
  ) {
    return AntuNetexData
      .stopPointsInJourneyPattern(journeyPattern)
      .filter(stopPointInJourneyPattern ->
        stopPointInJourneyPattern.getId().equals(stopPointInJourneyPatternRef)
      )
      .findFirst()
      .orElse(null);
  }
}
