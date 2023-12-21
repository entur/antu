package no.entur.antu.validator.nonincreasingpassingtime;

import org.rutebanken.netex.model.*;

import javax.xml.bind.JAXBElement;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import static no.entur.antu.validator.nonincreasingpassingtime.MappingSupport.createJaxbElement;
import static no.entur.antu.validator.nonincreasingpassingtime.MappingSupport.createWrappedRef;

public class NetexTestData {

    private static final DayType EVERYDAY = new DayType()
            .withId("EVERYDAY")
            .withName(new MultilingualString().withValue("everyday"));
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

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

    public CreateDeadRun deadRun(JourneyPattern journeyPattern) {
        return new CreateDeadRun(journeyPattern);
    }

    // TODO: where should ths go??
    public DatedServiceJourney createDatedServiceJourney(LocalDate operatingDayDate) {

        OperatingDay operatingDay = new OperatingDay()
                .withId(operatingDayDate.format(DATE_FORMATTER))
                .withCalendarDate(operatingDayDate.atStartOfDay());

        return new DatedServiceJourney()
                .withId("RUT:DatedServiceJourney:1") //should refactor if we need multiple DatedServiceJourney
                .withServiceAlteration(ServiceAlterationEnumeration.PLANNED)
                .withOperatingDayRef(new OperatingDayRefStructure().withRef(operatingDay.getId()));
    }

    public List<JourneyPattern> createJourneyPatterns(int numberOfJourneyPatterns) {
        return IntStream.range(0, numberOfJourneyPatterns)
                .mapToObj(index -> new CreateJourneyPattern().withId(index).create())
                .toList();
    }

    public List<ServiceJourney> createServiceJourneys(JourneyPattern journeyPattern, int numberOfServiceJourneys) {
        return IntStream.range(0, numberOfServiceJourneys)
                .mapToObj(index -> new CreateServiceJourney(journeyPattern).withId(index).create())
                .toList();
    }

    public class CreateDeadRun {
        private int id = 1;

        private final JourneyPattern journeyPattern;

        public CreateDeadRun(JourneyPattern journeyPattern) {
            this.journeyPattern = journeyPattern;
        }

        public CreateDeadRun withId(int id) {
            this.id = id;
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
                                    .withTimetabledPassingTime(createTimetabledPassingTimes(
                                            journeyPattern.getPointsInSequence()
                                                    .getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern()
                                    ))
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

        public CreateLine withTransportMode(AllVehicleModesOfTransportEnumeration transportMode) {
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

    public class CreateServiceJourney {

        private int id = 1;
        private final JourneyPattern journeyPattern;

        public CreateServiceJourney(JourneyPattern journeyPattern) {
            this.journeyPattern = journeyPattern;
        }

        public CreateServiceJourney withId(int id) {
            this.id = id;
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
                                    .withTimetabledPassingTime(createTimetabledPassingTimes(
                                            journeyPattern.getPointsInSequence()
                                                    .getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern()
                                    ))
                    );
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

        public CreateJourneyPattern withNumberOfStopPointInJourneyPattern(int numberOfStopPointInJourneyPattern) {
            this.numberOfStopPointInJourneyPattern = numberOfStopPointInJourneyPattern;
            return this;
        }

        public JourneyPattern create() {
            RouteRefStructure routeRef = new RouteRefStructure().withRef("RUT:Route:" + routeId);

            List<PointInLinkSequence_VersionedChildStructure> pointsInLink = createPointsInLink(numberOfStopPointInJourneyPattern);
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

        private List<PointInLinkSequence_VersionedChildStructure> createPointsInLink(int numberOfStopPointInJourneyPattern) {

            String DESTINATION_DISPLAY_ID_1 = "NSR:DestinationDisplay:1";
            String DESTINATION_DISPLAY_ID_2 = "NSR:DestinationDisplay:2";

            DestinationDisplay destinationBergen = new DestinationDisplay()
                    .withId(DESTINATION_DISPLAY_ID_1)
                    .withVias(
                            new Vias_RelStructure()
                                    .withVia(List.of(this.createViaDestinationDisplayRef(DESTINATION_DISPLAY_ID_2)))
                    )
                    .withFrontText(new MultilingualString().withValue("Bergen"));

            DestinationDisplay destinationStavanger = new DestinationDisplay()
                    .withId(DESTINATION_DISPLAY_ID_2)
                    .withVias(
                            new Vias_RelStructure()
                                    .withVia(List.of(this.createViaDestinationDisplayRef(DESTINATION_DISPLAY_ID_1)))
                    )
                    .withFrontText(new MultilingualString().withValue("Stavanger"));

            return IntStream.range(0, numberOfStopPointInJourneyPattern)
                    .mapToObj(index -> {
                        String stopPointId = "RUT:StopPointInJourneyPattern:" + id + "_" + (index + 1);
                        StopPointInJourneyPattern stopPoint = new StopPointInJourneyPattern()
                                .withId(stopPointId)
                                .withOrder(BigInteger.valueOf(index + 1))
                                .withScheduledStopPointRef(createScheduledStopPointRef("RUT:ScheduledStopPoint:" + (index + 1)));

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

        private Via_VersionedChildStructure createViaDestinationDisplayRef(String destinationDisplayId) {
            return new Via_VersionedChildStructure()
                    .withDestinationDisplayRef(
                            new DestinationDisplayRefStructure().withRef(destinationDisplayId)
                    );
        }
    }

    private List<TimetabledPassingTime> createTimetabledPassingTimes(List<PointInLinkSequence_VersionedChildStructure> pointsInLink) {
        // Create timetable with 4 stops using the stopTimes above
        return IntStream.range(0, pointsInLink.size())
                .mapToObj(index ->
                        new TimetabledPassingTime()
                                .withId("TTPT-" + (index + 1))
                                .withDepartureTime(LocalTime.of(5, index * 5))
                                .withPointInJourneyPatternRef(createStopPointRef(pointsInLink.get(index).getId()))
                )
                .toList();
    }

    static DayTypeRefs_RelStructure createEveryDayRefs() {
        return new DayTypeRefs_RelStructure()
                .withDayTypeRef(Collections.singleton(createEveryDayRef()));
    }

    /* private static utility methods */
    private static JAXBElement<ScheduledStopPointRefStructure> createScheduledStopPointRef(
            String id
    ) {
        return createWrappedRef(id, ScheduledStopPointRefStructure.class);
    }

    private static JAXBElement<StopPointInJourneyPatternRefStructure> createStopPointRef(String id) {
        return createWrappedRef(id, StopPointInJourneyPatternRefStructure.class);
    }

    private static JAXBElement<JourneyPatternRefStructure> createJourneyPatternRef(String id) {
        return createWrappedRef(id, JourneyPatternRefStructure.class);
    }

    private static JAXBElement<DestinationDisplayRefStructure> createDestinationDisplayRef(String id) {
        return createWrappedRef(id, DestinationDisplayRefStructure.class);
    }

    private static JAXBElement<DayTypeRefStructure> createEveryDayRef() {
        return createJaxbElement(new DayTypeRefStructure().withRef(EVERYDAY.getId()));
    }
}
