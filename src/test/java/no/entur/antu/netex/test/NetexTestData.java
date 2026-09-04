package no.entur.antu.netex.test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import no.entur.antu.netex.test.builder.DatedServiceJourneyBuilder;
import no.entur.antu.netex.test.builder.DeadRunBuilder;
import no.entur.antu.netex.test.builder.FlexibleLineBuilder;
import no.entur.antu.netex.test.builder.FlexibleStopPlaceBuilder;
import no.entur.antu.netex.test.builder.GenericLineBuilder;
import no.entur.antu.netex.test.builder.JourneyPatternBuilder;
import no.entur.antu.netex.test.builder.LineBuilder;
import no.entur.antu.netex.test.builder.OperatingDayBuilder;
import no.entur.antu.netex.test.builder.PassengerStopAssignmentBuilder;
import no.entur.antu.netex.test.builder.RouteBuilder;
import no.entur.antu.netex.test.builder.ServiceJourneyBuilder;
import no.entur.antu.netex.test.builder.ServiceJourneyInterchangeBuilder;
import no.entur.antu.netex.test.builder.ServiceLinkBuilder;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.index.impl.NetexEntitiesIndexImpl;
import org.rutebanken.netex.model.FlexibleStopPlace;
import org.rutebanken.netex.model.Line_VersionStructure;
import org.rutebanken.netex.model.PassengerStopAssignment;
import org.rutebanken.netex.model.ScheduledStopPointRefStructure;

/**
 * Test data for a JAXB validator: add the entities a test needs, then {@link #build()} the
 * NetexEntitiesIndex the validator consumes.
 *
 * <p>Every add* method mutates this container and returns a builder for the entity added, so a
 * test can keep configuring it. Nothing is built until build() is called, which means a test can
 * still adjust an entity after adding it.
 */
public class NetexTestData {

  private GenericLineBuilder<? extends Line_VersionStructure> line;
  private RouteBuilder route;

  private final List<JourneyPatternBuilder> journeyPatterns = new ArrayList<>();
  private final List<ServiceJourneyBuilder> serviceJourneys = new ArrayList<>();
  private final List<DatedServiceJourneyBuilder> datedServiceJourneys =
    new ArrayList<>();
  private final List<ServiceJourneyInterchangeBuilder> interchanges =
    new ArrayList<>();
  private final List<ServiceLinkBuilder> serviceLinks = new ArrayList<>();
  private final List<FlexibleStopPlaceBuilder> flexibleStopPlaces =
    new ArrayList<>();
  private final List<DeadRunBuilder> deadRuns = new ArrayList<>();
  private final List<PassengerStopAssignmentBuilder> passengerStopAssignments =
    new ArrayList<>();

  /**
   * Build the NetexEntitiesIndex for everything added so far.
   */
  public NetexEntitiesIndex build() {
    NetexEntitiesIndex netexEntitiesIndex = new NetexEntitiesIndexImpl();

    if (line != null) {
      line.registerInto(netexEntitiesIndex);
    }

    if (route != null) {
      netexEntitiesIndex.getRouteIndex().put(route.ref(), route.build());
    }

    fillIndexes(netexEntitiesIndex);
    return netexEntitiesIndex;
  }

  private void fillIndexes(NetexEntitiesIndex netexEntitiesIndex) {
    passengerStopAssignments
      .stream()
      .map(PassengerStopAssignmentBuilder::build)
      .forEach(passengerStopAssignment ->
        registerPassengerStopAssignment(
          netexEntitiesIndex,
          passengerStopAssignment
        )
      );

    journeyPatterns
      .stream()
      .map(JourneyPatternBuilder::build)
      .forEach(journeyPattern ->
        netexEntitiesIndex
          .getJourneyPatternIndex()
          .put(journeyPattern.getId(), journeyPattern)
      );

    interchanges
      .stream()
      .map(ServiceJourneyInterchangeBuilder::build)
      .forEach(interchange ->
        netexEntitiesIndex
          .getServiceJourneyInterchangeIndex()
          .put(interchange.getId(), interchange)
      );

    serviceJourneys
      .stream()
      .map(ServiceJourneyBuilder::build)
      .forEach(journey ->
        netexEntitiesIndex
          .getServiceJourneyIndex()
          .put(journey.getId(), journey)
      );

    datedServiceJourneys
      .stream()
      .map(DatedServiceJourneyBuilder::build)
      .forEach(journey ->
        netexEntitiesIndex
          .getDatedServiceJourneyIndex()
          .put(journey.getId(), journey)
      );

    deadRuns
      .stream()
      .map(DeadRunBuilder::build)
      .forEach(deadRun ->
        netexEntitiesIndex.getDeadRunIndex().put(deadRun.getId(), deadRun)
      );

    serviceLinks
      .stream()
      .map(ServiceLinkBuilder::build)
      .forEach(serviceLink ->
        netexEntitiesIndex
          .getServiceLinkIndex()
          .put(serviceLink.getId(), serviceLink)
      );

    flexibleStopPlaces.forEach(flexibleStopPlaceBuilder ->
      registerFlexibleStopPlace(netexEntitiesIndex, flexibleStopPlaceBuilder)
    );
  }

  private static void registerPassengerStopAssignment(
    NetexEntitiesIndex netexEntitiesIndex,
    PassengerStopAssignment passengerStopAssignment
  ) {
    String scheduledStopPointRef = passengerStopAssignment
      .getScheduledStopPointRef()
      .getValue()
      .getRef();

    netexEntitiesIndex
      .getPassengerStopAssignmentsByStopPointRefIndex()
      .put(scheduledStopPointRef, passengerStopAssignment);

    netexEntitiesIndex
      .getQuayIdByStopPointRefIndex()
      .put(
        scheduledStopPointRef,
        passengerStopAssignment.getQuayRef().getValue().getRef()
      );
  }

  private static void registerFlexibleStopPlace(
    NetexEntitiesIndex netexEntitiesIndex,
    FlexibleStopPlaceBuilder flexibleStopPlaceBuilder
  ) {
    FlexibleStopPlace flexibleStopPlace = flexibleStopPlaceBuilder.build();
    netexEntitiesIndex
      .getFlexibleStopPlaceIndex()
      .put(flexibleStopPlace.getId(), flexibleStopPlace);
    flexibleStopPlaceBuilder
      .scheduledStopPointRefs()
      .forEach(scheduledStopPointRef ->
        netexEntitiesIndex
          .getFlexibleStopPlaceIdByStopPointRefIndex()
          .put(scheduledStopPointRef, flexibleStopPlace.getId())
      );
  }

  /**
   * Adds a line with the given id, replacing any line added earlier.
   *
   * @param id the id of the line
   * @return LineBuilder
   */
  public LineBuilder addLine(int id) {
    LineBuilder lineBuilder = new LineBuilder(id);
    line = lineBuilder;
    return lineBuilder;
  }

  /**
   * Adds a line with id 1, replacing any line added earlier.
   *
   * @return LineBuilder
   */
  public LineBuilder addLine() {
    return addLine(1);
  }

  /**
   * Adds a flexible line with the given id, replacing any line added earlier.
   *
   * @param id the id of the line
   * @return FlexibleLineBuilder
   */
  public FlexibleLineBuilder addFlexibleLine(int id) {
    FlexibleLineBuilder flexibleLineBuilder = new FlexibleLineBuilder(id);
    line = flexibleLineBuilder;
    return flexibleLineBuilder;
  }

  /**
   * Adds a flexible line with id 1, replacing any line added earlier.
   *
   * @return FlexibleLineBuilder
   */
  public FlexibleLineBuilder addFlexibleLine() {
    return addFlexibleLine(1);
  }

  /**
   * Adds a route with the given id, replacing any route added earlier.
   *
   * @param id the id of the route
   * @return RouteBuilder
   */
  public RouteBuilder addRoute(int id) {
    route = new RouteBuilder(id, lineOrDefault());
    return route;
  }

  /**
   * Adds a route with id 1, replacing any route added earlier.
   *
   * @return RouteBuilder
   */
  public RouteBuilder addRoute() {
    return addRoute(1);
  }

  /**
   * Adds a journey pattern with the given id.
   *
   * @param id the id of the journey pattern
   * @return JourneyPatternBuilder
   */
  public JourneyPatternBuilder addJourneyPattern(int id) {
    JourneyPatternBuilder journeyPattern = new JourneyPatternBuilder(id);
    journeyPatterns.add(journeyPattern);
    return journeyPattern;
  }

  /**
   * Adds a journey pattern with id 1.
   *
   * @return JourneyPatternBuilder
   */
  public JourneyPatternBuilder addJourneyPattern() {
    return addJourneyPattern(1);
  }

  /**
   * Adds a service journey with the given id over the given journey pattern.
   *
   * @param id the id of the service journey
   * @param journeyPattern the journey pattern the service journey runs over
   * @return ServiceJourneyBuilder
   */
  public ServiceJourneyBuilder addServiceJourney(
    int id,
    JourneyPatternBuilder journeyPattern
  ) {
    ServiceJourneyBuilder serviceJourney = new ServiceJourneyBuilder(
      id,
      lineOrDefault(),
      journeyPattern
    );
    serviceJourneys.add(serviceJourney);
    return serviceJourney;
  }

  /**
   * Adds a service journey with id 1 over the given journey pattern.
   *
   * @param journeyPattern the journey pattern the service journey runs over
   * @return ServiceJourneyBuilder
   */
  public ServiceJourneyBuilder addServiceJourney(
    JourneyPatternBuilder journeyPattern
  ) {
    return addServiceJourney(1, journeyPattern);
  }

  /**
   * Adds numberOfServiceJourneys service journeys with ids 1..n over the given journey pattern.
   *
   * @param journeyPattern the journey pattern the service journeys run over
   * @param numberOfServiceJourneys the number of service journeys to add
   * @return the added builders, in order
   */
  public List<ServiceJourneyBuilder> addServiceJourneys(
    JourneyPatternBuilder journeyPattern,
    int numberOfServiceJourneys
  ) {
    GenericLineBuilder<? extends Line_VersionStructure> lineRef =
      lineOrDefault();
    List<ServiceJourneyBuilder> addedServiceJourneys = IntStream
      .rangeClosed(1, numberOfServiceJourneys)
      .mapToObj(index ->
        new ServiceJourneyBuilder(index, lineRef, journeyPattern)
      )
      .toList();
    serviceJourneys.addAll(addedServiceJourneys);
    return addedServiceJourneys;
  }

  /**
   * Adds a dead run with the given id over the given journey pattern.
   *
   * @param id the id of the dead run
   * @param journeyPattern the journey pattern the dead run runs over
   * @return DeadRunBuilder
   */
  public DeadRunBuilder addDeadRun(
    int id,
    JourneyPatternBuilder journeyPattern
  ) {
    DeadRunBuilder deadRun = new DeadRunBuilder(
      id,
      lineOrDefault(),
      journeyPattern
    );
    deadRuns.add(deadRun);
    return deadRun;
  }

  /**
   * Adds a dead run with id 1 over the given journey pattern.
   *
   * @param journeyPattern the journey pattern the dead run runs over
   * @return DeadRunBuilder
   */
  public DeadRunBuilder addDeadRun(JourneyPatternBuilder journeyPattern) {
    return addDeadRun(1, journeyPattern);
  }

  /**
   * Adds a dated service journey with the given id.
   *
   * @param id the id of the dated service journey
   * @param serviceJourneyRef the service journey it is dated from
   * @param operatingDayRef the operating day it runs on
   * @return DatedServiceJourneyBuilder
   */
  public DatedServiceJourneyBuilder addDatedServiceJourney(
    int id,
    ServiceJourneyBuilder serviceJourneyRef,
    OperatingDayBuilder operatingDayRef
  ) {
    DatedServiceJourneyBuilder datedServiceJourney =
      new DatedServiceJourneyBuilder(id, serviceJourneyRef, operatingDayRef);
    datedServiceJourneys.add(datedServiceJourney);
    return datedServiceJourney;
  }

  /**
   * Adds a dated service journey with id 1.
   *
   * @param serviceJourneyRef the service journey it is dated from
   * @param operatingDayRef the operating day it runs on
   * @return DatedServiceJourneyBuilder
   */
  public DatedServiceJourneyBuilder addDatedServiceJourney(
    ServiceJourneyBuilder serviceJourneyRef,
    OperatingDayBuilder operatingDayRef
  ) {
    return addDatedServiceJourney(1, serviceJourneyRef, operatingDayRef);
  }

  /**
   * Adds a service journey interchange with the given id.
   *
   * @param id the id of the service journey interchange
   * @return ServiceJourneyInterchangeBuilder
   */
  public ServiceJourneyInterchangeBuilder addServiceJourneyInterchange(int id) {
    ServiceJourneyInterchangeBuilder interchange =
      new ServiceJourneyInterchangeBuilder(id);
    interchanges.add(interchange);
    return interchange;
  }

  /**
   * Adds a service journey interchange with id 1.
   *
   * @return ServiceJourneyInterchangeBuilder
   */
  public ServiceJourneyInterchangeBuilder addServiceJourneyInterchange() {
    return addServiceJourneyInterchange(1);
  }

  /**
   * Adds a service link with the given id between two scheduled stop points.
   *
   * @param id the id of the service link
   * @return ServiceLinkBuilder
   */
  public ServiceLinkBuilder addServiceLink(
    int id,
    ScheduledStopPointRefStructure fromScheduledStopPointRef,
    ScheduledStopPointRefStructure toScheduledStopPointRef
  ) {
    ServiceLinkBuilder serviceLink = new ServiceLinkBuilder(id)
      .withFromScheduledStopPointRef(fromScheduledStopPointRef)
      .withToScheduledStopPointRef(toScheduledStopPointRef);
    serviceLinks.add(serviceLink);
    return serviceLink;
  }

  /**
   * Adds a service link with id 1 between two scheduled stop points.
   *
   * @return ServiceLinkBuilder
   */
  public ServiceLinkBuilder addServiceLink(
    ScheduledStopPointRefStructure fromScheduledStopPointRef,
    ScheduledStopPointRefStructure toScheduledStopPointRef
  ) {
    return addServiceLink(
      1,
      fromScheduledStopPointRef,
      toScheduledStopPointRef
    );
  }

  /**
   * Adds a flexible stop place with the given id.
   *
   * @param id the id of the flexible stop place
   * @return FlexibleStopPlaceBuilder
   */
  public FlexibleStopPlaceBuilder addFlexibleStopPlace(int id) {
    FlexibleStopPlaceBuilder flexibleStopPlace = new FlexibleStopPlaceBuilder(
      id
    );
    flexibleStopPlaces.add(flexibleStopPlace);
    return flexibleStopPlace;
  }

  /**
   * Adds a flexible stop place with id 1.
   *
   * @return FlexibleStopPlaceBuilder
   */
  public FlexibleStopPlaceBuilder addFlexibleStopPlace() {
    return addFlexibleStopPlace(1);
  }

  /**
   * Adds a passenger stop assignment with the given id.
   *
   * @param id the id of the passenger stop assignment
   * @return PassengerStopAssignmentBuilder
   */
  public PassengerStopAssignmentBuilder addPassengerStopAssignment(int id) {
    PassengerStopAssignmentBuilder passengerStopAssignment =
      new PassengerStopAssignmentBuilder(id);
    passengerStopAssignments.add(passengerStopAssignment);
    return passengerStopAssignment;
  }

  /**
   * Adds a passenger stop assignment with id 1.
   *
   * @return PassengerStopAssignmentBuilder
   */
  public PassengerStopAssignmentBuilder addPassengerStopAssignment() {
    return addPassengerStopAssignment(1);
  }

  /**
   * An operating day on the given date. Operating days are not registered in any index, so this
   * returns a builder without adding anything to this container.
   *
   * @param id the id of the operating day
   * @param date the calendar date
   * @return OperatingDayBuilder
   */
  public OperatingDayBuilder operatingDay(int id, LocalDate date) {
    return new OperatingDayBuilder(id, date);
  }

  /**
   * An operating day with id 1 on the given date.
   *
   * @param date the calendar date
   * @return OperatingDayBuilder
   */
  public OperatingDayBuilder operatingDay(LocalDate date) {
    return operatingDay(1, date);
  }

  /**
   * The line every journey and route hangs off.
   *
   * <p>A Line with id 1 is invented when a test has not added one, because a ServiceJourney
   * without a LineRef is not valid NeTEx and most tests do not care which line they are on. This
   * is a documented convenience, not an accident: see the follow-up issue on making it explicit.
   */
  private GenericLineBuilder<? extends Line_VersionStructure> lineOrDefault() {
    if (line == null) {
      line = new LineBuilder(1);
    }
    return line;
  }
}
