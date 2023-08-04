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
import net.opengis.gml._3.DirectPositionListType;
import net.opengis.gml._3.DirectPositionType;
import net.opengis.gml._3.LineStringType;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.index.impl.NetexEntitiesIndexImpl;
import org.rutebanken.netex.model.*;

public class NetexTestFragment {

  private static final DateTimeFormatter DATE_FORMATTER =
    DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final DayType EVERYDAY = new DayType()
    .withId("EVERYDAY")
    .withName(new MultilingualString().withValue("everyday"));

  private final Line line;

  public NetexTestFragment() {
    this.line = new CreateLine().create();
  }

  public NetexTestFragment(Line line) {
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

  public CreateStopPointInJourneyPattern stopPointInJourneyPattern(
    int journeyPatternId
  ) {
    return new CreateStopPointInJourneyPattern(journeyPatternId);
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
        .withId("TST:Line:" + id)
        .withName(new MultilingualString().withValue("Line " + id))
        .withTransportMode(transportMode);
    }
  }

  public static class CreateJourneyPattern {

    private int routeId = 1;
    private int id = 1;
    private int numberOfStopPointInJourneyPattern = 4;
    List<PointInLinkSequence_VersionedChildStructure> pointsInLink =
      new ArrayList<>();

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

    public CreateJourneyPattern withStopPointsInJourneyPattern(
      List<StopPointInJourneyPattern> stopPointsInJourneyPattern
    ) {
      this.pointsInLink =
        stopPointsInJourneyPattern
          .stream()
          .map(PointInLinkSequence_VersionedChildStructure.class::cast)
          .toList();
      return this;
    }

    public JourneyPattern create() {
      RouteRefStructure routeRef = new RouteRefStructure()
        .withRef("TST:Route:" + routeId);

      return new JourneyPattern()
        .withId("TST:JourneyPattern:" + id)
        .withRouteRef(routeRef)
        .withPointsInSequence(
          new PointsInJourneyPattern_RelStructure()
            .withPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern(
              this.pointsInLink.isEmpty()
                ? createPointsInLink(numberOfStopPointInJourneyPattern)
                : this.pointsInLink
            )
        );
    }

    private List<PointInLinkSequence_VersionedChildStructure> createPointsInLink(
      int numberOfStopPointInJourneyPattern
    ) {
      return IntStream
        .range(0, numberOfStopPointInJourneyPattern)
        .mapToObj(index -> {
          CreateStopPointInJourneyPattern createStopPointInJourneyPattern =
            new CreateStopPointInJourneyPattern(id)
              .withId(index + 1)
              .withOrder(index + 1)
              .withScheduledStopPointRef(index + 1);

          // Setting destination display id for first and last stop point
          if (index == 0 || index == numberOfStopPointInJourneyPattern - 1) {
            createStopPointInJourneyPattern.withDestinationDisplayId(index + 1);
          }

          return createStopPointInJourneyPattern.create();
        })
        .map(PointInLinkSequence_VersionedChildStructure.class::cast)
        .toList();
    }
  }

  public static class CreateStopPointInJourneyPattern {

    private final int journeyPatternId;
    private int id = 1;
    private int order = 1;
    private int scheduledStopPointId;
    private int destinationDisplayId = -1;

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

    public CreateStopPointInJourneyPattern withScheduledStopPointRef(
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
      return stopPointInJourneyPattern;
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
        .withId("TST:DeadRun:" + id)
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
        .withId("TST:ServiceJourney:" + id)
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

  public static class CreateNetexEntitiesIndex {

    private final List<JourneyPattern> journeyPatterns = new ArrayList<>();
    private final List<Journey_VersionStructure> journeys = new ArrayList<>();
    private final List<ServiceLink> serviceLinks = new ArrayList<>();
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

      passengerStopAssignments.forEach(passengerStopAssignment -> {
        netexEntitiesIndex
          .getPassengerStopAssignmentsByStopPointRefIndex()
          .put(
            passengerStopAssignment
              .getScheduledStopPointRef()
              .getValue()
              .getRef(),
            passengerStopAssignment
          );
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
