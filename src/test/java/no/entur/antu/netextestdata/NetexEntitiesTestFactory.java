package no.entur.antu.netextestdata;

import static org.entur.netex.validation.test.jaxb.support.JAXBUtils.createJaxbElement;

import jakarta.xml.bind.JAXBElement;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.opengis.gml._3.AbstractRingPropertyType;
import net.opengis.gml._3.DirectPositionListType;
import net.opengis.gml._3.DirectPositionType;
import net.opengis.gml._3.LineStringType;
import net.opengis.gml._3.LinearRingType;
import net.opengis.gml._3.ObjectFactory;
import net.opengis.gml._3.PolygonType;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.index.impl.NetexEntitiesIndexImpl;
import org.entur.netex.validation.test.jaxb.support.JAXBUtils;
import org.rutebanken.netex.model.*;

public class NetexEntitiesTestFactory {

  private CreateGenericLine<? extends Line_VersionStructure> line;

  private CreateRoute route;

  private final List<CreateJourneyPattern> journeyPatterns = new ArrayList<>();
  private final List<CreateServiceJourney> serviceJourneys = new ArrayList<>();
  private final List<CreateDatedServiceJourney> datedServiceJourneys =
    new ArrayList<>();

  private final List<CreateServiceJourneyInterchange> interchanges =
    new ArrayList<>();
  private final List<CreateServiceLink> serviceLinks = new ArrayList<>();
  private final List<CreateFlexibleStopPlace> flexibleStopPlaces =
    new ArrayList<>();
  private final List<CreateDeadRun> deadRuns = new ArrayList<>();
  private final List<CreatePassengerStopAssignment> passengerStopAssignments =
    new ArrayList<>();

  private CreateCompositeFrame compositeFrame;
  private List<CreateServiceCalendarFrame> serviceCalendarFrames;

  public NetexEntitiesIndex create() {
    NetexEntitiesIndex netexEntitiesIndex = new NetexEntitiesIndexImpl();

    if (line != null && line instanceof CreateLine createLine) {
      netexEntitiesIndex.getLineIndex().put(line.ref(), createLine.create());
    }

    if (line != null && line instanceof CreateFlexibleLine createFlexibleLine) {
      netexEntitiesIndex
        .getFlexibleLineIndex()
        .put(line.ref(), createFlexibleLine.create());
    }

    if (route != null) {
      netexEntitiesIndex.getRouteIndex().put(route.ref(), route.create());
    }

    if (compositeFrame != null) {
      netexEntitiesIndex.getCompositeFrames().add(compositeFrame.create());
    }

    if (serviceCalendarFrames != null) {
      serviceCalendarFrames.forEach(serviceCalendarFrame ->
        netexEntitiesIndex
          .getServiceCalendarFrames()
          .add(serviceCalendarFrame.create())
      );
    }

    fillIndexes(netexEntitiesIndex);
    return netexEntitiesIndex;
  }

  private void fillIndexes(NetexEntitiesIndex netexEntitiesIndex) {
    passengerStopAssignments
      .stream()
      .map(CreatePassengerStopAssignment::create)
      .forEach(passengerStopAssignment -> {
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

    journeyPatterns
      .stream()
      .map(CreateJourneyPattern::create)
      .forEach(journeyPattern ->
        netexEntitiesIndex
          .getJourneyPatternIndex()
          .put(journeyPattern.getId(), journeyPattern)
      );

    interchanges
      .stream()
      .map(CreateServiceJourneyInterchange::create)
      .forEach(interchange ->
        netexEntitiesIndex
          .getServiceJourneyInterchangeIndex()
          .put(interchange.getId(), interchange)
      );

    serviceJourneys
      .stream()
      .map(CreateServiceJourney::create)
      .forEach(journey ->
        netexEntitiesIndex
          .getServiceJourneyIndex()
          .put(journey.getId(), journey)
      );

    datedServiceJourneys
      .stream()
      .map(CreateDatedServiceJourney::create)
      .forEach(journey ->
        netexEntitiesIndex
          .getDatedServiceJourneyIndex()
          .put(journey.getId(), journey)
      );

    deadRuns
      .stream()
      .map(CreateDeadRun::create)
      .forEach(deadRun ->
        netexEntitiesIndex.getDeadRunIndex().put(deadRun.getId(), deadRun)
      );

    serviceLinks
      .stream()
      .map(CreateServiceLink::create)
      .forEach(serviceLink ->
        netexEntitiesIndex
          .getServiceLinkIndex()
          .put(serviceLink.getId(), serviceLink)
      );
    flexibleStopPlaces
      .stream()
      .map(CreateFlexibleStopPlace::create)
      .forEach(flexibleStopPlace ->
        netexEntitiesIndex
          .getFlexibleStopPlaceIndex()
          .put(flexibleStopPlace.getId(), flexibleStopPlace)
      );
  }

  /**
   * Create a line with the given id
   * The existing line will be overwritten.
   *
   * @param id the id of the line
   * @return CreateLine
   */
  public CreateLine createLine(int id) {
    line = new CreateLine(id);
    return (CreateLine) line;
  }

  /**
   * Create a line with id 1.
   * The existing line will be overwritten.
   *
   * @return CreateLine
   */
  public CreateLine createLine() {
    return createLine(1);
  }

  /**
   * Create a flexible line with the given id
   * The existing line will be overwritten.
   *
   * @param id the id of the line
   * @return CreateFlexibleLine
   */
  public CreateFlexibleLine createFlexibleLine(int id) {
    line = new CreateFlexibleLine(id);
    return (CreateFlexibleLine) line;
  }

  /**
   * Create a flexible line with id 1.
   * The existing line will be overwritten.
   *
   * @return CreateFlexibleLine
   */
  public CreateFlexibleLine createFlexibleLine() {
    return createFlexibleLine(1);
  }

  /**
   * Create a route with the given id
   * The existing route will be overwritten.
   *
   * @param id the id of the route
   * @return CreateRoute
   */
  public CreateRoute createRoute(int id) {
    if (line == null) {
      line = new CreateLine(1);
    }

    route = new CreateRoute(id, line);
    return route;
  }

  /**
   * Create a route with id 1.
   * The existing route will be overwritten.
   *
   * @return CreateRoute
   */
  public CreateRoute createRoute() {
    return createRoute(1);
  }

  /**
   * Adds a new journey pattern with the given id
   *
   * @param id the id of the journey pattern
   * @return CreateJourneyPattern
   */
  public CreateJourneyPattern createJourneyPattern(int id) {
    CreateJourneyPattern createJourneyPattern = new CreateJourneyPattern(id);
    journeyPatterns.add(createJourneyPattern);
    return createJourneyPattern;
  }

  /**
   * Adds a new journey pattern with id 1
   *
   * @return CreateJourneyPattern
   */
  public CreateJourneyPattern createJourneyPattern() {
    return createJourneyPattern(1);
  }

  /**
   * Adds a new service journey with the given id and the given journey pattern
   * The line will be created if it does not exist, with id 1
   *
   * @param id the id of the journey pattern
   * @param journeyPattern the journey pattern ref for the service journey
   * @return CreateServiceJourney
   */
  public CreateServiceJourney createServiceJourney(
    int id,
    CreateJourneyPattern journeyPattern
  ) {
    if (line == null) {
      line = new CreateLine(1);
    }

    CreateServiceJourney createServiceJourney = new CreateServiceJourney(
      id,
      line,
      journeyPattern
    );
    serviceJourneys.add(createServiceJourney);
    return createServiceJourney;
  }

  /**
   * Adds a new service journey with id 1 and the given journey pattern
   * The line will be created if it does not exist, with id 1
   *
   * @param journeyPattern the journey pattern ref for the service journey
   * @return CreateServiceJourney
   */
  public CreateServiceJourney createServiceJourney(
    CreateJourneyPattern journeyPattern
  ) {
    return createServiceJourney(1, journeyPattern);
  }

  /**
   * Adds numberOfServiceJourneys new service journeys with the given journey pattern.
   * The line will be created if it does not exist, with id 1
   * The service journeys will have ids from 1 to numberOfServiceJourneys
   *
   * @param createJourneyPattern the journey pattern ref for the service journeys
   * @param numberOfServiceJourneys the number of service journeys to create
   * @return List of CreateServiceJourney created
   */
  public List<CreateServiceJourney> createServiceJourneys(
    CreateJourneyPattern createJourneyPattern,
    int numberOfServiceJourneys
  ) {
    if (line == null) {
      line = new CreateLine(1);
    }
    return IntStream
      .rangeClosed(1, numberOfServiceJourneys)
      .mapToObj(index -> createServiceJourney(index, createJourneyPattern))
      .toList();
  }

  /**
   * Adds a new dead run with the given id and the given journey pattern
   * The line will be created if it does not exist, with id 1
   *
   * @param id the id of the journey pattern
   * @param journeyPattern the journey pattern ref for the dead run
   * @return CreateDeadRun
   */
  public CreateDeadRun createDeadRun(
    int id,
    CreateJourneyPattern journeyPattern
  ) {
    if (line == null) {
      line = new CreateLine(1);
    }
    CreateDeadRun deadRun = new CreateDeadRun(id, line, journeyPattern);
    deadRuns.add(deadRun);
    return deadRun;
  }

  /**
   * Adds a new dead run with id 1 and the given journey pattern
   * The line will be created if it does not exist, with id 1
   *
   * @param journeyPattern the journey pattern ref for the dead run
   * @return CreateDeadRun
   */
  public CreateDeadRun createDeadRun(CreateJourneyPattern journeyPattern) {
    return createDeadRun(1, journeyPattern);
  }

  /**
   * Adds a new dated service journey with the given id, service journey and operating day
   *
   * @param id the id of the dated service journey
   * @param serviceJourneyRef the service journey ref for the dated service journey
   * @param operatingDayRef the operating day ref for the dated service journey
   * @return CreateDatedServiceJourney
   */
  public CreateDatedServiceJourney createDatedServiceJourney(
    int id,
    CreateServiceJourney serviceJourneyRef,
    CreateOperatingDay operatingDayRef
  ) {
    CreateDatedServiceJourney createDatedServiceJourney =
      new CreateDatedServiceJourney(id, serviceJourneyRef, operatingDayRef);
    datedServiceJourneys.add(createDatedServiceJourney);
    return createDatedServiceJourney;
  }

  /**
   * Adds a new dated service journey with id 1, service journey and operating day
   *
   * @param serviceJourneyRef the service journey ref for the dated service journey
   * @param operatingDayRef the operating day ref for the dated service journey
   * @return CreateDatedServiceJourney
   */
  public CreateDatedServiceJourney createDatedServiceJourney(
    CreateServiceJourney serviceJourneyRef,
    CreateOperatingDay operatingDayRef
  ) {
    return createDatedServiceJourney(1, serviceJourneyRef, operatingDayRef);
  }

  /**
   * Adds a new service journey interchange with the given id
   *
   * @param id the id of the service journey interchange
   * @return CreateServiceJourneyInterchange
   */
  public CreateServiceJourneyInterchange createServiceJourneyInterchange(
    int id
  ) {
    CreateServiceJourneyInterchange createServiceJourneyInterchange =
      new CreateServiceJourneyInterchange(id);
    interchanges.add(createServiceJourneyInterchange);
    return createServiceJourneyInterchange;
  }

  /**
   * Adds a new service journey interchange with id 1
   *
   * @return CreateServiceJourneyInterchange
   */
  public CreateServiceJourneyInterchange createServiceJourneyInterchange() {
    return createServiceJourneyInterchange(1);
  }

  /**
   * Adds a new service link with the given id
   *
   * @param id the id of the service link
   * @return CreateServiceLink
   */
  public CreateServiceLink createServiceLink(
    int id,
    ScheduledStopPointRefStructure fromScheduledStopPointRef,
    ScheduledStopPointRefStructure toScheduledStopPointRef
  ) {
    CreateServiceLink createServiceLink = new CreateServiceLink(id)
      .withFromScheduledStopPointRef(fromScheduledStopPointRef)
      .withToScheduledStopPointRef(toScheduledStopPointRef);
    serviceLinks.add(createServiceLink);
    return createServiceLink;
  }

  /**
   * Adds a new service link with id 1
   *
   * @return CreateServiceLink
   */
  public CreateServiceLink createServiceLink(
    ScheduledStopPointRefStructure fromScheduledStopPointRef,
    ScheduledStopPointRefStructure toScheduledStopPointRef
  ) {
    return createServiceLink(
      1,
      fromScheduledStopPointRef,
      toScheduledStopPointRef
    );
  }

  /**
   * Adds a new flexible stop place with the given id
   *
   * @param id the id of the flexible stop place
   * @return CreateFlexibleStopPlace
   */
  public CreateFlexibleStopPlace createFlexibleStopPlace(int id) {
    CreateFlexibleStopPlace createFlexibleStopPlace =
      new CreateFlexibleStopPlace(id);
    flexibleStopPlaces.add(createFlexibleStopPlace);
    return createFlexibleStopPlace;
  }

  /**
   * Adds a new flexible stop place with id 1
   *
   * @return CreateFlexibleStopPlace
   */
  public CreateFlexibleStopPlace createFlexibleStopPlace() {
    return createFlexibleStopPlace(1);
  }

  /**
   * Adds a new passenger stop assignment with the given id
   *
   * @param id the id of the passenger stop assignment
   * @return CreatePassengerStopAssignment
   */
  public CreatePassengerStopAssignment createPassengerStopAssignment(int id) {
    CreatePassengerStopAssignment createPassengerStopAssignment =
      new CreatePassengerStopAssignment(id);
    passengerStopAssignments.add(createPassengerStopAssignment);
    return createPassengerStopAssignment;
  }

  /**
   * Adds a new passenger stop assignment with id 1
   *
   * @return CreatePassengerStopAssignment
   */
  public CreatePassengerStopAssignment createPassengerStopAssignment() {
    return createPassengerStopAssignment(1);
  }

  /**
   * Create a new CompositeFrame with the given id
   *
   * @param id the id of the CompositeFrame
   * @return CreateCompositeFrame
   */
  public CreateCompositeFrame createCompositeFrame(int id) {
    compositeFrame = new CreateCompositeFrame(id);
    return compositeFrame;
  }

  /**
   * Create a new CompositeFrame with id 1
   *
   * @return CreateCompositeFrame
   */
  public CreateCompositeFrame createCompositeFrame() {
    return createCompositeFrame(1);
  }

  /**
   * Adds new service calendar frame with the given id
   * The existing service calendar frame will be overwritten.
   * The service calendar frame will be added outside the composite frame.
   *
   * @param id the id of the service calendar frame
   * @return CreateServiceCalendarFrame
   */
  public CreateServiceCalendarFrame createServiceCalendarFrame(int id) {
    if (serviceCalendarFrames == null) {
      serviceCalendarFrames = new ArrayList<>();
    }
    CreateServiceCalendarFrame serviceCalendarFrame =
      new CreateServiceCalendarFrame(id);
    serviceCalendarFrames.add(serviceCalendarFrame);
    return serviceCalendarFrame;
  }

  /**
   * Adds new service calendar frame with id 1
   * The existing service calendar frame will be overwritten.
   * The service calendar frame will be added outside the composite frame.
   *
   * @return CreateServiceCalendarFrame
   */
  public CreateServiceCalendarFrame createServiceCalendarFrame() {
    return createServiceCalendarFrame(1);
  }

  /**
   * Creates the new ScheduledStopPointRefStructure with the given id
   *
   * @param id the id of the ScheduledStopPoint
   * @return ScheduledStopPointRefStructure
   */
  public static ScheduledStopPointRefStructure createScheduledStopPointRef(
    int id
  ) {
    return new ScheduledStopPointRefStructure()
      .withRef("TST:ScheduledStopPoint:" + id);
  }

  /**
   * Creates the new QuayRefStructure with the given id
   *
   * @param id the id of the Quay
   * @return QuayRefStructure
   */
  public static QuayRefStructure createQuayRef(int id) {
    return new QuayRefStructure().withRef("TST:Quay:" + id);
  }

  /**
   * Creates the new StopPlaceRefStructure with the given id
   *
   * @param id the id of the StopPlace
   * @return StopPlaceRefStructure
   */
  public static StopPlaceRefStructure createStopPointRef(int id) {
    return new StopPlaceRefStructure().withRef("TST:StopPoint:" + id);
  }

  /**
   * Creates the new StopPlaceRefStructure with the given id
   *
   * @param id the id of the StopPlace
   * @return StopPlaceRefStructure
   */
  public static ServiceLinkRefStructure createServiceLinkRef(int id) {
    return new ServiceLinkRefStructure().withRef("TST:ServiceLink:" + id);
  }

  /**
   * Creates the new VehicleJourneyRefStructure with the given id
   *
   * @param id the id of the VehicleJourney
   * @return VehicleJourneyRefStructure
   */
  public static VehicleJourneyRefStructure createServiceJourneyRef(int id) {
    return new VehicleJourneyRefStructure().withRef("TST:ServiceJourney:" + id);
  }

  /**
   * Creates the new DatedServiceJourneyRefStructure with the given id
   *
   * @param id the id of the DatedServiceJourney
   * @return DatedServiceJourneyRefStructure
   */
  public static DestinationDisplayRefStructure createDestinationDisplayRef(
    int id
  ) {
    return new DestinationDisplayRefStructure()
      .withRef("TST:DestinationDisplay:" + id);
  }

  /**
   * This interface enables the CreateEntity classes to the reference object of their ids.
   *
   * @param <R>
   */
  public interface CreateRef<R extends VersionOfObjectRefStructure> {
    R refObject();
  }

  /**
   * Abstract class for automatic handling of entity reference.
   * It creates the reference using reflection, based on the integer id provided
   * in the constructor
   */
  public abstract static class CreateEntity<T extends EntityStructure> {

    protected final int id;

    public CreateEntity(int id) {
      this.id = id;
    }

    public final String ref() {
      Type type =
        (
          (ParameterizedType) getClass().getGenericSuperclass()
        ).getActualTypeArguments()[0];
      return "TST:" + ((Class<?>) type).getSimpleName() + ":" + id;
    }

    public abstract T create();
  }

  public static class CreateCompositeFrame
    extends CreateEntity<CompositeFrame> {

    private CreateValidBetween validBetween;
    private List<CreateServiceCalendarFrame> serviceCalendarFrames;

    public CreateCompositeFrame(int id) {
      super(id);
    }

    /**
     * Adds new service calendar frame with the given id
     * The existing service calendar frame will be overwritten.
     * The service calendar frame will be added in the composite frame.
     *
     * @param id the id of the service calendar frame
     * @return CreateServiceCalendarFrame
     */
    public CreateServiceCalendarFrame createServiceCalendarFrame(int id) {
      if (serviceCalendarFrames == null) {
        serviceCalendarFrames = new ArrayList<>();
      }
      CreateServiceCalendarFrame serviceCalendarFrame =
        new CreateServiceCalendarFrame(id);
      serviceCalendarFrames.add(serviceCalendarFrame);
      return serviceCalendarFrame;
    }

    /**
     * Adds new service calendar frame with id 1
     * The existing service calendar frame will be overwritten.
     * The service calendar frame will be added in the composite frame.
     *
     * @return CreateServiceCalendarFrame
     */
    public CreateServiceCalendarFrame createServiceCalendarFrame() {
      return createServiceCalendarFrame(1);
    }

    public CompositeFrame create() {
      CompositeFrame compositeFrame = new CompositeFrame().withId(ref());

      if (validBetween != null) {
        compositeFrame.withValidBetween(validBetween.create());
      }

      if (serviceCalendarFrames != null) {
        compositeFrame.setFrames(
          new Frames_RelStructure()
            .withCommonFrame(
              serviceCalendarFrames
                .stream()
                .map(CreateServiceCalendarFrame::create)
                .map(JAXBUtils::createJaxbElement)
                .collect(Collectors.toCollection(ArrayList::new))
            )
        );
      }
      return compositeFrame;
    }
  }

  public static class CreateValidBetween extends CreateEntity<ValidBetween> {

    private LocalDateTime fromDateTime;
    private LocalDateTime localDateTime;

    public CreateValidBetween(int id) {
      super(id);
    }

    public CreateValidBetween withFromDate(LocalDateTime fromDateTime) {
      this.fromDateTime = fromDateTime;
      return this;
    }

    public CreateValidBetween withToDate(LocalDateTime localDateTime) {
      this.localDateTime = localDateTime;
      return this;
    }

    @Override
    public ValidBetween create() {
      return new ValidBetween()
        .withFromDate(fromDateTime)
        .withToDate(localDateTime);
    }
  }

  protected abstract static class CreateGenericCalender<
    T extends EntityStructure
  >
    extends CreateEntity<T> {

    protected CreateValidBetween validBetween;
    protected List<CreateDayTypeAssignment> dayTypeAssignments;
    protected List<CreateDayType> dayTypes;
    protected List<CreateOperatingDay> operatingDays;
    protected List<CreateOperatingPeriod> operatingPeriods;

    public CreateGenericCalender(int id) {
      super(id);
    }

    public void withValidBetween(CreateValidBetween validBetween) {
      this.validBetween = validBetween;
    }

    public CreateValidBetween createValidBetween(
      LocalDateTime fromDateTime,
      LocalDateTime toDateTime
    ) {
      validBetween =
        new CreateValidBetween(1)
          .withFromDate(fromDateTime)
          .withToDate(toDateTime);
      return validBetween;
    }

    /**
     * Adds a new operating day with the given id
     *
     * @param id the id of the operating day
     * @return CreateOperatingDay
     */
    public CreateOperatingDay createOperatingDay(int id, LocalDate date) {
      if (operatingDays == null) {
        operatingDays = new ArrayList<>();
      }
      CreateOperatingDay operatingDay = new CreateOperatingDay(id, date);
      operatingDays.add(operatingDay);
      return operatingDay;
    }

    /**
     * Adds a new operating day with id 1
     *
     * @return CreateOperatingDay
     */
    public CreateOperatingDay createOperatingDay(LocalDate date) {
      return createOperatingDay(1, date);
    }

    /**
     * Adds numberOfOperatingDays new operating days starting from the given date, adding one day for each operating day.
     *
     * @param numberOfOperatingDays the number of operating days to create
     * @param date the start date for the first operating day
     * @return List of CreateOperatingDay
     */
    public List<CreateOperatingDay> createOperatingDays(
      int numberOfOperatingDays,
      LocalDate date
    ) {
      return IntStream
        .rangeClosed(1, numberOfOperatingDays)
        .mapToObj(i -> createOperatingDay(i, date.plusDays(i - 1)))
        .toList();
    }

    /**
     * Adds a new operating period with the given id
     *
     * @param id the id of the operating period
     * @return CreateOperatingPeriod
     */
    public CreateOperatingPeriod createOperatingPeriod(
      int id,
      LocalDate fromDate,
      LocalDate toDate
    ) {
      if (operatingPeriods == null) {
        operatingPeriods = new ArrayList<>();
      }
      CreateOperatingPeriod operatingPeriod = new CreateOperatingPeriod(
        id,
        fromDate,
        toDate
      );
      operatingPeriods.add(operatingPeriod);
      return operatingPeriod;
    }

    /**
     * Adds a new operating period with id 1
     *
     * @return CreateOperatingPeriod
     */
    public CreateOperatingPeriod createOperatingPeriod(
      LocalDate fromDate,
      LocalDate toDate
    ) {
      return createOperatingPeriod(1, fromDate, toDate);
    }

    /**
     * Adds numberOfOperatingPeriods new operating periods starting from the given dates, adding one day for both from and to dates.
     *
     * @param numberOfOperatingPeriods the number of operating periods to create
     * @param fromDate the start date for the first operating period
     * @param toDate the end date for the first operating period
     * @return List of CreateOperatingPeriod
     */
    public List<CreateOperatingPeriod> createOperatingPeriods(
      int numberOfOperatingPeriods,
      LocalDate fromDate,
      LocalDate toDate
    ) {
      return IntStream
        .rangeClosed(1, numberOfOperatingPeriods)
        .mapToObj(i ->
          createOperatingPeriod(
            i,
            fromDate.plusDays(i - 1),
            toDate.plusDays(i - 1)
          )
        )
        .toList();
    }

    public List<CreateOperatingPeriod> createOperatingPeriods(
      int numberOfOperatingPeriods,
      LocalDate fromDate,
      LocalDate toDate,
      int numberOfDaysBetweenPeriods
    ) {
      return IntStream
        .rangeClosed(1, numberOfOperatingPeriods)
        .mapToObj(i ->
          createOperatingPeriod(
            i,
            fromDate.plusDays((long) (i - 1) * numberOfDaysBetweenPeriods),
            toDate.plusDays((long) (i - 1) * numberOfDaysBetweenPeriods)
          )
        )
        .toList();
    }

    /**
     * Adds a new day type with the given id
     *
     * @param id the id of the day type
     * @return CreateDayType
     */
    public CreateDayType createDayType(int id) {
      if (dayTypes == null) {
        dayTypes = new ArrayList<>();
      }
      CreateDayType dayType = new CreateDayType(id);
      dayTypes.add(dayType);
      return dayType;
    }

    /**
     * Adds a new day type with id 1
     *
     * @return CreateDayType
     */
    public CreateDayType createDayType() {
      return createDayType(1);
    }

    /**
     * Adds numberOfDayTypes new day types with the given ids
     * The day types will be created with the given dayOfWeeks in circular fashion.
     * If no dayOfWeeks are provided, the day types will be created without daysOfWeek.
     *
     * @param numberOfDayTypes the number of day types to create
     * @param dayOfWeeks the day of weeks to assign to the day types
     * @return List of CreateDayType
     */
    public List<CreateDayType> createDayTypes(
      int numberOfDayTypes,
      DayOfWeekEnumeration... dayOfWeeks
    ) {
      List<CreateDayType> createDayTypes = IntStream
        .rangeClosed(1, numberOfDayTypes)
        .mapToObj(this::createDayType)
        .toList();

      if (dayOfWeeks.length > 0) {
        return createDayTypes
          .stream()
          .map(createDayType ->
            createDayType.withDaysOfWeek(
              dayOfWeeks[(createDayType.id - 1) % dayOfWeeks.length]
            )
          )
          .toList();
      }

      return createDayTypes;
    }

    /**
     * Adds a new day type assignment with the given id
     *
     * @param id the id of the day type assignment
     * @return CreateDayTypeAssignment
     */
    public CreateDayTypeAssignment createDayTypeAssignment(
      int id,
      CreateDayType dayTypeRef
    ) {
      if (dayTypeAssignments == null) {
        dayTypeAssignments = new ArrayList<>();
      }
      CreateDayTypeAssignment dayTypeAssignment = new CreateDayTypeAssignment(
        id,
        dayTypeRef
      );
      dayTypeAssignments.add(dayTypeAssignment);
      return dayTypeAssignment;
    }

    /**
     * Adds a new day type assignment with id 1
     *
     * @return CreateDayTypeAssignment
     */
    public CreateDayTypeAssignment createDayTypeAssignment(
      CreateDayType dayTypeRef
    ) {
      return createDayTypeAssignment(1, dayTypeRef);
    }

    /**
     * Creates the dayTypes.size() number of day type assignments with dates starting from startDate, adding one day for each day type assignment.
     *
     * @param dayTypes the day types to create day type assignments for
     * @param startDate the start date for the first day type assignment
     * @return List of CreateDayTypeAssignment
     */
    public List<CreateDayTypeAssignment> createDayTypeAssignmentsWithDates(
      List<CreateDayType> dayTypes,
      LocalDate startDate
    ) {
      return IntStream
        .range(0, dayTypes.size())
        .mapToObj(i ->
          createDayTypeAssignment(i + 1, dayTypes.get(i))
            .withDate(startDate.plusDays(i))
        )
        .toList();
    }

    /**
     * Creates the dayTypes.size() number of day type assignments with operating days, by traversing the operatingDays list in circular fashion.
     *
     * @param dayTypes the day types to create day type assignments for
     * @param operatingDays the operating days to assign to the day type assignments
     * @return List of CreateDayTypeAssignment
     */
    public List<CreateDayTypeAssignment> createDayTypeAssignmentsWithOperatingDays(
      List<CreateDayType> dayTypes,
      List<CreateOperatingDay> operatingDays
    ) {
      List<CreateDayTypeAssignment> createDayTypeAssignments = IntStream
        .range(0, dayTypes.size())
        .mapToObj(i -> createDayTypeAssignment(i + 1, dayTypes.get(i)))
        .toList();

      if (operatingDays != null && !operatingDays.isEmpty()) {
        return IntStream
          .range(0, createDayTypeAssignments.size())
          .mapToObj(i ->
            createDayTypeAssignments
              .get(i)
              .withOperatingDayRef(operatingDays.get(i % operatingDays.size()))
          )
          .toList();
      }

      return createDayTypeAssignments;
    }

    /**
     * Creates the dayTypes.size() number of day type assignments with operating periods, by traversing the operatingPeriods list in circular fashion.
     *
     * @param dayTypes the day types to create day type assignments for
     * @param operatingPeriods the operating periods to assign to the day type assignments
     * @return List of CreateDayTypeAssignment
     */
    public List<CreateDayTypeAssignment> createDayTypeAssignmentsWithOperatingPeriods(
      List<CreateDayType> dayTypes,
      List<CreateOperatingPeriod> operatingPeriods
    ) {
      List<CreateDayTypeAssignment> dayTypeAssignments = IntStream
        .range(0, dayTypes.size())
        .mapToObj(i -> createDayTypeAssignment(i + 1, dayTypes.get(i)))
        .toList();

      if (operatingPeriods != null && !operatingPeriods.isEmpty()) {
        return IntStream
          .range(0, dayTypeAssignments.size())
          .mapToObj(i ->
            dayTypeAssignments
              .get(i)
              .withOperatingPeriodRef(
                operatingPeriods.get(i % operatingPeriods.size())
              )
          )
          .toList();
      }

      return dayTypeAssignments;
    }
  }

  public static class CreateServiceCalendarFrame
    extends CreateGenericCalender<ServiceCalendarFrame> {

    private CreateServiceCalendar serviceCalendar;

    public CreateServiceCalendarFrame(int id) {
      super(id);
    }

    public CreateServiceCalendar createServiceCalendar(int id) {
      serviceCalendar = new CreateServiceCalendar(id);
      return serviceCalendar;
    }

    public CreateServiceCalendar createServiceCalendar() {
      return createServiceCalendar(1);
    }

    @Override
    public ServiceCalendarFrame create() {
      ServiceCalendarFrame serviceCalendarFrame = new ServiceCalendarFrame()
        .withId(ref());

      if (validBetween != null) {
        serviceCalendarFrame.withValidBetween(validBetween.create());
      }

      if (serviceCalendar != null) {
        serviceCalendarFrame.withServiceCalendar(serviceCalendar.create());
      }

      if (dayTypeAssignments != null) {
        DayTypeAssignmentsInFrame_RelStructure dayTypeAssignmentsInFrameRelStructure =
          new DayTypeAssignmentsInFrame_RelStructure()
            .withDayTypeAssignment(
              dayTypeAssignments
                .stream()
                .map(CreateDayTypeAssignment::create)
                .collect(Collectors.toCollection(ArrayList::new))
            );
        serviceCalendarFrame.setDayTypeAssignments(
          dayTypeAssignmentsInFrameRelStructure
        );
      }

      if (dayTypes != null) {
        DayTypesInFrame_RelStructure dayTypesInFrameRelStructure =
          new DayTypesInFrame_RelStructure()
            .withDayType_(
              dayTypes
                .stream()
                .map(CreateDayType::create)
                .map(JAXBUtils::createJaxbElement)
                .collect(Collectors.toCollection(ArrayList::new))
            );
        serviceCalendarFrame.setDayTypes(dayTypesInFrameRelStructure);
      }

      if (operatingDays != null) {
        OperatingDaysInFrame_RelStructure operatingDaysInFrameRelStructure =
          new OperatingDaysInFrame_RelStructure()
            .withOperatingDay(
              operatingDays
                .stream()
                .map(CreateOperatingDay::create)
                .collect(Collectors.toCollection(ArrayList::new))
            );
        serviceCalendarFrame.setOperatingDays(operatingDaysInFrameRelStructure);
      }

      if (operatingPeriods != null) {
        OperatingPeriodsInFrame_RelStructure operatingPeriodsInFrameRelStructure =
          new OperatingPeriodsInFrame_RelStructure()
            .withOperatingPeriodOrUicOperatingPeriod(
              operatingPeriods
                .stream()
                .map(CreateOperatingPeriod::create)
                .collect(Collectors.toCollection(ArrayList::new))
            );
        serviceCalendarFrame.setOperatingPeriods(
          operatingPeriodsInFrameRelStructure
        );
      }

      return serviceCalendarFrame;
    }
  }

  public static class CreateServiceCalendar
    extends CreateGenericCalender<ServiceCalendar> {

    public CreateServiceCalendar(int id) {
      super(id);
    }

    @Override
    public ServiceCalendar create() {
      ServiceCalendar serviceCalendar = new ServiceCalendar().withId(ref());

      if (validBetween != null) {
        serviceCalendar.withValidBetween(validBetween.create());
      }

      if (dayTypeAssignments != null) {
        DayTypeAssignments_RelStructure dayTypeAssignmentsRelStructure =
          new DayTypeAssignments_RelStructure()
            .withDayTypeAssignment(
              dayTypeAssignments
                .stream()
                .map(CreateDayTypeAssignment::create)
                .collect(Collectors.toCollection(ArrayList::new))
            );
        serviceCalendar.setDayTypeAssignments(dayTypeAssignmentsRelStructure);
      }
      if (dayTypes != null) {
        DayTypes_RelStructure dayTypesRelStructure = new DayTypes_RelStructure()
          .withDayTypeRefOrDayType_(
            dayTypes
              .stream()
              .map(CreateDayType::create)
              .map(JAXBUtils::createJaxbElement)
              .collect(Collectors.toCollection(ArrayList::new))
          );
        serviceCalendar.setDayTypes(dayTypesRelStructure);
      }

      if (operatingDays != null) {
        OperatingDays_RelStructure operatingDaysRelStructure =
          new OperatingDays_RelStructure()
            .withOperatingDayRefOrOperatingDay(
              operatingDays
                .stream()
                .map(CreateOperatingDay::create)
                .collect(Collectors.toCollection(ArrayList::new))
            );
        serviceCalendar.setOperatingDays(operatingDaysRelStructure);
      }

      if (operatingPeriods != null) {
        OperatingPeriods_RelStructure operatingPeriodsRelStructure =
          new OperatingPeriods_RelStructure()
            .withOperatingPeriodRefOrOperatingPeriodOrUicOperatingPeriod(
              operatingPeriods
                .stream()
                .map(CreateOperatingPeriod::create)
                .map(JAXBUtils::createJaxbElement)
                .collect(Collectors.toCollection(ArrayList::new))
            );
        serviceCalendar.setOperatingPeriods(operatingPeriodsRelStructure);
      }

      return serviceCalendar;
    }
  }

  public static class CreateFlexibleArea extends CreateEntity<FlexibleArea> {

    private List<Double> coordinates;
    private boolean withNullPolygon = false;

    public CreateFlexibleArea(int id) {
      super(id);
    }

    public CreateFlexibleArea withCoordinates(List<Double> coordinates) {
      this.coordinates = coordinates;
      return this;
    }

    public CreateFlexibleArea withNullPolygon(boolean withNullPolygon) {
      this.withNullPolygon = withNullPolygon;
      return this;
    }

    public FlexibleArea create() {
      LinearRingType linearRing = new LinearRingType();
      DirectPositionListType positionList = new DirectPositionListType()
        .withValue(coordinates);
      linearRing.withPosList(positionList);

      FlexibleArea flexibleArea = new FlexibleArea()
        .withId(ref())
        .withName(new MultilingualString().withValue("FlexibleArea " + id));

      if (withNullPolygon) {
        return flexibleArea.withPolygon(null);
      }

      return flexibleArea.withPolygon(
        new PolygonType()
          .withExterior(
            new AbstractRingPropertyType()
              .withAbstractRing(
                new ObjectFactory().createLinearRing(linearRing)
              )
          )
      );
    }
  }

  public static class CreateFlexibleStopPlace
    extends CreateEntity<FlexibleStopPlace> {

    private CreateFlexibleArea flexibleArea;

    public CreateFlexibleStopPlace(int id) {
      super(id);
    }

    /**
     * Creates a new flexible area with the given id,
     * if it does not already exist.
     *
     * @param id the id of the flexible area
     * @return CreateFlexibleArea
     */
    public CreateFlexibleArea flexibleArea(int id) {
      if (flexibleArea == null) {
        flexibleArea = new CreateFlexibleArea(id);
      }
      return flexibleArea;
    }

    public FlexibleStopPlace create() {
      return new FlexibleStopPlace()
        .withId(ref())
        .withName(new MultilingualString().withValue("FlexibleStopPlace " + id))
        .withAreas(
          new FlexibleStopPlace_VersionStructure.Areas()
            .withFlexibleAreaOrFlexibleAreaRefOrHailAndRideArea(
              Optional
                .ofNullable(flexibleArea)
                .map(CreateFlexibleArea::create)
                .orElse(null)
            )
        );
    }
  }

  public static class CreatePassengerStopAssignment
    extends CreateEntity<PassengerStopAssignment> {

    private ScheduledStopPointRefStructure scheduleStopPointRef;

    private StopPlaceRefStructure stopPlaceRef;

    private QuayRefStructure quayRef;

    public CreatePassengerStopAssignment(int id) {
      super(id);
    }

    public CreatePassengerStopAssignment withScheduledStopPointRef(
      ScheduledStopPointRefStructure scheduledStopPointRef
    ) {
      this.scheduleStopPointRef = scheduledStopPointRef;
      return this;
    }

    public CreatePassengerStopAssignment withStopPlaceRef(
      StopPlaceRefStructure stopPlaceRef
    ) {
      this.stopPlaceRef = stopPlaceRef;
      return this;
    }

    public CreatePassengerStopAssignment withQuayRef(QuayRefStructure QuayRef) {
      this.quayRef = QuayRef;
      return this;
    }

    public PassengerStopAssignment create() {
      return new PassengerStopAssignment()
        .withId(ref())
        .withScheduledStopPointRef(createJaxbElement(scheduleStopPointRef))
        .withQuayRef(createJaxbElement(quayRef))
        .withStopPlaceRef(createJaxbElement(stopPlaceRef));
    }
  }

  public static class CreateDatedServiceJourney
    extends CreateEntity<DatedServiceJourney> {

    private final CreateOperatingDay operatingDayRef;
    private final CreateServiceJourney serviceJourneyRef;
    private CreateDatedServiceJourney datedServiceJourneyRef;
    private ServiceAlterationEnumeration serviceAlteration;

    public CreateDatedServiceJourney(
      int id,
      CreateServiceJourney serviceJourneyRef,
      CreateOperatingDay operatingDayRef
    ) {
      super(id);
      this.serviceJourneyRef = serviceJourneyRef;
      this.operatingDayRef = operatingDayRef;
    }

    public CreateDatedServiceJourney withServiceAlteration(
      ServiceAlterationEnumeration serviceAlteration
    ) {
      this.serviceAlteration = serviceAlteration;
      return this;
    }

    public CreateDatedServiceJourney withDatedServiceJourneyRef(
      CreateDatedServiceJourney datedServiceJourneyRef
    ) {
      this.datedServiceJourneyRef = datedServiceJourneyRef;
      return this;
    }

    public DatedServiceJourney create() {
      DatedServiceJourney datedServiceJourney = new DatedServiceJourney()
        .withId(ref());

      Collection<JAXBElement<? extends JourneyRefStructure>> journeyRefs =
        new ArrayList<>();
      journeyRefs.add(
        createJaxbElement(
          new ServiceJourneyRefStructure().withRef(serviceJourneyRef.ref())
        )
      );
      if (datedServiceJourneyRef != null) {
        journeyRefs.add(
          createJaxbElement(
            new DatedServiceJourneyRefStructure()
              .withRef(datedServiceJourneyRef.ref())
          )
        );
      }

      return datedServiceJourney
        .withJourneyRef(journeyRefs)
        .withOperatingDayRef(
          new OperatingDayRefStructure().withRef(operatingDayRef.ref())
        )
        .withServiceAlteration(serviceAlteration);
    }
  }

  public static class CreateOperatingPeriod
    extends CreateEntity<OperatingPeriod> {

    private final LocalDate fromDate;
    private final LocalDate toDate;

    public CreateOperatingPeriod(int id, LocalDate fromDate, LocalDate toDate) {
      super(id);
      this.fromDate = fromDate;
      this.toDate = toDate;
    }

    public OperatingPeriod create() {
      return new OperatingPeriod()
        .withId(ref())
        .withFromDate(fromDate.atStartOfDay())
        .withToDate(toDate.atStartOfDay());
    }
  }

  public static class CreateOperatingDay extends CreateEntity<OperatingDay> {

    private final LocalDate calendarDate;

    public CreateOperatingDay(int id, LocalDate calendarDate) {
      super(id);
      this.calendarDate = calendarDate;
    }

    public OperatingDay create() {
      return new OperatingDay()
        .withId(ref())
        .withCalendarDate(calendarDate.atStartOfDay());
    }
  }

  public static class CreateDayType extends CreateEntity<DayType> {

    private Collection<DayOfWeekEnumeration> daysOfWeek;
    private boolean asSinglePropertyOfDay;

    public CreateDayType(int id) {
      super(id);
    }

    public CreateDayType withDaysOfWeek(DayOfWeekEnumeration... daysOfWeek) {
      this.daysOfWeek = Arrays.asList(daysOfWeek);
      return this;
    }

    public DayType create() {
      DayType dayType = new DayType().withId(ref());

      if (daysOfWeek != null && !daysOfWeek.isEmpty()) {
        return dayType.withProperties(
          new PropertiesOfDay_RelStructure()
            .withPropertyOfDay(new PropertyOfDay().withDaysOfWeek(daysOfWeek))
        );
      }
      return dayType;
    }
  }

  public static class CreateDayTypeAssignment
    extends CreateEntity<DayTypeAssignment> {

    private LocalDate date;
    private final CreateDayType dayTypeRef;
    private CreateOperatingDay operatingDayRef;
    private CreateOperatingPeriod operatingPeriodRef;

    public CreateDayTypeAssignment(int id, CreateDayType DayTypeRef) {
      super(id);
      this.dayTypeRef = DayTypeRef;
    }

    public CreateDayTypeAssignment withDate(LocalDate date) {
      this.date = date;
      this.operatingDayRef = null;
      this.operatingPeriodRef = null;
      return this;
    }

    public CreateDayTypeAssignment withOperatingDayRef(
      CreateOperatingDay operatingDayRef
    ) {
      this.operatingDayRef = operatingDayRef;
      this.date = null;
      this.operatingPeriodRef = null;
      return this;
    }

    public CreateDayTypeAssignment withOperatingPeriodRef(
      CreateOperatingPeriod operatingPeriodRef
    ) {
      this.operatingPeriodRef = operatingPeriodRef;
      this.date = null;
      this.operatingDayRef = null;
      return this;
    }

    public DayTypeAssignment create() {
      DayTypeAssignment dayTypeAssignment = new DayTypeAssignment()
        .withId(ref())
        .withDayTypeRef(
          createJaxbElement(new DayTypeRefStructure().withRef(dayTypeRef.ref()))
        );

      Optional
        .ofNullable(date)
        .ifPresent(d -> dayTypeAssignment.withDate(d.atStartOfDay()));

      Optional
        .ofNullable(operatingDayRef)
        .map(CreateOperatingDay::ref)
        .ifPresent(ref ->
          dayTypeAssignment.withOperatingDayRef(
            new OperatingDayRefStructure().withRef(ref)
          )
        );

      Optional
        .ofNullable(operatingPeriodRef)
        .map(CreateOperatingPeriod::ref)
        .ifPresent(ref ->
          dayTypeAssignment.withOperatingPeriodRef(
            createJaxbElement(new OperatingPeriodRefStructure().withRef(ref))
          )
        );

      return dayTypeAssignment;
    }
  }

  public abstract static class CreateGenericLine<
    T extends Line_VersionStructure
  >
    extends CreateEntity<T> {

    protected AllVehicleModesOfTransportEnumeration transportMode;
    protected TransportSubmodeStructure transportSubmode;

    public CreateGenericLine(int id) {
      super(id);
    }

    public CreateGenericLine<T> withTransportMode(
      AllVehicleModesOfTransportEnumeration transportMode
    ) {
      this.transportMode = transportMode;
      return this;
    }

    public CreateGenericLine<T> withTransportSubmode(
      TransportSubmodeStructure transportSubmode
    ) {
      this.transportSubmode = transportSubmode;
      return this;
    }
  }

  public static class CreateLine extends CreateGenericLine<Line> {

    public CreateLine(int id) {
      super(id);
    }

    public Line create() {
      return new Line()
        .withId(ref())
        .withName(new MultilingualString().withValue("Line " + id))
        .withTransportMode(transportMode)
        .withTransportSubmode(transportSubmode);
    }
  }

  public static class CreateFlexibleLine
    extends CreateGenericLine<FlexibleLine> {

    private FlexibleLineTypeEnumeration flexibleLineType;

    public CreateFlexibleLine(int id) {
      super(id);
    }

    public CreateFlexibleLine withFlexibleLineType(
      FlexibleLineTypeEnumeration flexibleLineType
    ) {
      this.flexibleLineType = flexibleLineType;
      return this;
    }

    public FlexibleLine create() {
      return new FlexibleLine()
        .withId(ref())
        .withFlexibleLineType(flexibleLineType)
        .withName(new MultilingualString().withValue("FlexibleLine " + id))
        .withTransportMode(transportMode)
        .withTransportSubmode(transportSubmode);
    }
  }

  public static class CreateRoute extends CreateEntity<Route> {

    private final CreateGenericLine<? extends Line_VersionStructure> lineRef;

    public CreateRoute(
      int id,
      CreateGenericLine<? extends Line_VersionStructure> lineRef
    ) {
      super(id);
      this.lineRef = lineRef;
    }

    public Route create() {
      return new Route()
        .withId(ref())
        .withLineRef(
          createJaxbElement(new LineRefStructure().withRef(lineRef.ref()))
        );
    }
  }

  public static class CreateJourneyPattern
    extends CreateEntity<JourneyPattern> {

    private CreateRoute routeRef;

    private final List<CreateStopPointInJourneyPattern> stopPointsInJourneyPatterns =
      new ArrayList<>();

    private final List<CreateLinkInJourneyPattern> serviceLinksInJourneyPatterns =
      new ArrayList<>();

    private boolean noServiceLinksInJourneyPattern = false;

    public CreateJourneyPattern(int id) {
      super(id);
    }

    public CreateJourneyPattern withRoute(CreateRoute routeRef) {
      this.routeRef = routeRef;
      return this;
    }

    public CreateJourneyPattern withNoServiceLinksInJourneyPattern() {
      this.noServiceLinksInJourneyPattern = true;
      return this;
    }

    /**
     * Adds a new stop point in the journey pattern with the given id
     *
     * @param id the id of the stop point in the journey pattern
     * @return CreateStopPointInJourneyPattern
     */
    public CreateStopPointInJourneyPattern createStopPointInJourneyPattern(
      int id
    ) {
      CreateStopPointInJourneyPattern createStopPointInJourneyPattern =
        new CreateStopPointInJourneyPattern(id)
          .withOrder(id)
          .withScheduledStopPointRef(createScheduledStopPointRef(id));
      stopPointsInJourneyPatterns.add(createStopPointInJourneyPattern);
      return createStopPointInJourneyPattern;
    }

    /**
     * Adds a new service link in the journey pattern with the given id
     *
     * @param id the id of the service link in the journey pattern
     * @return CreateLinkInJourneyPattern
     */
    public CreateLinkInJourneyPattern createServiceLinkInJourneyPattern(
      int id
    ) {
      CreateLinkInJourneyPattern createLinkInJourneyPattern =
        new CreateLinkInJourneyPattern(id);
      serviceLinksInJourneyPatterns.add(createLinkInJourneyPattern);
      return createLinkInJourneyPattern;
    }

    /**
     * Adds numberOfStopPointInJourneyPattern new stop points in the journey pattern
     * The stop points will have ids from 1 to numberOfStopPointInJourneyPattern
     *
     * @param numberOfStopPointInJourneyPattern the number of stop points to create
     * @return List of CreateStopPointInJourneyPattern created
     */
    public List<CreateStopPointInJourneyPattern> createStopPointsInJourneyPattern(
      int numberOfStopPointInJourneyPattern
    ) {
      List<CreateStopPointInJourneyPattern> stopPointsInJourneyPatterns =
        IntStream
          .rangeClosed(1, numberOfStopPointInJourneyPattern)
          .mapToObj(index -> {
            CreateStopPointInJourneyPattern createStopPointInJourneyPattern =
              new CreateStopPointInJourneyPattern(index)
                .withOrder(index)
                .withScheduledStopPointRef(createScheduledStopPointRef(index))
                .withForBoarding(index == 1) // first stop point
                .withForAlighting(index == numberOfStopPointInJourneyPattern); // last stop point

            // Setting destination display id for first and last stop point
            if (index == 1 || index == numberOfStopPointInJourneyPattern) {
              createStopPointInJourneyPattern.withDestinationDisplayId(
                createDestinationDisplayRef(index)
              );
            }

            return createStopPointInJourneyPattern;
          })
          .toList();

      this.stopPointsInJourneyPatterns.addAll(stopPointsInJourneyPatterns);
      return stopPointsInJourneyPatterns;
    }

    /**
     * Adds numberOfServiceLinksInJourneyPattern new service links in the journey pattern
     * The service links will have ids from 1 to numberOfServiceLinksInJourneyPattern
     *
     * @param numberOfServiceLinksInJourneyPattern the number of service links to create
     * @return List of CreateLinkInJourneyPattern created
     */
    public List<CreateLinkInJourneyPattern> createServiceLinksInJourneyPattern(
      int numberOfServiceLinksInJourneyPattern
    ) {
      List<CreateLinkInJourneyPattern> linksInJourneyPatterns = IntStream
        .range(0, numberOfServiceLinksInJourneyPattern)
        .mapToObj(index ->
          new CreateLinkInJourneyPattern(index + 1)
            .withOrder(index + 1)
            .withServiceLinkRef(createServiceLinkRef(index + 1))
        )
        .toList();

      serviceLinksInJourneyPatterns.addAll(linksInJourneyPatterns);
      return linksInJourneyPatterns;
    }

    public JourneyPattern create() {
      JourneyPattern journeyPattern = new JourneyPattern().withId(ref());

      if (routeRef != null) {
        journeyPattern.withRouteRef(
          new RouteRefStructure().withRef(routeRef.ref())
        );
      }

      journeyPattern.withPointsInSequence(
        new PointsInJourneyPattern_RelStructure()
          .withPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern(
            this.stopPointsInJourneyPatterns.isEmpty()
              ? List.of()
              : this.stopPointsInJourneyPatterns.stream()
                .map(CreateStopPointInJourneyPattern::create)
                .map(PointInLinkSequence_VersionedChildStructure.class::cast)
                .toList()
          )
      );

      if (!noServiceLinksInJourneyPattern) {
        journeyPattern.withLinksInSequence(
          new LinksInJourneyPattern_RelStructure()
            .withServiceLinkInJourneyPatternOrTimingLinkInJourneyPattern(
              this.serviceLinksInJourneyPatterns.isEmpty()
                ? List.of()
                : this.serviceLinksInJourneyPatterns.stream()
                  .map(CreateLinkInJourneyPattern::create)
                  .map(LinkInLinkSequence_VersionedChildStructure.class::cast)
                  .toList()
            )
        );
      }

      return journeyPattern;
    }
  }

  public static class CreateStopPointInJourneyPattern
    extends CreateEntity<StopPointInJourneyPattern> {

    private int order = 1;
    private ScheduledStopPointRefStructure scheduledStopPointRef;
    private DestinationDisplayRefStructure destinationDisplayRef;
    private boolean forAlighting = false;
    private boolean forBoarding = false;

    public CreateStopPointInJourneyPattern(int id) {
      super(id);
    }

    public CreateStopPointInJourneyPattern withOrder(int order) {
      this.order = order;
      return this;
    }

    public CreateStopPointInJourneyPattern withScheduledStopPointRef(
      ScheduledStopPointRefStructure scheduledStopPointRef
    ) {
      this.scheduledStopPointRef = scheduledStopPointRef;
      return this;
    }

    public CreateStopPointInJourneyPattern withDestinationDisplayId(
      DestinationDisplayRefStructure destinationDisplayRef
    ) {
      this.destinationDisplayRef = destinationDisplayRef;
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
          .withId(ref())
          .withOrder(BigInteger.valueOf(order));

      if (scheduledStopPointRef != null) {
        stopPointInJourneyPattern.withScheduledStopPointRef(
          createJaxbElement(scheduledStopPointRef)
        );
      }

      if (destinationDisplayRef != null) {
        stopPointInJourneyPattern.setDestinationDisplayRef(
          createJaxbElement(destinationDisplayRef).getValue()
        );
      }

      stopPointInJourneyPattern.withForAlighting(forAlighting);
      stopPointInJourneyPattern.withForBoarding(forBoarding);

      return stopPointInJourneyPattern;
    }
  }

  public static class CreateLinkInJourneyPattern
    extends CreateEntity<LinkInJourneyPattern> {

    private int order = 1;
    private ServiceLinkRefStructure serviceLinkRef;

    public CreateLinkInJourneyPattern(int id) {
      super(id);
    }

    public CreateLinkInJourneyPattern withOrder(int order) {
      this.order = order;
      return this;
    }

    public CreateLinkInJourneyPattern withServiceLinkRef(
      ServiceLinkRefStructure serviceLinkRef
    ) {
      this.serviceLinkRef = serviceLinkRef;
      return this;
    }

    public LinkInJourneyPattern create() {
      return new LinkInJourneyPattern()
        .withId(ref())
        .withOrder(BigInteger.valueOf(order))
        .withServiceLinkRef(serviceLinkRef);
    }
  }

  public abstract static class CreateJourney<T extends Journey_VersionStructure>
    extends CreateEntity<T> {

    protected final CreateGenericLine<? extends Line_VersionStructure> line;
    protected final CreateJourneyPattern journeyPattern;
    protected List<CreateTimetabledPassingTime> timetabledPassingTimes;
    protected List<DayTypeRefStructure> dayTypes;
    protected AllVehicleModesOfTransportEnumeration transportMode;
    protected TransportSubmodeStructure transportSubmode;

    public CreateJourney(
      int id,
      CreateGenericLine<? extends Line_VersionStructure> line,
      CreateJourneyPattern journeyPattern
    ) {
      super(id);
      this.line = line;
      this.journeyPattern = journeyPattern;
    }

    /**
     * Adds a new timetabled passing time with the given id
     *
     * @param id the id of the timetabled passing time
     * @param createStopPointInJourneyPattern the stop point in the journey pattern ref for the timetabled passing time
     * @return CreateTimetabledPassingTime
     */
    public CreateTimetabledPassingTime createTimetabledPassingTime(
      int id,
      CreateStopPointInJourneyPattern createStopPointInJourneyPattern
    ) {
      if (timetabledPassingTimes == null) {
        timetabledPassingTimes = new ArrayList<>();
      }
      CreateTimetabledPassingTime createTimetabledPassingTime =
        new CreateTimetabledPassingTime(id, createStopPointInJourneyPattern);
      timetabledPassingTimes.add(createTimetabledPassingTime);
      return createTimetabledPassingTime;
    }

    /**
     * Creates timetabled passing times for the given stop points in the journey pattern
     *
     * @param stopPointsInJourneyPattern the stop points in the journey pattern
     * @return List of CreateTimetabledPassingTime
     */
    public List<CreateTimetabledPassingTime> createTimetabledPassingTimes(
      List<CreateStopPointInJourneyPattern> stopPointsInJourneyPattern
    ) {
      return IntStream
        .range(0, stopPointsInJourneyPattern.size())
        .mapToObj(i ->
          createTimetabledPassingTime(i + 1, stopPointsInJourneyPattern.get(i))
        )
        .toList();
    }

    public CreateJourney<T> addDayTypeRef(DayTypeRefStructure dayTypeRef) {
      if (dayTypes == null) {
        dayTypes = new ArrayList<>();
      }
      dayTypes.add(dayTypeRef);
      return this;
    }

    public CreateJourney<T> addDayTypeRef(CreateDayType dayType) {
      return addDayTypeRef(new DayTypeRefStructure().withRef(dayType.ref()));
    }

    public CreateJourney<T> addDayTypeRefs(List<CreateDayType> dayTypes) {
      dayTypes.forEach(this::addDayTypeRef);
      return this;
    }

    public CreateJourney<T> withTransportMode(
      AllVehicleModesOfTransportEnumeration transportMode
    ) {
      this.transportMode = transportMode;
      return this;
    }

    public CreateJourney<T> withTransportSubmode(
      TransportSubmodeStructure transportSubmode
    ) {
      this.transportSubmode = transportSubmode;
      return this;
    }
  }

  public static class CreateDeadRun extends CreateJourney<DeadRun> {

    public CreateDeadRun(
      int id,
      CreateGenericLine<? extends Line_VersionStructure> lineRef,
      CreateJourneyPattern journeyPattern
    ) {
      super(id, lineRef, journeyPattern);
    }

    public DeadRun create() {
      DeadRun deadRun = new DeadRun()
        .withId(ref())
        .withLineRef(
          createJaxbElement(new LineRefStructure().withRef(line.ref()))
        )
        .withJourneyPatternRef(
          createJaxbElement(
            new JourneyPatternRefStructure().withRef(journeyPattern.ref())
          )
        );

      if (dayTypes != null) {
        deadRun.withDayTypes(
          new DayTypeRefs_RelStructure()
            .withDayTypeRef(
              dayTypes
                .stream()
                .map(JAXBUtils::createJaxbElement)
                .collect(Collectors.toCollection(ArrayList::new))
            )
        );
      }

      if (timetabledPassingTimes != null) {
        deadRun.withPassingTimes(
          new TimetabledPassingTimes_RelStructure()
            .withTimetabledPassingTime(
              timetabledPassingTimes
                .stream()
                .map(CreateTimetabledPassingTime::create)
                .toList()
            )
        );
      }

      if (transportMode != null) {
        deadRun.withTransportMode(transportMode);
      }

      if (transportSubmode != null) {
        deadRun.withTransportSubmode(transportSubmode);
      }

      return deadRun;
    }
  }

  public static class CreateServiceJourney
    extends CreateJourney<ServiceJourney>
    implements CreateRef<VersionOfObjectRefStructure> {

    public CreateServiceJourney(
      int id,
      CreateGenericLine<? extends Line_VersionStructure> lineRef,
      CreateJourneyPattern journeyPattern
    ) {
      super(id, lineRef, journeyPattern);
    }

    public VehicleJourneyRefStructure refObject() {
      return NetexEntitiesTestFactory.createServiceJourneyRef(id);
    }

    public ServiceJourney create() {
      ServiceJourney serviceJourney = new ServiceJourney()
        .withId(ref())
        .withLineRef(
          createJaxbElement(new LineRefStructure().withRef(line.ref()))
        )
        .withJourneyPatternRef(
          createJaxbElement(
            new JourneyPatternRefStructure().withRef(journeyPattern.ref())
          )
        );

      if (dayTypes != null) {
        serviceJourney.withDayTypes(
          new DayTypeRefs_RelStructure()
            .withDayTypeRef(
              dayTypes
                .stream()
                .map(JAXBUtils::createJaxbElement)
                .collect(Collectors.toCollection(ArrayList::new))
            )
        );
      }

      if (timetabledPassingTimes != null) {
        serviceJourney.withPassingTimes(
          new TimetabledPassingTimes_RelStructure()
            .withTimetabledPassingTime(
              timetabledPassingTimes
                .stream()
                .map(CreateTimetabledPassingTime::create)
                .toList()
            )
        );
      }

      if (transportMode != null) {
        serviceJourney.withTransportMode(transportMode);
      }

      if (transportSubmode != null) {
        serviceJourney.withTransportSubmode(transportSubmode);
      }

      return serviceJourney;
    }
  }

  public static class CreateServiceJourneyInterchange
    extends CreateEntity<ServiceJourneyInterchange> {

    private boolean guaranteed = true;
    private Duration maximumWaitTime;
    private ScheduledStopPointRefStructure fromPointRef;
    private ScheduledStopPointRefStructure toPointRef;
    private VehicleJourneyRefStructure fromJourneyRef;
    private VehicleJourneyRefStructure toJourneyRef;

    public CreateServiceJourneyInterchange(int id) {
      super(id);
    }

    public CreateServiceJourneyInterchange withGuaranteed(boolean guaranteed) {
      this.guaranteed = guaranteed;
      return this;
    }

    public CreateServiceJourneyInterchange withMaximumWaitTime(
      Duration maximumWaitTime
    ) {
      this.maximumWaitTime = maximumWaitTime;
      return this;
    }

    public CreateServiceJourneyInterchange withFromPointRef(
      ScheduledStopPointRefStructure fromPointRef
    ) {
      this.fromPointRef = fromPointRef;
      return this;
    }

    public CreateServiceJourneyInterchange withToPointRef(
      ScheduledStopPointRefStructure toPointRef
    ) {
      this.toPointRef = toPointRef;
      return this;
    }

    public CreateServiceJourneyInterchange withFromJourneyRef(
      VehicleJourneyRefStructure fromJourneyRef
    ) {
      this.fromJourneyRef = fromJourneyRef;
      return this;
    }

    public CreateServiceJourneyInterchange withToJourneyRef(
      VehicleJourneyRefStructure toJourneyRef
    ) {
      this.toJourneyRef = toJourneyRef;
      return this;
    }

    public ServiceJourneyInterchange create() {
      ServiceJourneyInterchange serviceJourneyInterchange =
        new ServiceJourneyInterchange()
          .withId(ref())
          .withGuaranteed(guaranteed)
          .withMaximumWaitTime(maximumWaitTime);

      if (fromPointRef != null) {
        serviceJourneyInterchange.withFromPointRef(fromPointRef);
      }

      if (toPointRef != null) {
        serviceJourneyInterchange.withToPointRef(toPointRef);
      }

      if (fromJourneyRef != null) {
        serviceJourneyInterchange.withFromJourneyRef(fromJourneyRef);
      }

      if (toJourneyRef != null) {
        serviceJourneyInterchange.withToJourneyRef(toJourneyRef);
      }

      return serviceJourneyInterchange;
    }
  }

  public static class CreateTimetabledPassingTime
    extends CreateEntity<TimetabledPassingTime> {

    private final CreateStopPointInJourneyPattern pointInJourneyPattern;
    private LocalTime departureTime;
    private LocalTime arrivalTime;
    private LocalTime earliestDepartureTime;
    private LocalTime latestArrivalTime;

    public CreateTimetabledPassingTime(
      int id,
      CreateStopPointInJourneyPattern pointInJourneyPattern
    ) {
      super(id);
      this.pointInJourneyPattern = pointInJourneyPattern;
    }

    public CreateTimetabledPassingTime withDepartureTime(
      LocalTime departureTime
    ) {
      this.departureTime = departureTime;
      return this;
    }

    public CreateTimetabledPassingTime withArrivalTime(LocalTime arrivalTime) {
      this.arrivalTime = arrivalTime;
      return this;
    }

    public CreateTimetabledPassingTime withEarliestDepartureTime(
      LocalTime earliestDepartureTime
    ) {
      this.earliestDepartureTime = earliestDepartureTime;
      return this;
    }

    public CreateTimetabledPassingTime withLatestArrivalTime(
      LocalTime latestArrivalTime
    ) {
      this.latestArrivalTime = latestArrivalTime;
      return this;
    }

    public TimetabledPassingTime create() {
      return new TimetabledPassingTime()
        .withId(ref())
        .withDepartureTime(departureTime)
        .withArrivalTime(arrivalTime)
        .withEarliestDepartureTime(earliestDepartureTime)
        .withLatestArrivalTime(latestArrivalTime)
        .withPointInJourneyPatternRef(
          createJaxbElement(
            new StopPointInJourneyPatternRefStructure()
              .withRef(pointInJourneyPattern.ref())
          )
        );
    }
  }

  public static class CreateServiceLink extends CreateEntity<ServiceLink> {

    private ScheduledStopPointRefStructure fromScheduledStopPointRef;
    private ScheduledStopPointRefStructure toScheduledStopPointRef;
    private LinkSequenceProjection_VersionStructure linkSequenceProjection_VersionStructure;

    public CreateServiceLink(int id) {
      super(id);
    }

    public CreateServiceLink withFromScheduledStopPointRef(
      ScheduledStopPointRefStructure fromScheduledStopPointRef
    ) {
      this.fromScheduledStopPointRef = fromScheduledStopPointRef;
      return this;
    }

    public CreateServiceLink withToScheduledStopPointRef(
      ScheduledStopPointRefStructure toScheduledStopPointRef
    ) {
      this.toScheduledStopPointRef = toScheduledStopPointRef;
      return this;
    }

    public CreateServiceLink withLineStringList(
      List<Double> lineStringPositions
    ) {
      this.linkSequenceProjection_VersionStructure =
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
      this.linkSequenceProjection_VersionStructure =
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
        .withId(ref())
        .withFromPointRef(fromScheduledStopPointRef)
        .withToPointRef(toScheduledStopPointRef)
        .withProjections(
          new Projections_RelStructure()
            .withProjectionRefOrProjection(
              createJaxbElement(linkSequenceProjection_VersionStructure)
            )
        );
    }
  }
}
