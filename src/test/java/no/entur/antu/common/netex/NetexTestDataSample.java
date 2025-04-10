package no.entur.antu.common.netex;

import static org.entur.netex.validation.test.jaxb.support.JAXBUtils.createWrappedRef;

import jakarta.xml.bind.JAXBElement;
import java.math.BigInteger;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.namespace.QName;
import org.rutebanken.netex.model.*;

/**
 * This class is mostly a copy of this class in OpenTripPlanner:
 * https://github.com/opentripplanner/OpenTripPlanner/blob/master/application/src/test/java/org/opentripplanner/netex/mapping/NetexTestDataSample.java
 * */
public class NetexTestDataSample {

  private static final DayType EVERYDAY = new DayType();

  public static final String SERVICE_JOURNEY_ID = "RUT:ServiceJourney:1";
  static final String DESTINATION_DISPLAY_ID_1 = "NSR:DestinationDisplay:1";
  static final String DESTINATION_DISPLAY_ID_2 = "NSR:DestinationDisplay:2";
  static final String SCHEDULED_STOP_POINT_ID = "RUT:ScheduledStopPoint:1";
  static final String FLEXIBLE_STOP_PLACE_ID = "RUT:FlexibleStopPlace:1";
  static final String LINE_ID = "RUT:Line:1";
  static final String ROUTE_ID = "RUT:Route:1";

  private static JAXBElement<ScheduledStopPointRefStructure> createScheduledStopPointRef(
    String id
  ) {
    return createWrappedRef(id, ScheduledStopPointRefStructure.class);
  }

  public static JAXBElement<StopPointInJourneyPatternRefStructure> createStopPointRef(
    String id
  ) {
    return createWrappedRef(id, StopPointInJourneyPatternRefStructure.class);
  }

  private static JAXBElement<DestinationDisplayRefStructure> createDestinationDisplayRef(
    String id
  ) {
    return createWrappedRef(id, DestinationDisplayRefStructure.class);
  }

  @SuppressWarnings("unchecked")
  public static <T> JAXBElement<T> createJaxbElement(T value) {
    return new JAXBElement<>(
      new QName("x"),
      (Class<T>) value.getClass(),
      value
    );
  }

  private static JAXBElement<DayTypeRefStructure> createEveryDayRef() {
    return createJaxbElement(
      new DayTypeRefStructure().withRef(EVERYDAY.getId())
    );
  }

  public static Line defaultLine() {
    return new Line()
      .withId(LINE_ID)
      .withName(new MultilingualString().withValue("Line 1"))
      .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS);
  }

  static DayTypeRefs_RelStructure createEveryDayRefs() {
    return new DayTypeRefs_RelStructure()
      .withDayTypeRef(Collections.singleton(createEveryDayRef()));
  }

  private static JAXBElement<JourneyPatternRefStructure> createJourneyPatternRef(
    String id
  ) {
    return createWrappedRef(id, JourneyPatternRefStructure.class);
  }

  public static RouteRefStructure defaultRouteRef() {
    return new RouteRefStructure().withRef(ROUTE_ID);
  }

  private static Via_VersionedChildStructure createViaDestinationDisplayRef(
    String destinationDisplayId
  ) {
    return new Via_VersionedChildStructure()
      .withDestinationDisplayRef(
        new DestinationDisplayRefStructure().withRef(destinationDisplayId)
      );
  }

  private static Vias_RelStructure via(String destinationDisplayId) {
    return new Vias_RelStructure()
      .withVia(
        List.of(
          NetexTestDataSample.createViaDestinationDisplayRef(
            destinationDisplayId
          )
        )
      );
  }

  public static List<PointInLinkSequence_VersionedChildStructure> defaultPointsInLink() {
    DestinationDisplay destinationBergen = new DestinationDisplay()
      .withId(DESTINATION_DISPLAY_ID_1)
      .withVias(via(DESTINATION_DISPLAY_ID_1))
      .withFrontText(new MultilingualString().withValue("Bergen"));

    DestinationDisplay destinationStavanger = new DestinationDisplay()
      .withId(DESTINATION_DISPLAY_ID_2)
      .withVias(via(DESTINATION_DISPLAY_ID_1))
      .withFrontText(new MultilingualString().withValue("Stavanger"));

    List<TimetabledPassingTime> timetabledPassingTimes = NetexTestDataSample
      .defaultTimetabledPassingTimes()
      .getTimetabledPassingTime();
    List<PointInLinkSequence_VersionedChildStructure> pointsInLink =
      new ArrayList<>();
    for (int i = 0; i < 4; i++) {
      String stopPointId = "RUT:ScheduledStopPoint:" + (i + 1);
      StopPointInJourneyPattern stopPoint = new StopPointInJourneyPattern()
        .withId(stopPointId)
        .withOrder(BigInteger.valueOf(i + 1))
        .withScheduledStopPointRef(createScheduledStopPointRef(stopPointId));

      if (i == 0) stopPoint.setDestinationDisplayRef(
        createDestinationDisplayRef(destinationBergen.getId()).getValue()
      );
      if (i == 2) stopPoint.setDestinationDisplayRef(
        createDestinationDisplayRef(destinationStavanger.getId()).getValue()
      );

      pointsInLink.add(stopPoint);
      timetabledPassingTimes
        .get(i)
        .setPointInJourneyPatternRef(createStopPointRef(stopPointId));
    }
    return pointsInLink;
  }

  public static JourneyPattern defaultJourneyPattern() {
    RouteRefStructure routeRef = defaultRouteRef();
    return new JourneyPattern()
      .withId("RUT:JourneyPattern:1")
      .withRouteRef(routeRef)
      .withPointsInSequence(
        new PointsInJourneyPattern_RelStructure()
          .withPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern(
            defaultPointsInLink()
          )
      );
  }

  private static TimetabledPassingTime createTimetablePassingTime(
    String id,
    int hh,
    int mm,
    String stopPointId
  ) {
    return new TimetabledPassingTime()
      .withId(id)
      .withDepartureTime(LocalTime.of(hh, mm))
      .withPointInJourneyPatternRef(createStopPointRef(stopPointId));
  }

  public static TimetabledPassingTimes_RelStructure defaultTimetabledPassingTimes() {
    int[] stopTimes = { 0, 4, 10, 15 };
    int NUM_OF_STOPS = stopTimes.length;
    List<TimetabledPassingTime> timetabledPassingTimes = new ArrayList<>();
    for (int i = 0; i < NUM_OF_STOPS; i++) {
      timetabledPassingTimes.add(
        createTimetablePassingTime(
          "TTPT-" + (i + 1),
          5,
          stopTimes[i],
          "RUT:ScheduledStopPoint:" + (i + 1)
        )
      );
    }
    return new TimetabledPassingTimes_RelStructure()
      .withTimetabledPassingTime(timetabledPassingTimes);
  }

  public static TimetabledPassingTimes_RelStructure defaultTimetabledPassingTime(
    String stopPointInJourneyPatternRefId
  ) {
    return new TimetabledPassingTimes_RelStructure()
      .withTimetabledPassingTime(
        new TimetabledPassingTime()
          .withPointInJourneyPatternRef(
            createStopPointRef(stopPointInJourneyPatternRefId)
          )
      );
  }

  public static ServiceJourney defaultServiceJourney() {
    Line line = defaultLine();
    JourneyPattern journeyPattern = defaultJourneyPattern();
    return new ServiceJourney()
      .withId(SERVICE_JOURNEY_ID)
      .withLineRef(createWrappedRef(line.getId(), LineRefStructure.class))
      .withDayTypes(createEveryDayRefs())
      .withJourneyPatternRef(createJourneyPatternRef(journeyPattern.getId()))
      .withPassingTimes(defaultTimetabledPassingTimes());
  }

  private static TimetabledPassingTime getPassingTime(
    ServiceJourney serviceJourney,
    int order
  ) {
    return serviceJourney
      .getPassingTimes()
      .getTimetabledPassingTime()
      .get(order);
  }

  public static TimetabledPassingTime getFirstPassingTime(
    ServiceJourney serviceJourney
  ) {
    return getPassingTime(serviceJourney, 0);
  }

  public static TimetabledPassingTime getSecondPassingTime(
    ServiceJourney serviceJourney
  ) {
    return getPassingTime(serviceJourney, 1);
  }

  public static TimetabledPassingTimes_RelStructure defaultFlexibleTimetabledPassingTime(
    String stopPointInJourneyPatternRefId
  ) {
    return new TimetabledPassingTimes_RelStructure()
      .withTimetabledPassingTime(
        new TimetabledPassingTime()
          .withPointInJourneyPatternRef(
            createStopPointRef(stopPointInJourneyPatternRefId)
          )
          .withEarliestDepartureTime(LocalTime.MIDNIGHT)
          .withLatestArrivalTime(LocalTime.MIDNIGHT)
      );
  }

  public static ServiceJourney serviceJourneyWithFlexibleTimetabledPassingTime() {
    return defaultServiceJourney()
      .withPassingTimes(
        defaultFlexibleTimetabledPassingTime(SCHEDULED_STOP_POINT_ID)
      );
  }

  public static ServiceJourney serviceJourneyWithIncompletePassingTimeForFlexibleStop() {
    TimetabledPassingTime timetabledPassingTime = defaultTimetabledPassingTime(
      SCHEDULED_STOP_POINT_ID
    )
      .getTimetabledPassingTime()
      .get(0)
      .withLatestArrivalTime(null)
      .withEarliestDepartureTime(LocalTime.MIDNIGHT);
    return defaultServiceJourney()
      .withPassingTimes(
        new TimetabledPassingTimes_RelStructure()
          .withTimetabledPassingTime(timetabledPassingTime)
      );
  }

  public static ServiceJourney serviceJourneyWithInconsistentPassingTimeForFlexibleStop() {
    TimetabledPassingTime timetabledPassingTime = defaultTimetabledPassingTime(
      SCHEDULED_STOP_POINT_ID
    )
      .getTimetabledPassingTime()
      .get(0)
      .withEarliestDepartureTime(LocalTime.MIDNIGHT.plusMinutes(1))
      .withLatestArrivalTime(LocalTime.MIDNIGHT);
    return defaultServiceJourney()
      .withPassingTimes(
        new TimetabledPassingTimes_RelStructure()
          .withTimetabledPassingTime(timetabledPassingTime)
      );
  }

  public static FlexibleStopPlace defaultFlexibleStopPlace() {
    return new FlexibleStopPlace().withId(FLEXIBLE_STOP_PLACE_ID);
  }

  public static ServiceJourney serviceJourneyWithIncompletePassingTimesForRegularStop() {
    return defaultServiceJourney()
      .withPassingTimes(defaultTimetabledPassingTime(SCHEDULED_STOP_POINT_ID));
  }

  public static ServiceJourney serviceJourneyWithInconsistentPassingTimesForRegularStop() {
    TimetabledPassingTime timetabledPassingTime = defaultTimetabledPassingTime(
      SCHEDULED_STOP_POINT_ID
    )
      .getTimetabledPassingTime()
      .get(0)
      .withDepartureTime(LocalTime.of(12, 12))
      .withArrivalTime(LocalTime.of(13, 13));
    TimetabledPassingTime inconsistentPassingTime =
      timetabledPassingTime.withArrivalTime(
        timetabledPassingTime.getDepartureTime().plusMinutes(1)
      );
    return defaultServiceJourney()
      .withPassingTimes(
        new TimetabledPassingTimes_RelStructure()
          .withTimetabledPassingTime(inconsistentPassingTime)
      );
  }
}
