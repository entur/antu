package no.entur.antu.validation;

import com.google.common.collect.Multimap;
import jakarta.xml.bind.JAXBElement;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.jaxb.*;
import org.entur.netex.validation.validator.model.*;
import org.entur.netex.validation.validator.model.SimpleLine;
import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;
import org.rutebanken.netex.model.DatedServiceJourney;
import org.rutebanken.netex.model.DayTypeAssignment;
import org.rutebanken.netex.model.DayTypeAssignment_VersionStructure;
import org.rutebanken.netex.model.FlexibleLine;
import org.rutebanken.netex.model.FlexibleLineTypeEnumeration;
import org.rutebanken.netex.model.FlexibleStopPlace;
import org.rutebanken.netex.model.FlexibleStopPlacesInFrame_RelStructure;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.LinkInJourneyPattern;
import org.rutebanken.netex.model.LinkInLinkSequence_VersionedChildStructure;
import org.rutebanken.netex.model.LinksInJourneyPattern_RelStructure;
import org.rutebanken.netex.model.PointInLinkSequence_VersionedChildStructure;
import org.rutebanken.netex.model.PointsInJourneyPattern_RelStructure;
import org.rutebanken.netex.model.Route;
import org.rutebanken.netex.model.ServiceAlterationEnumeration;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.ServiceJourneyInterchange;
import org.rutebanken.netex.model.ServiceLink;
import org.rutebanken.netex.model.ServiceLinksInFrame_RelStructure;
import org.rutebanken.netex.model.Service_VersionFrameStructure;
import org.rutebanken.netex.model.Site_VersionFrameStructure;
import org.rutebanken.netex.model.StopPointInJourneyPattern;
import org.rutebanken.netex.model.Timetable_VersionFrameStructure;
import org.rutebanken.netex.model.TimetabledPassingTime;
import org.rutebanken.netex.model.TimetabledPassingTimes_RelStructure;
import org.rutebanken.netex.model.VehicleJourneyRefStructure;
import org.rutebanken.netex.model.VersionOfObjectRefStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record AntuNetexData(
  String validationReportId,
  NetexEntitiesIndex netexEntitiesIndex,
  NetexDataRepository netexDataRepository,
  StopPlaceRepository stopPlaceRepository
) {
  private static final Logger LOGGER = LoggerFactory.getLogger(
    AntuNetexData.class
  );

  public QuayCoordinates coordinatesForQuayId(QuayId quayid) {
    return stopPlaceRepository.getCoordinatesForQuayId(quayid);
  }

  public Stream<JourneyPattern> journeyPatterns() {
    return netexEntitiesIndex.getJourneyPatternIndex().getAll().stream();
  }

  /**
   * Find the quay id for the given scheduled stop point.
   * If the quay id is not found in the common data repository,
   * it will be looked up from the netex entities index.
   * <br>
   * Which basically means that if the PassengerStopAssignment is
   * not in Common file, it will be looked up from the line file.
   */
  public QuayId quayIdForScheduledStopPoint(
    ScheduledStopPointId scheduledStopPointId
  ) {
    if (scheduledStopPointId == null) {
      return null;
    }
    return netexDataRepository.hasQuayIds(validationReportId())
      ? netexDataRepository.quayIdForScheduledStopPoint(
        scheduledStopPointId,
        validationReportId()
      )
      : QuayId.ofValidId(
        netexEntitiesIndex()
          .getQuayIdByStopPointRefIndex()
          .get(scheduledStopPointId.id())
      );
  }

  public FromToScheduledStopPointId scheduledStopPointsForServiceLinkId(
    ServiceLinkId serviceLinkId
  ) {
    // Should extend this function to check line file if we don't find the
    // service links in common file. Same as findQuayIdForScheduledStopPoint.
    return netexDataRepository.fromToScheduledStopPointIdForServiceLink(
      serviceLinkId,
      validationReportId()
    );
  }

  public Map.Entry<ScheduledStopPointId, QuayCoordinates> coordinatesPerQuayId(
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

    QuayId quayId = quayIdForScheduledStopPoint(scheduledStopPointId);

    if (quayId == null) {
      return null;
    }

    QuayCoordinates coordinatesForQuayId =
      stopPlaceRepository.getCoordinatesForQuayId(quayId);
    return coordinatesForQuayId == null
      ? null
      : Map.entry(scheduledStopPointId, coordinatesForQuayId);
  }

  public String stopPointName(ScheduledStopPointId scheduledStopPointId) {
    QuayId quayId = quayIdForScheduledStopPoint(scheduledStopPointId);
    if (quayId == null) {
      LOGGER.debug(
        "Stop place name cannot be found due to missing stop point assignment."
      );
      return Optional
        .ofNullable(scheduledStopPointId)
        .map(ScheduledStopPointId::id)
        .orElse(null);
    }
    return Optional
      .ofNullable(stopPlaceRepository.getStopPlaceNameForQuayId(quayId))
      .orElse(quayId.id());
  }

  /**
   * Find the transport mode for the given service journey.
   * If the transport mode is not set on the service journey,
   * it will be looked up from the line or flexible line.
   */
  public AllVehicleModesOfTransportEnumeration transportMode(
    ServiceJourney serviceJourney
  ) {
    AllVehicleModesOfTransportEnumeration transportMode =
      serviceJourney.getTransportMode();
    if (transportMode == null) {
      JourneyPattern journeyPattern = netexEntitiesIndex
        .getJourneyPatternIndex()
        .get(serviceJourney.getJourneyPatternRef().getValue().getRef());

      return transportMode(journeyPattern);
    }
    return transportMode;
  }

  /**
   * Find the transport mode for the given journey pattern.
   * it will be looked up from the line or flexible line with FIXED Type
   */
  public AllVehicleModesOfTransportEnumeration transportMode(
    JourneyPattern journeyPattern
  ) {
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

    return isFixedFlexibleLine(flexibleLine)
      ? flexibleLine.getTransportMode()
      : null;
  }

  private static boolean isFixedFlexibleLine(FlexibleLine flexibleLine) {
    return (
      flexibleLine != null &&
      flexibleLine.getFlexibleLineType() == FlexibleLineTypeEnumeration.FIXED
    );
  }

  public SimpleLine lineInfo(String fileName) {
    return netexEntitiesIndex
      .getLineIndex()
      .getAll()
      .stream()
      .findFirst()
      .map(line -> SimpleLine.of(line, fileName))
      .orElse(
        netexEntitiesIndex
          .getFlexibleLineIndex()
          .getAll()
          .stream()
          .filter(AntuNetexData::isFixedFlexibleLine)
          .findFirst()
          .map(line -> SimpleLine.of(line, fileName))
          .orElse(null)
      );
  }

  public Stream<FlexibleStopPlace> flexibleStopPlaces() {
    return netexEntitiesIndex
      .getSiteFrames()
      .stream()
      .map(Site_VersionFrameStructure::getFlexibleStopPlaces)
      .filter(Objects::nonNull)
      .map(FlexibleStopPlacesInFrame_RelStructure::getFlexibleStopPlace)
      .flatMap(List::stream);
  }

  public Stream<ServiceLink> serviceLinks() {
    return serviceLinks(netexEntitiesIndex);
  }

  public static Stream<ServiceLink> serviceLinks(NetexEntitiesIndex index) {
    return index
      .getServiceFrames()
      .stream()
      .map(Service_VersionFrameStructure::getServiceLinks)
      .filter(Objects::nonNull)
      .map(ServiceLinksInFrame_RelStructure::getServiceLink)
      .flatMap(Collection::stream);
  }

  /**
   * Returns the Stream of all ServiceJourneys in all the TimeTableFrames.
   */
  public Stream<ServiceJourney> serviceJourneys() {
    return netexEntitiesIndex
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

  public ServiceJourney serviceJourney(
    VehicleJourneyRefStructure vehicleJourneyRefStructure
  ) {
    return Optional
      .ofNullable(vehicleJourneyRefStructure)
      .map(VehicleJourneyRefStructure::getRef)
      .map(serviceJourneyRef ->
        netexEntitiesIndex
          .getServiceJourneyIndex()
          .get(vehicleJourneyRefStructure.getRef())
      )
      .orElse(null);
  }

  public ServiceJourneyStop serviceJourneyStopAtScheduleStopPoint(
    VehicleJourneyRefStructure vehicleJourneyRefStructure,
    ScheduledStopPointId scheduledStopPointId
  ) {
    return serviceJourneyStops(vehicleJourneyRefStructure)
      .stream()
      .filter(serviceJourneyStop ->
        serviceJourneyStop.scheduledStopPointId().equals(scheduledStopPointId)
      )
      .findFirst()
      .orElse(null);
  }

  public List<ServiceJourneyStop> serviceJourneyStops(
    VehicleJourneyRefStructure vehicleJourneyRefStructure
  ) {
    return Optional
      .ofNullable(
        netexDataRepository.serviceJourneyStops(
          validationReportId(),
          ServiceJourneyId.ofValidId(vehicleJourneyRefStructure)
        )
      )
      .orElse(List.of());
  }

  /**
   * Returns the Stream of all ServiceJourneyInterchanges in all the TimeTableFrames.
   */
  public Stream<ServiceJourneyInterchange> serviceJourneyInterchanges() {
    return netexEntitiesIndex
      .getTimetableFrames()
      .stream()
      .map(Timetable_VersionFrameStructure::getJourneyInterchanges)
      .filter(Objects::nonNull)
      .flatMap(journeyInterchangesInFrame ->
        journeyInterchangesInFrame
          .getServiceJourneyPatternInterchangeOrServiceJourneyInterchange()
          .stream()
      )
      .filter(ServiceJourneyInterchange.class::isInstance)
      .map(ServiceJourneyInterchange.class::cast);
  }

  /**
   * Returns the Stream of all the valid ServiceJourneys in all the TimeTableFrames.
   * The valid serviceJourneys are those that have number of timetabledPassingTime equals to number of StopPointsInJourneyPattern.
   * This is validated with SERVICE_JOURNEY_10.
   */
  public Stream<ServiceJourney> validServiceJourneys() {
    return serviceJourneys()
      .filter(serviceJourney -> {
        JourneyPattern journeyPattern = journeyPattern(serviceJourney);
        if (journeyPattern == null) {
          return false;
        }
        return (
          stopPointsInJourneyPattern(journeyPattern).toList().size() ==
          timetabledPassingTimes(serviceJourney).toList().size()
        );
      });
  }

  /**
   * Returns the Stream of TimetabledPassingTimes for the given ServiceJourney.
   * Missing TimetabledPassingTimes is validated with SERVICE_JOURNEY_3
   */
  public Stream<TimetabledPassingTime> timetabledPassingTimes(
    ServiceJourney serviceJourney
  ) {
    return Optional
      .ofNullable(serviceJourney.getPassingTimes())
      .map(TimetabledPassingTimes_RelStructure::getTimetabledPassingTime)
      .stream()
      .flatMap(Collection::stream);
  }

  /**
   * Return the JourneyPattern for the given ServiceJourney.
   * Missing JourneyPatternRef on ServiceJourney is validated with SERVICE_JOURNEY_10
   */
  public JourneyPattern journeyPattern(ServiceJourney serviceJourney) {
    return Optional
      .ofNullable(serviceJourney.getJourneyPatternRef())
      .map(JAXBElement::getValue)
      .map(VersionOfObjectRefStructure::getRef)
      .map(ref -> netexEntitiesIndex.getJourneyPatternIndex().get(ref))
      .orElse(null);
  }

  /**
   * Return the StopPointInJourneyPattern ID of a given TimeTabledPassingTime.
   */
  public static String stopPointRef(
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

  public Map.Entry<ServiceJourneyId, List<ScheduledStopPointId>> scheduledStopPointIdsByServiceJourneyId(
    ServiceJourney serviceJourney
  ) {
    return Optional
      .ofNullable(ServiceJourneyId.ofValidId(serviceJourney))
      .map(serviceJourneyId ->
        new AbstractMap.SimpleEntry<>(
          serviceJourneyId,
          stopPointsInJourneyPattern(journeyPattern(serviceJourney))
            .map(stopPointInJourneyPattern ->
              stopPointInJourneyPattern
                .getScheduledStopPointRef()
                .getValue()
                .getRef()
            )
            .map(ScheduledStopPointId::new)
            .toList()
        )
      )
      .orElse(null);
  }

  public List<DayTypeAssignment> getAvailableDayTypeAssignments(
    ServiceJourney serviceJourney
  ) {
    Multimap<String, DayTypeAssignment> dayTypeAssignmentsByDayTypeIdIndex =
      netexEntitiesIndex().getDayTypeAssignmentsByDayTypeIdIndex();

    return serviceJourney
      .getDayTypes()
      .getDayTypeRef()
      .stream()
      .map(jaxbElement -> jaxbElement.getValue().getRef())
      .map(dayTypeAssignmentsByDayTypeIdIndex::get)
      .flatMap(Collection::stream)
      .filter(DayTypeAssignment_VersionStructure::isIsAvailable)
      .toList();
  }

  public List<DatedServiceJourney> datedServiceJourneys(
    ServiceJourney serviceJourney
  ) {
    return netexEntitiesIndex()
      .getDatedServiceJourneyIndex()
      .getAll()
      .stream()
      .filter(dsj ->
        dsj.getServiceAlteration() != ServiceAlterationEnumeration.REPLACED &&
        dsj.getServiceAlteration() != ServiceAlterationEnumeration.CANCELLATION
      )
      .toList();
  }

  /**
   * Find the stop points in journey pattern for the given journey pattern, sorted by order.
   */
  public static Stream<StopPointInJourneyPattern> stopPointsInJourneyPattern(
    JourneyPattern journeyPattern
  ) {
    return Optional
      .ofNullable(journeyPattern.getPointsInSequence())
      .map(
        PointsInJourneyPattern_RelStructure::getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern
      )
      .map(stopPointsInJourneyPattern ->
        stopPointsInJourneyPattern
          .stream()
          .filter(StopPointInJourneyPattern.class::isInstance)
          .map(StopPointInJourneyPattern.class::cast)
          .sorted(
            Comparator.comparing(
              PointInLinkSequence_VersionedChildStructure::getOrder
            )
          )
      )
      .orElse(Stream.empty());
  }

  /**
   * Find the links in journey pattern for the given journey pattern, sorted by order.
   */
  public static Stream<LinkInJourneyPattern> linksInJourneyPattern(
    JourneyPattern journeyPattern
  ) {
    return Optional
      .ofNullable(journeyPattern.getLinksInSequence())
      .map(
        LinksInJourneyPattern_RelStructure::getServiceLinkInJourneyPatternOrTimingLinkInJourneyPattern
      )
      .map(serviceLinksInJourneyPattern ->
        serviceLinksInJourneyPattern
          .stream()
          .filter(LinkInJourneyPattern.class::isInstance)
          .map(LinkInJourneyPattern.class::cast)
          .sorted(
            Comparator.comparing(
              LinkInLinkSequence_VersionedChildStructure::getOrder
            )
          )
      )
      .orElse(Stream.empty());
  }

  /**
   * Find the stop point in journey pattern for the
   * given stop point in journey pattern reference.
   */
  public static StopPointInJourneyPattern stopPointInJourneyPattern(
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
