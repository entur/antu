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
import java.util.function.BinaryOperator;
import java.util.stream.Collector;
import java.util.stream.IntStream;
import net.opengis.gml._3.AbstractRingPropertyType;
import net.opengis.gml._3.DirectPositionListType;
import net.opengis.gml._3.DirectPositionType;
import net.opengis.gml._3.LineStringType;
import net.opengis.gml._3.LinearRingType;
import net.opengis.gml._3.PolygonType;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.index.impl.NetexEntitiesIndexImpl;
import org.jetbrains.annotations.NotNull;
import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;
import org.rutebanken.netex.model.DatedServiceJourney;
import org.rutebanken.netex.model.DayType;
import org.rutebanken.netex.model.DayTypeRefStructure;
import org.rutebanken.netex.model.DayTypeRefs_RelStructure;
import org.rutebanken.netex.model.DeadRun;
import org.rutebanken.netex.model.DestinationDisplayRefStructure;
import org.rutebanken.netex.model.EntityStructure;
import org.rutebanken.netex.model.FlexibleArea;
import org.rutebanken.netex.model.FlexibleStopPlace;
import org.rutebanken.netex.model.FlexibleStopPlace_VersionStructure;
import org.rutebanken.netex.model.FlexibleStopPlacesInFrame_RelStructure;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.JourneyPatternRefStructure;
import org.rutebanken.netex.model.Journey_VersionStructure;
import org.rutebanken.netex.model.JourneysInFrame_RelStructure;
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.LineRefStructure;
import org.rutebanken.netex.model.LinkInJourneyPattern;
import org.rutebanken.netex.model.LinkInLinkSequence_VersionedChildStructure;
import org.rutebanken.netex.model.LinkSequenceProjection_VersionStructure;
import org.rutebanken.netex.model.LinksInJourneyPattern_RelStructure;
import org.rutebanken.netex.model.MultilingualString;
import org.rutebanken.netex.model.OperatingDay;
import org.rutebanken.netex.model.OperatingDayRefStructure;
import org.rutebanken.netex.model.PassengerStopAssignment;
import org.rutebanken.netex.model.PointInLinkSequence_VersionedChildStructure;
import org.rutebanken.netex.model.PointsInJourneyPattern_RelStructure;
import org.rutebanken.netex.model.Projections_RelStructure;
import org.rutebanken.netex.model.QuayRefStructure;
import org.rutebanken.netex.model.Route;
import org.rutebanken.netex.model.RouteRefStructure;
import org.rutebanken.netex.model.ScheduledStopPointRefStructure;
import org.rutebanken.netex.model.ServiceAlterationEnumeration;
import org.rutebanken.netex.model.ServiceFrame;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.ServiceLink;
import org.rutebanken.netex.model.ServiceLinkRefStructure;
import org.rutebanken.netex.model.ServiceLinksInFrame_RelStructure;
import org.rutebanken.netex.model.SiteFrame;
import org.rutebanken.netex.model.StopPlaceRefStructure;
import org.rutebanken.netex.model.StopPointInJourneyPattern;
import org.rutebanken.netex.model.StopPointInJourneyPatternRefStructure;
import org.rutebanken.netex.model.TimetableFrame;
import org.rutebanken.netex.model.TimetabledPassingTime;
import org.rutebanken.netex.model.TimetabledPassingTimes_RelStructure;

public class NetexTestFragment {

  private static final DateTimeFormatter DATE_FORMATTER =
    DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final DayType EVERYDAY = new DayType()
    .withId("EVERYDAY")
    .withName(new MultilingualString().withValue("everyday"));

  public CreateLine line() {
    return new CreateLine();
  }

  public CreateRoute route() {
    return new CreateRoute();
  }

  public CreateJourneyPattern journeyPattern() {
    return new CreateJourneyPattern();
  }

  public CreateServiceJourney serviceJourney(
    Line line,
    JourneyPattern journeyPattern
  ) {
    return new CreateServiceJourney(line, journeyPattern);
  }

  public CreateServiceJourney serviceJourney(JourneyPattern journeyPattern) {
    return serviceJourney(new CreateLine().create(), journeyPattern);
  }

  public CreateTimetabledPassingTimes timetabledPassingTimes() {
    return new CreateTimetabledPassingTimes();
  }

  public CreateDeadRun deadRun(Line line, JourneyPattern journeyPattern) {
    return new CreateDeadRun(line, journeyPattern);
  }

  public CreateDeadRun deadRun(JourneyPattern journeyPattern) {
    return deadRun(new CreateLine().create(), journeyPattern);
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
    Line line,
    JourneyPattern journeyPattern,
    int numberOfServiceJourneys
  ) {
    return IntStream
      .range(0, numberOfServiceJourneys)
      .mapToObj(index ->
        new CreateServiceJourney(line, journeyPattern).withId(index).create()
      )
      .toList();
  }

  public List<ServiceJourney> createServiceJourneys(
    JourneyPattern journeyPattern,
    int numberOfServiceJourneys
  ) {
    return createServiceJourneys(
      new CreateLine().create(),
      journeyPattern,
      numberOfServiceJourneys
    );
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

  public CreateStopPointInJourneyPattern stopPointInJourneyPattern(
    int journeyPatternId
  ) {
    return new CreateStopPointInJourneyPattern(journeyPatternId);
  }

  public CreateLinkInJourneyPattern linkInJourneyPattern(int journeyPatternId) {
    return new CreateLinkInJourneyPattern(journeyPatternId);
  }

  public CreatePassengerStopAssignment passengerStopAssignment() {
    return new CreatePassengerStopAssignment();
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

  public static class CreatePassengerStopAssignment {

    private int id;

    private String scheduleStopPointRef;

    private String stopPlaceRef;

    private String quayRef;

    public CreatePassengerStopAssignment withId(int id) {
      this.id = id;
      return this;
    }

    public CreatePassengerStopAssignment withScheduleStopPointId(
      int scheduleStopPointId
    ) {
      this.scheduleStopPointRef =
        "TST:ScheduledStopPoint:" + scheduleStopPointId;
      return this;
    }

    public CreatePassengerStopAssignment withStopPlaceId(int stopPlaceId) {
      this.stopPlaceRef = "TST:StopPlace:" + stopPlaceId;
      return this;
    }

    public CreatePassengerStopAssignment withQuayId(int quayId) {
      this.quayRef = "TST:Quay:" + quayId;
      return this;
    }

    public PassengerStopAssignment create() {
      return new PassengerStopAssignment()
        .withId("TST:PassengerStopAssignment:" + id)
        .withScheduledStopPointRef(
          createWrappedRef(
            scheduleStopPointRef,
            ScheduledStopPointRefStructure.class
          )
        )
        .withQuayRef(createWrappedRef(quayRef, QuayRefStructure.class))
        .withStopPlaceRef(
          createWrappedRef(stopPlaceRef, StopPlaceRefStructure.class)
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
        .withId("TST:DatedServiceJourney:" + id)
        .withServiceAlteration(ServiceAlterationEnumeration.PLANNED)
        .withOperatingDayRef(
          new OperatingDayRefStructure().withRef(operatingDay.getId())
        );
    }
  }

  public static class CreateLine {

    private int id = 1;
    private AllVehicleModesOfTransportEnumeration transportMode;

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

    public Line create() {
      return new Line()
        .withId("TST:Line:" + id)
        .withName(new MultilingualString().withValue("Line " + id))
        .withTransportMode(transportMode);
    }
  }

  public static class CreateRoute {

    private int id = 1;
    private Line line;

    public CreateRoute withId(int id) {
      this.id = id;
      return this;
    }

    public CreateRoute withLine(Line line) {
      this.line = line;
      return this;
    }

    public Route create() {
      return new Route()
        .withLineRef(
          createJaxbElement(new LineRefStructure().withRef(line.getId()))
        )
        .withId("TST:Route:" + id);
    }
  }

  public static class CreateJourneyPattern {

    private Route route;
    private int id = 1;
    private int numberOfStopPointInJourneyPattern = 4;

    private int numberOfServiceLinksInJourneyPattern = 3;

    List<PointInLinkSequence_VersionedChildStructure> pointsInSequence =
      new ArrayList<>();

    List<LinkInLinkSequence_VersionedChildStructure> linkInSequence =
      new ArrayList<>();

    public CreateJourneyPattern withId(int id) {
      this.id = id;
      return this;
    }

    public CreateJourneyPattern withRoute(Route route) {
      this.route = route;
      return this;
    }

    public CreateJourneyPattern withNumberOfStopPointInJourneyPattern(
      int numberOfStopPointInJourneyPattern
    ) {
      this.numberOfStopPointInJourneyPattern =
        numberOfStopPointInJourneyPattern;
      return this;
    }

    public CreateJourneyPattern withNumberOfServiceLinksInJourneyPattern(
      int numberOfServiceLinksInJourneyPattern
    ) {
      this.numberOfServiceLinksInJourneyPattern =
        numberOfServiceLinksInJourneyPattern;
      return this;
    }

    public CreateJourneyPattern withStopPointsInJourneyPattern(
      List<StopPointInJourneyPattern> stopPointsInJourneyPattern
    ) {
      this.pointsInSequence =
        stopPointsInJourneyPattern
          .stream()
          .map(PointInLinkSequence_VersionedChildStructure.class::cast)
          .toList();
      return this;
    }

    public CreateJourneyPattern withServiceLinksInJourneyPattern(
      List<LinkInJourneyPattern> linksInJourneyPattern
    ) {
      this.linkInSequence =
        linksInJourneyPattern
          .stream()
          .map(LinkInLinkSequence_VersionedChildStructure.class::cast)
          .toList();
      return this;
    }

    public JourneyPattern create() {
      JourneyPattern journeyPattern = new JourneyPattern()
        .withId("TST:JourneyPattern:" + id);

      if (route != null) {
        journeyPattern.withRouteRef(
          new RouteRefStructure().withRef(route.getId())
        );
      }

      if (
        numberOfStopPointInJourneyPattern > 0 || !pointsInSequence.isEmpty()
      ) {
        journeyPattern.withPointsInSequence(
          new PointsInJourneyPattern_RelStructure()
            .withPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern(
              this.pointsInSequence.isEmpty()
                ? createPointsInLinkSequence(numberOfStopPointInJourneyPattern)
                : this.pointsInSequence
            )
        );
      }

      if (
        numberOfServiceLinksInJourneyPattern > 0 || !linkInSequence.isEmpty()
      ) {
        journeyPattern.withLinksInSequence(
          new LinksInJourneyPattern_RelStructure()
            .withServiceLinkInJourneyPatternOrTimingLinkInJourneyPattern(
              this.linkInSequence.isEmpty()
                ? createLinksInLinkSequence(
                  numberOfServiceLinksInJourneyPattern
                )
                : this.linkInSequence
            )
        );
      }

      return journeyPattern;
    }

    private List<PointInLinkSequence_VersionedChildStructure> createPointsInLinkSequence(
      int numberOfStopPointInJourneyPattern
    ) {
      return IntStream
        .range(0, numberOfStopPointInJourneyPattern)
        .mapToObj(index -> {
          CreateStopPointInJourneyPattern createStopPointInJourneyPattern =
            new CreateStopPointInJourneyPattern(id)
              .withId(index + 1)
              .withOrder(index + 1)
              .withScheduledStopPointId(index + 1);

          // Setting destination display id for first and last stop point
          if (index == 0 || index == numberOfStopPointInJourneyPattern - 1) {
            createStopPointInJourneyPattern.withDestinationDisplayId(index + 1);
          }

          return createStopPointInJourneyPattern.create();
        })
        .map(PointInLinkSequence_VersionedChildStructure.class::cast)
        .toList();
    }

    private List<LinkInLinkSequence_VersionedChildStructure> createLinksInLinkSequence(
      int numberOfStopPointInJourneyPattern
    ) {
      return IntStream
        .range(0, numberOfStopPointInJourneyPattern)
        .mapToObj(index -> {
          CreateLinkInJourneyPattern createLinkInJourneyPattern =
            new CreateLinkInJourneyPattern(id)
              .withId(index + 1)
              .withOrder(index + 1)
              .withServiceLinkId(index + 1);

          return createLinkInJourneyPattern.create();
        })
        .map(LinkInLinkSequence_VersionedChildStructure.class::cast)
        .toList();
    }
  }

  public static class CreateStopPointInJourneyPattern {

    private final int journeyPatternId;
    private int id = 1;
    private int order = 1;
    private int scheduledStopPointId = 1;
    private int destinationDisplayId = -1;
    private boolean forAlighting = false;
    private boolean forBoarding = false;

    public CreateStopPointInJourneyPattern(
      CreateJourneyPattern journeyPattern
    ) {
      this.journeyPatternId = journeyPattern.id;
    }

    public CreateStopPointInJourneyPattern(int journeyPatternId) {
      this.journeyPatternId = journeyPatternId;
    }

    public CreateStopPointInJourneyPattern withId(int id) {
      this.id = id;
      return this;
    }

    public CreateStopPointInJourneyPattern withOrder(int order) {
      this.order = order;
      return this;
    }

    public CreateStopPointInJourneyPattern withScheduledStopPointId(
      int scheduledStopPointId
    ) {
      this.scheduledStopPointId = scheduledStopPointId;
      return this;
    }

    public CreateStopPointInJourneyPattern withDestinationDisplayId(
      int destinationDisplayId
    ) {
      this.destinationDisplayId = destinationDisplayId;
      return this;
    }

    public CreateStopPointInJourneyPattern withForAlighting(
      boolean forAlighting
    ) {
      this.forAlighting = forAlighting;
      return this;
    }

    public CreateStopPointInJourneyPattern withForBoarding(
      boolean forBoarding
    ) {
      this.forBoarding = forBoarding;
      return this;
    }

    public StopPointInJourneyPattern create() {
      StopPointInJourneyPattern stopPointInJourneyPattern =
        new StopPointInJourneyPattern()
          .withId(
            "TST:StopPointInJourneyPattern:" + journeyPatternId + "_" + id
          )
          .withOrder(BigInteger.valueOf(order))
          .withScheduledStopPointRef(
            createScheduledStopPointRef(
              "TST:ScheduledStopPoint:" + scheduledStopPointId
            )
          );
      if (destinationDisplayId > 0) {
        stopPointInJourneyPattern.setDestinationDisplayRef(
          createDestinationDisplayRef(
            "TST:DestinationDisplay:" + destinationDisplayId
          )
            .getValue()
        );
      }

      stopPointInJourneyPattern.withForAlighting(forAlighting);
      stopPointInJourneyPattern.withForBoarding(forBoarding);

      return stopPointInJourneyPattern;
    }
  }

  public static class CreateLinkInJourneyPattern {

    private final int journeyPatternId;
    private int id = 1;
    private int order = 1;
    private int serviceLinkId;

    public CreateLinkInJourneyPattern(int journeyPatternId) {
      this.journeyPatternId = journeyPatternId;
    }

    public CreateLinkInJourneyPattern withId(int id) {
      this.id = id;
      return this;
    }

    public CreateLinkInJourneyPattern withOrder(int order) {
      this.order = order;
      return this;
    }

    public CreateLinkInJourneyPattern withServiceLinkId(int serviceLinkId) {
      this.serviceLinkId = serviceLinkId;
      return this;
    }

    public LinkInJourneyPattern create() {
      return new LinkInJourneyPattern()
        .withId("TST:LinkInJourneyPattern:" + journeyPatternId + "_" + id)
        .withOrder(BigInteger.valueOf(order))
        .withServiceLinkRef(
          new ServiceLinkRefStructure()
            .withRef("TST:ServiceLink:" + serviceLinkId)
        );
    }
  }

  public static class CreateDeadRun {

    private int id = 1;
    private final Line line;
    private final JourneyPattern journeyPattern;

    private CreateTimetabledPassingTimes createTimetabledPassingTimes;

    public CreateDeadRun(Line line, JourneyPattern journeyPattern) {
      this.line = line;
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
        .withId("TST:DeadRun:" + id)
        .withLineRef(createLineRef(line.getId()))
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

  public static class CreateServiceJourney {

    private int id = 1;
    private final Line line;
    private final JourneyPattern journeyPattern;
    private CreateTimetabledPassingTimes createTimetabledPassingTimes;

    public CreateServiceJourney(Line line, JourneyPattern journeyPattern) {
      this.line = line;
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
        .withId("TST:ServiceJourney:" + id)
        .withLineRef(createLineRef(line.getId()))
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
          .withId("TST:ServiceLinkProjection:" + id)
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
          .withId("TST:ServiceLinkProjection:" + id)
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
        .withId("TST:ServiceLink:" + id)
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

  public class CreateNetexEntitiesIndex {

    private Line line;
    private Route route;
    private final List<JourneyPattern> journeyPatterns = new ArrayList<>();
    private final List<Journey_VersionStructure> journeys = new ArrayList<>();
    private final List<ServiceLink> serviceLinks = new ArrayList<>();
    private final List<FlexibleStopPlace> flexibleStopPlaces =
      new ArrayList<>();

    public CreateNetexEntitiesIndex addLine(Line line) {
      this.line = line;
      return this;
    }

    public CreateNetexEntitiesIndex addRoute(Route route) {
      this.route = route;
      return this;
    }

    public CreateNetexEntitiesIndex addFlexibleStopPlace(
      FlexibleStopPlace... flexibleStopPlace
    ) {
      this.flexibleStopPlaces.addAll(Arrays.asList(flexibleStopPlace));
      return this;
    }

    private final List<PassengerStopAssignment> passengerStopAssignments =
      new ArrayList<>();

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

    public CreateNetexEntitiesIndex addPassengerStopAssignments(
      PassengerStopAssignment... passengerStopAssignments
    ) {
      this.passengerStopAssignments.addAll(
          Arrays.asList(passengerStopAssignments)
        );
      return this;
    }

    public CreateNetexEntitiesIndex addPassengerStopAssignment(
      PassengerStopAssignment passengerStopAssignment
    ) {
      this.passengerStopAssignments.add(passengerStopAssignment);
      return this;
    }

    public NetexEntitiesIndex create() {
      NetexEntitiesIndex netexEntitiesIndex = new NetexEntitiesIndexImpl();

      if (line != null) {
        netexEntitiesIndex.getLineIndex().put(line.getId(), line);
      }

      if (route != null) {
        netexEntitiesIndex.getRouteIndex().put(route.getId(), route);
      }

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

      fillIndexes(netexEntitiesIndex);
      return netexEntitiesIndex;
    }

    private void fillIndexes(NetexEntitiesIndex netexEntitiesIndex) {
      passengerStopAssignments.forEach(passengerStopAssignment -> {
        // PassengerStopAssignmentsByStopPointRefIndex
        netexEntitiesIndex
          .getPassengerStopAssignmentsByStopPointRefIndex()
          .put(
            passengerStopAssignment
              .getScheduledStopPointRef()
              .getValue()
              .getRef(),
            passengerStopAssignment
          );

        // QuayIdByStopPointRefIndex
        netexEntitiesIndex
          .getQuayIdByStopPointRefIndex()
          .put(
            passengerStopAssignment
              .getScheduledStopPointRef()
              .getValue()
              .getRef(),
            passengerStopAssignment.getQuayRef().getValue().getRef()
          );
      });
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

  private static JAXBElement<LineRefStructure> createLineRef(String id) {
    return createWrappedRef(id, LineRefStructure.class);
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
