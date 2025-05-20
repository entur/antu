package no.entur.antu.validation.validator.interchange.waitingtime;

import no.entur.antu.common.repository.TestNetexDataRepository;
import no.entur.antu.netextestdata.NetexEntitiesTestFactory;
import org.entur.netex.validation.validator.SimpleValidationEntryFactory;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.model.ScheduledStopPointId;
import org.entur.netex.validation.validator.model.ServiceJourneyId;
import org.entur.netex.validation.validator.model.ServiceJourneyInterchangeInfo;
import org.entur.netex.validation.validator.model.ServiceJourneyStop;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.*;

import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

class InterchangeWaitingTimeValidatorTest2 {

    private TestNetexDataRepository netexDataRepository;

    // Constants for test data identification
    private static final String CODESPACE = "codespace";
    private static final String TEST_CASE_NO_SHARED_DATE_WITH_UNSATISIFIED_WAITING_TIME = "idNoSharedActiveDateWithUnsatisifiedWaitingTime";
    private static final String TEST_CASE_NO_SHARED_DATE_WITH_SATISFIED_WAITING_TIME = "idNoSharedActiveDateWithSatisfiedWaitingTime";
    private static final String TEST_CASE_DAY_OFFSET_WITH_SATISFIED_WAITING_TIME = "idDayOffsetWithSatisfiedWaitingTime";
    private static final String TEST_CASE_DAY_OFFSET_WITH_UNSATISFIED_WAITING_TIME = "idDayOffsetWithUnsatisfiedWaitingTime";

    // Constants for service journey references
    private static final String SERVICE_JOURNEY_1 = "Test:ServiceJourney:1";
    private static final String SERVICE_JOURNEY_2 = "Test:ServiceJourney:2";
    private static final String SERVICE_JOURNEY_3 = "Test:ServiceJourney:3";
    private static final String SERVICE_JOURNEY_4 = "Test:ServiceJourney:4";
    private static final String SERVICE_JOURNEY_5 = "Test:ServiceJourney:5";
    private static final String SERVICE_JOURNEY_6 = "Test:ServiceJourney:6";
    private static final String SERVICE_JOURNEY_7 = "Test:ServiceJourney:7";
    private static final String SERVICE_JOURNEY_8 = "Test:ServiceJourney:8";

    // Constants for stop points
    private static final int STOP_POINT_1 = 1;
    private static final int STOP_POINT_2 = 2;
    private static final int STOP_POINT_3 = 3;
    private static final int STOP_POINT_4 = 4;
    private static final int STOP_POINT_5 = 5;
    private static final int STOP_POINT_6 = 6;
    private static final int STOP_POINT_7 = 7;
    private static final int STOP_POINT_8 = 8;

    @BeforeEach
    void setUp() {
        this.netexDataRepository = new TestNetexDataRepository();
    }

    private VehicleJourneyRefStructure createJourneyRef(String journeyId) {
        return new VehicleJourneyRefStructure()
                .withValue(ServiceJourneyId.ofValidId(journeyId).id());
    }

    private ScheduledStopPointRefStructure createStopPointRef(int stopPointId) {
        return new ScheduledStopPointRefStructure()
                .withRef(NetexEntitiesTestFactory.createScheduledStopPointRef(stopPointId).getRef());
    }

    private void setupTestCaseWithSatisifiedWaitingTime() {
        ServiceJourneyInterchange interchange = new ServiceJourneyInterchange()
                .withId("ServiceJourneyInterchange:2")
                .withMaximumWaitTime(Duration.ofHours(1))
                .withFromJourneyRef(new VehicleJourneyRefStructure().withRef(ServiceJourneyId.ofValidId(SERVICE_JOURNEY_3).id()))
                .withToJourneyRef(new VehicleJourneyRefStructure().withRef(ServiceJourneyId.ofValidId(SERVICE_JOURNEY_4).id()))
                .withFromPointRef(createStopPointRef(STOP_POINT_3))
                .withToPointRef(createStopPointRef(STOP_POINT_4));

        netexDataRepository.addServiceJourneyInterchangeInfo(
                TEST_CASE_NO_SHARED_DATE_WITH_SATISFIED_WAITING_TIME,
                ServiceJourneyInterchangeInfo.of("", interchange)
        );

        Map<ServiceJourneyId, List<LocalDateTime>> activeDatesMap = Map.ofEntries(
                Map.entry(
                        ServiceJourneyId.ofValidId(SERVICE_JOURNEY_3),
                        List.of(LocalDateTime.of(2025, 1, 1, 0, 0, 0))
                ),
                Map.entry(
                        ServiceJourneyId.ofValidId(SERVICE_JOURNEY_4),
                        List.of(LocalDateTime.of(2025, 1, 2, 0, 0, 0))
                )
        );

        netexDataRepository.putServiceJourneyIdToActiveDates(
                TEST_CASE_NO_SHARED_DATE_WITH_SATISFIED_WAITING_TIME,
                activeDatesMap
        );

        Map<ServiceJourneyId, List<ServiceJourneyStop>> journeyStopMap = Map.ofEntries(
                Map.entry(
                        ServiceJourneyId.ofValidId(SERVICE_JOURNEY_3),
                        List.of(createServiceJourneyArrivalStop(STOP_POINT_3, 23, 45, 0, Optional.empty()))
                ),
                Map.entry(
                        ServiceJourneyId.ofValidId(SERVICE_JOURNEY_4),
                        List.of(createServiceJourneyDepartureStop(STOP_POINT_4, 0, 15, 0, Optional.empty()))
                )
        );

        netexDataRepository.putServiceJourneyStop(
                TEST_CASE_NO_SHARED_DATE_WITH_SATISFIED_WAITING_TIME,
                journeyStopMap
        );
    }

    private void setupTestCaseWithUnsatisfiedWaitingTime() {
        ServiceJourneyInterchange interchange = new ServiceJourneyInterchange()
                .withId("ServiceJourneyInterchange:1")
                .withMaximumWaitTime(Duration.ZERO)
                .withFromJourneyRef(new VehicleJourneyRefStructure().withRef(ServiceJourneyId.ofValidId(SERVICE_JOURNEY_1).id()))
                .withToJourneyRef(new VehicleJourneyRefStructure().withRef(ServiceJourneyId.ofValidId(SERVICE_JOURNEY_2).id()))
                .withFromPointRef(createStopPointRef(STOP_POINT_1))
                .withToPointRef(createStopPointRef(STOP_POINT_2));

        netexDataRepository.addServiceJourneyInterchangeInfo(
                TEST_CASE_NO_SHARED_DATE_WITH_UNSATISIFIED_WAITING_TIME,
                ServiceJourneyInterchangeInfo.of("", interchange)
        );

        Map<ServiceJourneyId, List<LocalDateTime>> activeDatesMap = Map.ofEntries(
                Map.entry(
                        ServiceJourneyId.ofValidId(SERVICE_JOURNEY_1),
                        List.of(LocalDateTime.of(2025, 1, 1, 0, 0, 0))
                ),
                Map.entry(
                        ServiceJourneyId.ofValidId(SERVICE_JOURNEY_2),
                        List.of(LocalDateTime.of(2025, 1, 2, 0, 0, 0))
                )
        );

        netexDataRepository.putServiceJourneyIdToActiveDates(
                TEST_CASE_NO_SHARED_DATE_WITH_UNSATISIFIED_WAITING_TIME,
                activeDatesMap
        );

        Map<ServiceJourneyId, List<ServiceJourneyStop>> journeyStopMap = Map.ofEntries(
                Map.entry(
                        ServiceJourneyId.ofValidId(SERVICE_JOURNEY_1),
                        List.of(createServiceJourneyArrivalStop(STOP_POINT_1, 14, 0, 0, Optional.empty()))
                ),
                Map.entry(
                        ServiceJourneyId.ofValidId(SERVICE_JOURNEY_2),
                        List.of(createServiceJourneyDepartureStop(STOP_POINT_2, 14, 0, 0, Optional.empty()))
                )
        );

        netexDataRepository.putServiceJourneyStop(
                TEST_CASE_NO_SHARED_DATE_WITH_UNSATISIFIED_WAITING_TIME,
                journeyStopMap
        );
    }

    private void setupTestCaseWithDayOffsetAndSatisifiedWaitingTime() {
        ServiceJourneyInterchange interchange = new ServiceJourneyInterchange()
                .withId("ServiceJourneyInterchange:3")
                .withMaximumWaitTime(Duration.ofHours(1))
                .withFromJourneyRef(new VehicleJourneyRefStructure().withRef(ServiceJourneyId.ofValidId(SERVICE_JOURNEY_5).id()))
                .withToJourneyRef(new VehicleJourneyRefStructure().withRef(ServiceJourneyId.ofValidId(SERVICE_JOURNEY_6).id()))
                .withFromPointRef(createStopPointRef(STOP_POINT_5))
                .withToPointRef(createStopPointRef(STOP_POINT_6));

        netexDataRepository.addServiceJourneyInterchangeInfo(
                TEST_CASE_DAY_OFFSET_WITH_SATISFIED_WAITING_TIME,
                ServiceJourneyInterchangeInfo.of("", interchange)
        );

        Map<ServiceJourneyId, List<LocalDateTime>> activeDatesMap = Map.ofEntries(
                Map.entry(
                        ServiceJourneyId.ofValidId(SERVICE_JOURNEY_5),
                        List.of(LocalDateTime.of(2025, 1, 1, 0, 0, 0))
                ),
                Map.entry(
                        ServiceJourneyId.ofValidId(SERVICE_JOURNEY_6),
                        List.of(LocalDateTime.of(2025, 1, 1, 0, 0, 0))
                )
        );

        netexDataRepository.putServiceJourneyIdToActiveDates(
                TEST_CASE_DAY_OFFSET_WITH_SATISFIED_WAITING_TIME,
                activeDatesMap
        );

        Map<ServiceJourneyId, List<ServiceJourneyStop>> journeyStopMap = Map.ofEntries(
                Map.entry(
                        ServiceJourneyId.ofValidId(SERVICE_JOURNEY_5),
                        List.of(createServiceJourneyArrivalStop(STOP_POINT_5, 23, 45, 0, Optional.empty()))
                ),
                Map.entry(
                        ServiceJourneyId.ofValidId(SERVICE_JOURNEY_6),
                        List.of(createServiceJourneyDepartureStop(STOP_POINT_6, 0, 15, 0, Optional.of(1)))
                )
        );

        netexDataRepository.putServiceJourneyStop(
                TEST_CASE_DAY_OFFSET_WITH_SATISFIED_WAITING_TIME,
                journeyStopMap
        );
    }

    private void setupTestCaseWithDayOffsetAndUnsatisifiedWaitingTime() {
        ServiceJourneyInterchange interchange = new ServiceJourneyInterchange()
                .withId("ServiceJourneyInterchange:4")
                .withMaximumWaitTime(Duration.ofHours(1))
                .withFromJourneyRef(new VehicleJourneyRefStructure().withRef(ServiceJourneyId.ofValidId(SERVICE_JOURNEY_7).id()))
                .withToJourneyRef(new VehicleJourneyRefStructure().withRef(ServiceJourneyId.ofValidId(SERVICE_JOURNEY_8).id()))
                .withFromPointRef(createStopPointRef(STOP_POINT_7))
                .withToPointRef(createStopPointRef(STOP_POINT_8));

        netexDataRepository.addServiceJourneyInterchangeInfo(
                TEST_CASE_DAY_OFFSET_WITH_UNSATISFIED_WAITING_TIME,
                ServiceJourneyInterchangeInfo.of("", interchange)
        );

        Map<ServiceJourneyId, List<LocalDateTime>> activeDatesMap = Map.ofEntries(
                Map.entry(
                        ServiceJourneyId.ofValidId(SERVICE_JOURNEY_7),
                        List.of(LocalDateTime.of(2025, 1, 1, 0, 0, 0))
                ),
                Map.entry(
                        ServiceJourneyId.ofValidId(SERVICE_JOURNEY_8),
                        List.of(LocalDateTime.of(2025, 1, 1, 0, 0, 0))
                )
        );

        netexDataRepository.putServiceJourneyIdToActiveDates(
                TEST_CASE_DAY_OFFSET_WITH_UNSATISFIED_WAITING_TIME,
                activeDatesMap
        );

        Map<ServiceJourneyId, List<ServiceJourneyStop>> journeyStopMap = Map.ofEntries(
                Map.entry(
                        ServiceJourneyId.ofValidId(SERVICE_JOURNEY_7),
                        List.of(createServiceJourneyArrivalStop(STOP_POINT_7, 23, 45, 0, Optional.empty()))
                ),
                Map.entry(
                        ServiceJourneyId.ofValidId(SERVICE_JOURNEY_8),
                        List.of(createServiceJourneyDepartureStop(STOP_POINT_8, 0, 46, 0, Optional.of(1)))
                )
        );

        netexDataRepository.putServiceJourneyStop(
                TEST_CASE_DAY_OFFSET_WITH_UNSATISFIED_WAITING_TIME,
                journeyStopMap
        );
    }

    private ServiceJourneyStop createServiceJourneyArrivalStop(int stopPointId, int arrivalHour, int arrivalMinute, int arrivalSecond, Optional<Integer> dayOffset) {
        ScheduledStopPointId stopId = ScheduledStopPointId.of(
                NetexEntitiesTestFactory.createScheduledStopPointRef(stopPointId)
        );

        TimetabledPassingTime passingTime = new TimetabledPassingTime()
                .withArrivalTime(LocalTime.of(arrivalHour, arrivalMinute, arrivalSecond));

        if (dayOffset.isPresent()) {
            passingTime = passingTime.withArrivalDayOffset(BigInteger.valueOf(dayOffset.get()));
        }
        return ServiceJourneyStop.of(stopId, passingTime);
    }

    private ServiceJourneyStop createServiceJourneyDepartureStop(int stopPointId, int departureHour, int departureMinute, int departureSecond, Optional<Integer> dayOffset) {
        ScheduledStopPointId stopId = ScheduledStopPointId.of(
                NetexEntitiesTestFactory.createScheduledStopPointRef(stopPointId)
        );

        TimetabledPassingTime passingTime = new TimetabledPassingTime()
                .withDepartureTime(LocalTime.of(departureHour, departureMinute, departureSecond));

        if (dayOffset.isPresent()) {
            passingTime = passingTime.withDepartureDayOffset(BigInteger.valueOf(dayOffset.get()));
        }

        return ServiceJourneyStop.of(stopId, passingTime);
    }

    @Test
    void testNoSharedActiveDateWithUnsatisfiedWaitingTimeGivesValidationError() {
        setupTestCaseWithUnsatisfiedWaitingTime();
        InterchangeWaitingTimeValidator validator = new InterchangeWaitingTimeValidator(new SimpleValidationEntryFactory(), netexDataRepository);
        ValidationReport validationReport = new ValidationReport(CODESPACE, TEST_CASE_NO_SHARED_DATE_WITH_UNSATISIFIED_WAITING_TIME);
        ValidationReport resultingValidationReport = validator.validate(validationReport);
        assertEquals(1, resultingValidationReport.getValidationReportEntries().size());
    }

    @Test
    void testNoSharedActiveDateWithSatisfiedWaitingTimeGivesNoValidationError() {
        setupTestCaseWithSatisifiedWaitingTime();
        InterchangeWaitingTimeValidator validator = new InterchangeWaitingTimeValidator(new SimpleValidationEntryFactory(), netexDataRepository);
        ValidationReport validationReport = new ValidationReport(CODESPACE, TEST_CASE_NO_SHARED_DATE_WITH_SATISFIED_WAITING_TIME);
        ValidationReport resultingValidationReport = validator.validate(validationReport);
        assertTrue(resultingValidationReport.getValidationReportEntries().isEmpty());
    }

    @Test
    void testDayOffsetWithSatisfiedWaitingTimeGivesNoValidationError() {
        setupTestCaseWithDayOffsetAndSatisifiedWaitingTime();
        InterchangeWaitingTimeValidator validator = new InterchangeWaitingTimeValidator(new SimpleValidationEntryFactory(), netexDataRepository);
        ValidationReport validationReport = new ValidationReport(CODESPACE, TEST_CASE_DAY_OFFSET_WITH_SATISFIED_WAITING_TIME);
        ValidationReport resultingValidationReport = validator.validate(validationReport);
        assertTrue(resultingValidationReport.getValidationReportEntries().isEmpty());
    }

    @Test
    void testDayOffsetWithUnsatisfiedWaitingTimeGivesValidationError() {
        setupTestCaseWithDayOffsetAndUnsatisifiedWaitingTime();
        InterchangeWaitingTimeValidator validator = new InterchangeWaitingTimeValidator(new SimpleValidationEntryFactory(), netexDataRepository);
        ValidationReport validationReport = new ValidationReport(CODESPACE, TEST_CASE_DAY_OFFSET_WITH_UNSATISFIED_WAITING_TIME);
        ValidationReport resultingValidationReport = validator.validate(validationReport);
        assertEquals(1, resultingValidationReport.getValidationReportEntries().size());
    }
}