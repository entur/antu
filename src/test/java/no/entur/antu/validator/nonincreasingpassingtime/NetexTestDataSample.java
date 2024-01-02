package no.entur.antu.validator.nonincreasingpassingtime;

import static no.entur.antu.validator.nonincreasingpassingtime.MappingSupport.createJaxbElement;
import static no.entur.antu.validator.nonincreasingpassingtime.MappingSupport.createWrappedRef;

import com.google.common.collect.ArrayListMultimap;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.bind.JAXBElement;
import org.rutebanken.netex.model.*;

public class NetexTestDataSample {

  public static final String SERVICE_JOURNEY_ID = "RUT:ServiceJourney:1";
  public static final List<String> DATED_SERVICE_JOURNEY_ID = List.of(
    "RUT:DatedServiceJourney:1",
    "RUT:DatedServiceJourney:2"
  );
  public static final List<String> OPERATING_DAYS = List.of(
    "2022-02-28",
    "2022-02-29"
  );
  private static final DayType EVERYDAY = new DayType()
    .withId("EVERYDAY")
    .withName(new MultilingualString().withValue("everyday"));
  private static final DateTimeFormatter DATE_FORMATTER =
    DateTimeFormatter.ofPattern("yyyy-MM-dd");

  private final JourneyPattern journeyPattern;

  private final ServiceJourney serviceJourney;
  private final List<TimetabledPassingTime> timetabledPassingTimes =
    new ArrayList<>();
  private final ArrayListMultimap<String, DatedServiceJourney> datedServiceJourneyBySjId =
    ArrayListMultimap.create();

  public NetexTestDataSample() {
    this(new int[] { 0, 4, 10, 15 });
  }

  public NetexTestDataSample(int[] stopTimes) {
    final int NUM_OF_STOPS = stopTimes.length;

    Line line = new Line()
      .withId("RUT:Line:1")
      .withName(new MultilingualString().withValue("Line 1"))
      .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS);

    JAXBElement<LineRefStructure> lineRef = createWrappedRef(
      line.getId(),
      LineRefStructure.class
    );

    // Add Netex Route (not the same as an OTP Route)
    String routeId = "RUT:Route:1";
    RouteRefStructure routeRef = new RouteRefStructure().withRef(routeId);

    // Create timetable with 4 stops using the stopTimes above
    for (int i = 0; i < NUM_OF_STOPS; i++) {
      timetabledPassingTimes.add(
        createTimetablePassingTime("TTPT-" + (i + 1), 5, stopTimes[i])
      );
    }

    final String DESTINATION_DISPLAY_ID_1 = "NSR:DestinationDisplay:1";
    final String DESTINATION_DISPLAY_ID_2 = "NSR:DestinationDisplay:2";

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

    List<PointInLinkSequence_VersionedChildStructure> pointsInLink =
      new ArrayList<>();

    for (int i = 0; i < NUM_OF_STOPS; i++) {
      String stopPointId = "RUT:StopPointInJourneyPattern:" + (i + 1);
      StopPointInJourneyPattern stopPoint = new StopPointInJourneyPattern()
        .withId(stopPointId)
        .withOrder(BigInteger.valueOf(i + 1))
        .withScheduledStopPointRef(
          createScheduledStopPointRef("RUT:ScheduledStopPoint:" + (i + 1))
        );

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

    // Create Journey Pattern with route and points
    journeyPattern =
      new JourneyPattern()
        .withId("RUT:JourneyPattern:1")
        .withRouteRef(routeRef)
        .withPointsInSequence(
          new PointsInJourneyPattern_RelStructure()
            .withPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern(
              pointsInLink
            )
        );

    // Create a new Service Journey with line, dayType, journeyPattern and timetable from above
    serviceJourney =
      new ServiceJourney()
        .withId(SERVICE_JOURNEY_ID)
        .withLineRef(lineRef)
        .withDayTypes(createEveryDayRefs())
        .withJourneyPatternRef(createJourneyPatternRef(journeyPattern.getId()))
        .withPassingTimes(
          new TimetabledPassingTimes_RelStructure()
            .withTimetabledPassingTime(timetabledPassingTimes)
        );

    for (int i = 0; i < DATED_SERVICE_JOURNEY_ID.size(); i++) {
      OperatingDay operatingDay = new OperatingDay()
        .withId(OPERATING_DAYS.get(i))
        .withCalendarDate(
          LocalDate.parse(OPERATING_DAYS.get(i), DATE_FORMATTER).atStartOfDay()
        );

      DatedServiceJourney datedServiceJourney = new DatedServiceJourney()
        .withId(DATED_SERVICE_JOURNEY_ID.get(i))
        .withServiceAlteration(ServiceAlterationEnumeration.PLANNED)
        .withOperatingDayRef(
          new OperatingDayRefStructure().withRef(operatingDay.getId())
        );

      datedServiceJourneyBySjId.put(SERVICE_JOURNEY_ID, datedServiceJourney);
    }
  }

  static DayTypeRefs_RelStructure createEveryDayRefs() {
    return new DayTypeRefs_RelStructure()
      .withDayTypeRef(Collections.singleton(createEveryDayRef()));
  }

  public JourneyPattern getJourneyPattern() {
    return journeyPattern;
  }

  List<TimetabledPassingTime> getTimetabledPassingTimes() {
    return timetabledPassingTimes;
  }

  public ServiceJourney getServiceJourney() {
    return serviceJourney;
  }

  ArrayListMultimap<String, DatedServiceJourney> getDatedServiceJourneyBySjId() {
    return datedServiceJourneyBySjId;
  }

  private static TimetabledPassingTime createTimetablePassingTime(
    String id,
    int hh,
    int mm
  ) {
    return new TimetabledPassingTime()
      .withId(id)
      .withDepartureTime(LocalTime.of(hh, mm));
  }

  /* private static utility methods */

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

  private Via_VersionedChildStructure createViaDestinationDisplayRef(
    String destinationDisplayId
  ) {
    return new Via_VersionedChildStructure()
      .withDestinationDisplayRef(
        new DestinationDisplayRefStructure().withRef(destinationDisplayId)
      );
  }
}
