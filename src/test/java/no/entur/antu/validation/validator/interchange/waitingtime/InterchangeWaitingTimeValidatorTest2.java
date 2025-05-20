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

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

class InterchangeWaitingTimeValidatorTest2 {

    private TestNetexDataRepository netexDataRepository;

    // Constants for test data identification
    private static final String CODESPACE = "codespace";
    private static final String TEST_CASE_NO_SHARED_DATE_WITH_UNSATISIFIED_WAITING_TIME = "idNoSharedActiveDateWithUnsatisifiedWaitingTime";
    private static final String TEST_CASE_NO_SHARED_DATE_WITH_SATISFIED_WAITING_TIME = "idNoSharedActiveDateWithSatisfiedWaitingTime";

    // Constants for service journey references
    private static final String SERVICE_JOURNEY_1 = "Test:ServiceJourney:1";
    private static final String SERVICE_JOURNEY_2 = "Test:ServiceJourney:2";
    private static final String SERVICE_JOURNEY_3 = "Test:ServiceJourney:3";
    private static final String SERVICE_JOURNEY_4 = "Test:ServiceJourney:4";

    // Constants for stop points
    private static final int STOP_POINT_1 = 1;
    private static final int STOP_POINT_2 = 2;
    private static final int STOP_POINT_3 = 3;
    private static final int STOP_POINT_4 = 4;

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
                        List.of(createServiceJourneyArrivalStop(STOP_POINT_3, 23, 45, 0))
                ),
                Map.entry(
                        ServiceJourneyId.ofValidId(SERVICE_JOURNEY_4),
                        List.of(createServiceJourneyDepartureStop(STOP_POINT_4, 0, 15, 0))
                )
        );

        netexDataRepository.putServiceJourneyStop(
                TEST_CASE_NO_SHARED_DATE_WITH_SATISFIED_WAITING_TIME,
                journeyStopMap
        );
    }

    private void setupTestCaseWithUnsatisfiedWaitingTime() {
        ServiceJourney serviceJourney = new ServiceJourney();
        serviceJourney.setId(SERVICE_JOURNEY_1);

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
                        List.of(createServiceJourneyArrivalStop(STOP_POINT_1, 14, 0, 0))
                ),
                Map.entry(
                        ServiceJourneyId.ofValidId(SERVICE_JOURNEY_2),
                        List.of(createServiceJourneyDepartureStop(STOP_POINT_2, 14, 0, 0))
                )
        );

        netexDataRepository.putServiceJourneyStop(
                TEST_CASE_NO_SHARED_DATE_WITH_UNSATISIFIED_WAITING_TIME,
                journeyStopMap
        );
    }

    private ServiceJourneyStop createServiceJourneyArrivalStop(int stopPointId, int arrivalHour, int arrivalMinute, int arrivalSecond) {
        ScheduledStopPointId stopId = ScheduledStopPointId.of(
                NetexEntitiesTestFactory.createScheduledStopPointRef(stopPointId)
        );

        TimetabledPassingTime passingTime = new TimetabledPassingTime()
                .withArrivalTime(LocalTime.of(arrivalHour, arrivalMinute, arrivalSecond));

        return ServiceJourneyStop.of(stopId, passingTime);
    }

    private ServiceJourneyStop createServiceJourneyDepartureStop(int stopPointId, int departureHour, int departureMinute, int departureSecond) {
        ScheduledStopPointId stopId = ScheduledStopPointId.of(
                NetexEntitiesTestFactory.createScheduledStopPointRef(stopPointId)
        );

        TimetabledPassingTime passingTime = new TimetabledPassingTime()
                .withDepartureTime(LocalTime.of(departureHour, departureMinute, departureSecond));

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
}