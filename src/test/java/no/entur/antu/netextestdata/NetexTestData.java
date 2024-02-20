package no.entur.antu.netextestdata;

import static no.entur.antu.netextestdata.MappingSupport.createJaxbElement;
import static no.entur.antu.netextestdata.MappingSupport.createWrappedRef;

import jakarta.xml.bind.JAXBElement;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import net.opengis.gml._3.AbstractRingPropertyType;
import net.opengis.gml._3.DirectPositionListType;
import net.opengis.gml._3.DirectPositionType;
import net.opengis.gml._3.LineStringType;
import net.opengis.gml._3.LinearRingType;
import net.opengis.gml._3.PolygonType;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.index.impl.NetexEntitiesIndexImpl;
import org.rutebanken.netex.model.*;

public class NetexTestData {

  private static final DateTimeFormatter DATE_FORMATTER =
    DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final DayType EVERYDAY = new DayType()
    .withId("EVERYDAY")
    .withName(new MultilingualString().withValue("everyday"));

  private final Line line;

  public NetexTestData() {
    this.line = new CreateLine().create();
  }

  public NetexTestData(Line line) {
    this.line = line;
  }

  public JAXBElement<LineRefStructure> getLineRef() {
    return createWrappedRef(this.line.getId(), LineRefStructure.class);
  }

  public CreateJourneyPattern journeyPattern() {
    return new CreateJourneyPattern();
  }

  public CreateServiceJourney serviceJourney(JourneyPattern journeyPattern) {
    return new CreateServiceJourney(journeyPattern);
  }

  public CreateTimetabledPassingTimes timetabledPassingTimes() {
    return new CreateTimetabledPassingTimes();
  }

  public CreateDeadRun deadRun(JourneyPattern journeyPattern) {
    return new CreateDeadRun(journeyPattern);
  }

  public CreateDatedServiceJourney datedServiceJourney() {
    return new CreateDatedServiceJourney();
  }

  public List<JourneyPattern> createJourneyPatterns(
    int numberOfJourneyPatterns
  ) {
    return IntStream
      .range(0, numberOfJourneyPatterns)
      .mapToObj(index -> new CreateJourneyPattern().withId(index).create())
      .toList();
  }

  public List<ServiceJourney> createServiceJourneys(
    JourneyPattern journeyPattern,
    int numberOfServiceJourneys
  ) {
    return IntStream
      .range(0, numberOfServiceJourneys)
      .mapToObj(index ->
        new CreateServiceJourney(journeyPattern).withId(index).create()
      )
      .toList();
  }

  public CreateServiceLink serviceLink(
    String fromScheduledStopPointRef,
    String toScheduledStopPointRef
  ) {
    return new CreateServiceLink()
      .withFromScheduledStopPointRef(fromScheduledStopPointRef)
      .withToScheduledStopPointRef(toScheduledStopPointRef);
  }

  public CreateFlexibleArea flexibleArea() {
    return new CreateFlexibleArea();
  }

  public CreateFlexibleStopPlace flexibleStopPlace(FlexibleArea flexibleArea) {
    return new CreateFlexibleStopPlace().withFlexibleArea(flexibleArea);
  }

  public CreateNetexEntitiesIndex netexEntitiesIndex() {
    return new CreateNetexEntitiesIndex();
  }

  public CreateNetexEntitiesIndex netexEntitiesIndex(
    JourneyPattern journeyPattern,
    Journey_VersionStructure journey
  ) {
    return new CreateNetexEntitiesIndex()
      .addJourneyPatterns(journeyPattern)
      .addServiceJourneys(journey);
  }

  public CreateNetexEntitiesIndex netexEntitiesIndex(ServiceLink serviceLink) {
    return new CreateNetexEntitiesIndex().addServiceLinks(serviceLink);
  }

  public CreateNetexEntitiesIndex netexEntitiesIndex(
    FlexibleStopPlace flexibleStopPlace
  ) {
    return new CreateNetexEntitiesIndex()
      .addFlexibleStopPlace(flexibleStopPlace);
  }

  public static class CreateFlexibleArea {

    private int id = 1;

    private List<Double> coordinates;

    public CreateFlexibleArea withId(int id) {
      this.id = id;
      return this;
    }

    public CreateFlexibleArea withCoordinates(List<Double> coordinates) {
      this.coordinates = coordinates;
      return this;
    }

    public FlexibleArea create() {
      LinearRingType linearRing = new LinearRingType();
      DirectPositionListType positionList = new DirectPositionListType()
        .withValue(coordinates);
      linearRing.withPosList(positionList);

      return new FlexibleArea()
        .withId("RUT:FlexibleArea:" + id)
        .withName(new MultilingualString().withValue("FlexibleArea " + id))
        .withPolygon(
          new PolygonType()
            .withExterior(
              new AbstractRingPropertyType()
                .withAbstractRing(
                  new net.opengis.gml._3.ObjectFactory()
                    .createLinearRing(linearRing)
                )
            )
        );
    }
  }

  public static class CreateFlexibleStopPlace {

    private int id = 1;

    private FlexibleArea flexibleArea;

    public CreateFlexibleStopPlace withId(int id) {
      this.id = id;
      return this;
    }

    public CreateFlexibleStopPlace withFlexibleArea(FlexibleArea flexibleArea) {
      this.flexibleArea = flexibleArea;
      return this;
    }

    public FlexibleStopPlace create() {
      return new FlexibleStopPlace()
        .withId("RUT:FlexibleStopPlace:" + id)
        .withName(new MultilingualString().withValue("FlexibleStopPlace " + id))
        .withAreas(
          new FlexibleStopPlace_VersionStructure.Areas()
            .withFlexibleAreaOrFlexibleAreaRefOrHailAndRideArea(flexibleArea)
        );
    }
  }

  public static class CreateDatedServiceJourney {

    private int id = 1;
    private LocalDate operatingDayDate;

    public CreateDatedServiceJourney withId(int id) {
      this.id = id;
      return this;
    }

    public CreateDatedServiceJourney withOperatingDayDate(
      LocalDate operatingDayDate
    ) {
      this.operatingDayDate = operatingDayDate;
      return this;
    }

    public DatedServiceJourney create() {
      OperatingDay operatingDay = new OperatingDay()
        .withId(operatingDayDate.format(DATE_FORMATTER))
        .withCalendarDate(operatingDayDate.atStartOfDay());

      return new DatedServiceJourney()
        .withId("RUT:DatedServiceJourney:" + id)
        .withServiceAlteration(ServiceAlterationEnumeration.PLANNED)
        .withOperatingDayRef(
          new OperatingDayRefStructure().withRef(operatingDay.getId())
        );
    }
  }

  public static class CreateLine {

    private int id = 1;
    private AllVehicleModesOfTransportEnumeration transportMode =
      AllVehicleModesOfTransportEnumeration.BUS;

    public CreateLine withId(int id) {
      this.id = id;
      return this;
    }

    public CreateLine withTransportMode(
      AllVehicleModesOfTransportEnumeration transportMode
    ) {
      this.transportMode = transportMode;
      return this;
    }

    private Line create() {
      return new Line()
        .withId("RUT:Line:" + id)
        .withName(new MultilingualString().withValue("Line " + id))
        .withTransportMode(transportMode);
    }
  }

  public static class CreateJourneyPattern {

    private int routeId = 1;
    private int id = 1;
    private int numberOfStopPointInJourneyPattern = 4;

    public CreateJourneyPattern withId(int id) {
      this.id = id;
      return this;
    }

    public CreateJourneyPattern withRouteId(int routeId) {
      this.routeId = routeId;
      return this;
    }

    public CreateJourneyPattern withNumberOfStopPointInJourneyPattern(
      int numberOfStopPointInJourneyPattern
    ) {
      this.numberOfStopPointInJourneyPattern =
        numberOfStopPointInJourneyPattern;
      return this;
    }

    public JourneyPattern create() {
      RouteRefStructure routeRef = new RouteRefStructure()
        .withRef("RUT:Route:" + routeId);

      List<PointInLinkSequence_VersionedChildStructure> pointsInLink =
        createPointsInLink(numberOfStopPointInJourneyPattern);
      return new JourneyPattern()
        .withId("RUT:JourneyPattern:" + id)
        .withRouteRef(routeRef)
        .withPointsInSequence(
          new PointsInJourneyPattern_RelStructure()
            .withPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern(
              pointsInLink
            )
        );
    }

    private List<PointInLinkSequence_VersionedChildStructure> createPointsInLink(
      int numberOfStopPointInJourneyPattern
    ) {
      String DESTINATION_DISPLAY_ID_1 = "NSR:DestinationDisplay:1";
      String DESTINATION_DISPLAY_ID_2 = "NSR:DestinationDisplay:2";

      DestinationDisplay destinationBergen = new DestinationDisplay()
        .withId(DESTINATION_DISPLAY_ID_1)
        .withVias(
          new Vias_RelStructure()
            .withVia(
              List.of(
                this.createViaDestinationDisplayRef(DESTINATION_DISPLAY_ID_2)
              )
            )
        )
        .withFrontText(new MultilingualString().withValue("Bergen"));

      DestinationDisplay destinationStavanger = new DestinationDisplay()
        .withId(DESTINATION_DISPLAY_ID_2)
        .withVias(
          new Vias_RelStructure()
            .withVia(
              List.of(
                this.createViaDestinationDisplayRef(DESTINATION_DISPLAY_ID_1)
              )
            )
        )
        .withFrontText(new MultilingualString().withValue("Stavanger"));

      return IntStream
        .range(0, numberOfStopPointInJourneyPattern)
        .mapToObj(index -> {
          String stopPointId =
            "RUT:StopPointInJourneyPattern:" + id + "_" + (index + 1);
          StopPointInJourneyPattern stopPoint = new StopPointInJourneyPattern()
            .withId(stopPointId)
            .withOrder(BigInteger.valueOf(index + 1))
            .withScheduledStopPointRef(
              createScheduledStopPointRef(
                "RUT:ScheduledStopPoint:" + (index + 1)
              )
            );

          if (index == 0) stopPoint.setDestinationDisplayRef(
            createDestinationDisplayRef(destinationBergen.getId()).getValue()
          );
          if (index == 2) stopPoint.setDestinationDisplayRef(
            createDestinationDisplayRef(destinationStavanger.getId()).getValue()
          );

          return stopPoint;
        })
        .map(PointInLinkSequence_VersionedChildStructure.class::cast)
        .toList();
    }

    private Via_VersionedChildStructure createViaDestinationDisplayRef(
      String destinationDisplayId
    ) {
      return new Via_VersionedChildStructure()
        .withDestinationDisplayRef(
          new DestinationDisplayRefStructure().withRef(destinationDisplayId)
        );
    }
  }

  public class CreateDeadRun {

    private int id = 1;

    private final JourneyPattern journeyPattern;

    private CreateTimetabledPassingTimes createTimetabledPassingTimes;

    public CreateDeadRun(JourneyPattern journeyPattern) {
      this.journeyPattern = journeyPattern;
      this.createTimetabledPassingTimes = new CreateTimetabledPassingTimes();
    }

    public CreateDeadRun withId(int id) {
      this.id = id;
      return this;
    }

    public CreateDeadRun withCreateTimetabledPassingTimes(
      CreateTimetabledPassingTimes createTimetabledPassingTimes
    ) {
      this.createTimetabledPassingTimes = createTimetabledPassingTimes;
      return this;
    }

    public DeadRun create() {
      return new DeadRun()
        .withId("RUT:DeadRun:" + id)
        .withLineRef(getLineRef())
        .withDayTypes(createEveryDayRefs())
        .withJourneyPatternRef(createJourneyPatternRef(journeyPattern.getId()))
        .withPassingTimes(
          new TimetabledPassingTimes_RelStructure()
            .withTimetabledPassingTime(
              createTimetabledPassingTimes.create(journeyPattern)
            )
        );
    }
  }

  public class CreateServiceJourney {

    private int id = 1;
    private final JourneyPattern journeyPattern;

    private CreateTimetabledPassingTimes createTimetabledPassingTimes;

    public CreateServiceJourney(JourneyPattern journeyPattern) {
      this.journeyPattern = journeyPattern;
      this.createTimetabledPassingTimes = new CreateTimetabledPassingTimes();
    }

    public CreateServiceJourney withId(int id) {
      this.id = id;
      return this;
    }

    public CreateServiceJourney withCreateTimetabledPassingTimes(
      CreateTimetabledPassingTimes createTimetabledPassingTimes
    ) {
      this.createTimetabledPassingTimes = createTimetabledPassingTimes;
      return this;
    }

    public ServiceJourney create() {
      return new ServiceJourney()
        .withId("RUT:ServiceJourney:" + id)
        .withLineRef(getLineRef())
        .withDayTypes(createEveryDayRefs())
        .withJourneyPatternRef(createJourneyPatternRef(journeyPattern.getId()))
        .withPassingTimes(
          new TimetabledPassingTimes_RelStructure()
            .withTimetabledPassingTime(
              createTimetabledPassingTimes.create(journeyPattern)
            )
        );
    }
  }

  public static class CreateTimetabledPassingTimes {

    private int departureTimeOffset = 5;

    public CreateTimetabledPassingTimes withDepartureTimeOffset(
      int departureTimeOffset
    ) {
      this.departureTimeOffset = departureTimeOffset;
      return this;
    }

    public List<TimetabledPassingTime> create(JourneyPattern journeyPattern) {
      return createTimetabledPassingTimes(
        journeyPattern
          .getPointsInSequence()
          .getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern()
      );
    }

    private List<TimetabledPassingTime> createTimetabledPassingTimes(
      List<PointInLinkSequence_VersionedChildStructure> pointsInLink
    ) {
      return IntStream
        .range(0, pointsInLink.size())
        .mapToObj(index ->
          new TimetabledPassingTime()
            .withId("TTPT-" + (index + 1))
            .withDepartureTime(LocalTime.of(5, index * departureTimeOffset))
            .withPointInJourneyPatternRef(
              createStopPointRef(pointsInLink.get(index).getId())
            )
        )
        .toList();
    }
  }

  public static class CreateServiceLink {

    private int id = 1;
    private ScheduledStopPointRefStructure fromScheduledStopPointRef;
    private ScheduledStopPointRefStructure toScheduledStopPointRef;
    private LinkSequenceProjection_VersionStructure linkSequenceProjectionVersionStructure;

    public CreateServiceLink withId(int id) {
      this.id = id;
      return this;
    }

    public CreateServiceLink withFromScheduledStopPointRef(
      String fromScheduledStopPointRef
    ) {
      this.fromScheduledStopPointRef =
        new ScheduledStopPointRefStructure().withRef(fromScheduledStopPointRef);
      return this;
    }

    public CreateServiceLink withToScheduledStopPointRef(
      String toScheduledStopPointRef
    ) {
      this.toScheduledStopPointRef =
        new ScheduledStopPointRefStructure().withRef(toScheduledStopPointRef);
      return this;
    }

    public CreateServiceLink withLineStringList(
      List<Double> lineStringPositions
    ) {
      this.linkSequenceProjectionVersionStructure =
        new LinkSequenceProjection_VersionStructure()
          .withId("ServiceLinkProjection:" + id)
          .withLineString(
            new LineStringType()
              .withPosList(
                new DirectPositionListType().withValue(lineStringPositions)
              )
          );
      return this;
    }

    public CreateServiceLink withLineStringPositions(
      List<DirectPositionType> lineStringPositions
    ) {
      this.linkSequenceProjectionVersionStructure =
        new LinkSequenceProjection_VersionStructure()
          .withId("ServiceLinkProjection:" + id)
          .withLineString(
            new LineStringType()
              .withPosOrPointProperty(
                lineStringPositions.toArray(Object[]::new)
              )
          );
      return this;
    }

    public ServiceLink create() {
      return new ServiceLink()
        .withId("RUT:ServiceLink:" + id)
        .withFromPointRef(fromScheduledStopPointRef)
        .withToPointRef(toScheduledStopPointRef)
        .withProjections(
          new Projections_RelStructure()
            .withProjectionRefOrProjection(
              createLinkSequenceProjection_VersionStructureElement(
                linkSequenceProjectionVersionStructure
              )
            )
        );
    }
  }

  public static class CreateNetexEntitiesIndex {

    private final List<JourneyPattern> journeyPatterns = new ArrayList<>();
    private final List<Journey_VersionStructure> journeys = new ArrayList<>();
    private final List<ServiceLink> serviceLinks = new ArrayList<>();
    private final List<FlexibleStopPlace> flexibleStopPlaces =
      new ArrayList<>();

    public CreateNetexEntitiesIndex addFlexibleStopPlace(
      FlexibleStopPlace... flexibleStopPlace
    ) {
      this.flexibleStopPlaces.addAll(Arrays.asList(flexibleStopPlace));
      return this;
    }

    public CreateNetexEntitiesIndex addServiceJourneys(
      Journey_VersionStructure... serviceJourney
    ) {
      this.journeys.addAll(Arrays.asList(serviceJourney));
      return this;
    }

    public CreateNetexEntitiesIndex addJourneyPatterns(
      JourneyPattern... journeyPatterns
    ) {
      this.journeyPatterns.addAll(Arrays.asList(journeyPatterns));
      return this;
    }

    public CreateNetexEntitiesIndex addServiceLinks(
      ServiceLink... serviceLinks
    ) {
      this.serviceLinks.addAll(Arrays.asList(serviceLinks));
      return this;
    }

    public NetexEntitiesIndex create() {
      NetexEntitiesIndex netexEntitiesIndex = new NetexEntitiesIndexImpl();
      journeyPatterns.forEach(journeyPattern ->
        netexEntitiesIndex
          .getJourneyPatternIndex()
          .put(journeyPattern.getId(), journeyPattern)
      );

      journeys.forEach(journey ->
        netexEntitiesIndex
          .getTimetableFrames()
          .add(
            new TimetableFrame()
              .withVehicleJourneys(
                new JourneysInFrame_RelStructure()
                  .withId("JR:123")
                  .withVehicleJourneyOrDatedVehicleJourneyOrNormalDatedVehicleJourney(
                    journey
                  )
              )
          )
      );

      serviceLinks.forEach(serviceLink ->
        netexEntitiesIndex
          .getServiceFrames()
          .add(
            new ServiceFrame()
              .withServiceLinks(
                new ServiceLinksInFrame_RelStructure()
                  .withServiceLink(serviceLink)
              )
          )
      );

      flexibleStopPlaces.forEach(flexibleStopPlace ->
        netexEntitiesIndex
          .getSiteFrames()
          .add(
            new SiteFrame()
              .withFlexibleStopPlaces(
                new FlexibleStopPlacesInFrame_RelStructure()
                  .withFlexibleStopPlace(flexibleStopPlace)
              )
          )
      );

      return netexEntitiesIndex;
    }
  }

  private static DayTypeRefs_RelStructure createEveryDayRefs() {
    return new DayTypeRefs_RelStructure()
      .withDayTypeRef(Collections.singleton(createEveryDayRef()));
  }

  /* private static utility methods */
  private static JAXBElement<LinkSequenceProjection_VersionStructure> createLinkSequenceProjection_VersionStructureElement(
    LinkSequenceProjection_VersionStructure value
  ) {
    return createJaxbElement(value);
  }

  private static JAXBElement<ScheduledStopPointRefStructure> createScheduledStopPointRef(
    String id
  ) {
    return createWrappedRef(id, ScheduledStopPointRefStructure.class);
  }

  private static JAXBElement<StopPointInJourneyPatternRefStructure> createStopPointRef(
    String id
  ) {
    return createWrappedRef(id, StopPointInJourneyPatternRefStructure.class);
  }

  private static JAXBElement<JourneyPatternRefStructure> createJourneyPatternRef(
    String id
  ) {
    return createWrappedRef(id, JourneyPatternRefStructure.class);
  }

  private static JAXBElement<DestinationDisplayRefStructure> createDestinationDisplayRef(
    String id
  ) {
    return createWrappedRef(id, DestinationDisplayRefStructure.class);
  }

  private static JAXBElement<DayTypeRefStructure> createEveryDayRef() {
    return createJaxbElement(
      new DayTypeRefStructure().withRef(EVERYDAY.getId())
    );
  }
}
