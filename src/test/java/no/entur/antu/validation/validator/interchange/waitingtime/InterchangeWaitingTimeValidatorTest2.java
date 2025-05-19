package no.entur.antu.validation.validator.interchange.waitingtime;

import no.entur.antu.AntuRouteBuilderIntegrationTestBase;
import no.entur.antu.TestApp;
import no.entur.antu.common.repository.TestNetexDataRepository;
import no.entur.antu.config.TestConfig;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

class InterchangeWaitingTimeValidatorTest2 {

    private TestNetexDataRepository netexDataRepository;

    // Constants for test data identification
    private static final String CODESPACE = "codespace";
    private static final String TEST_CASE_NO_SHARED_DATE_WITH_WAITING_TIME = "idNoSharedActiveDateWithUnsatisifiedWaitingTime";
    private static final String TEST_CASE_NO_SHARED_DATE_WITHOUT_WAITING_TIME = "idNoSharedActiveDateWithoutWaitingTime";

    // Constants for service journey references
    private static final String SERVICE_JOURNEY_1 = "Test:ServiceJourney:1";
    private static final String SERVICE_JOURNEY_2 = "Test:ServiceJourney:2";
    private static final String SERVICE_JOURNEY_3 = "Test:ServiceJourney:3";
    private static final String SERVICE_JOURNEY_4 = "Test:ServiceJourney:4";

    // Constants for stop points
    private static final int STOP_POINT_1 = 1;
    private static final int STOP_POINT_2 = 2;

    @BeforeEach
    void setUp() {
        this.netexDataRepository = new TestNetexDataRepository();
        setupServiceJourneyInterchange();
        setupTestCaseWithUnsatisfiedWaitingTime();
//        setupTestCaseWithoutWaitingTime();
        setupServiceJourneyStopData();
    }

    private void setupServiceJourneyInterchange() {
        // Create service journey interchange with zero maximum wait time
        ServiceJourney serviceJourney = new ServiceJourney();
        serviceJourney.setId(SERVICE_JOURNEY_1);

//        new VehicleJourneyRefStructure().withRef(ServiceJourneyId.ofValidId(SERVICE_JOURNEY_1).id());

        ServiceJourneyInterchange interchange = new ServiceJourneyInterchange()
                .withId("ServiceJourneyInterchange:1")
                .withMaximumWaitTime(Duration.ZERO)
                .withFromJourneyRef(new VehicleJourneyRefStructure().withRef(ServiceJourneyId.ofValidId(SERVICE_JOURNEY_1).id()))
                .withToJourneyRef(new VehicleJourneyRefStructure().withRef(ServiceJourneyId.ofValidId(SERVICE_JOURNEY_2).id()))
                .withFromPointRef(createStopPointRef(STOP_POINT_1))
                .withToPointRef(createStopPointRef(STOP_POINT_2));

        netexDataRepository.addServiceJourneyInterchangeInfo(
                ServiceJourneyInterchangeInfo.of("", interchange)
        );
    }

    private VehicleJourneyRefStructure createJourneyRef(String journeyId) {
        return new VehicleJourneyRefStructure()
                .withValue(ServiceJourneyId.ofValidId(journeyId).id());
    }

    private ScheduledStopPointRefStructure createStopPointRef(int stopPointId) {
        return new ScheduledStopPointRefStructure()
                .withRef(NetexEntitiesTestFactory.createScheduledStopPointRef(stopPointId).getRef());
    }

    private void setupTestCaseWithUnsatisfiedWaitingTime() {
        // Setup test case with journeys on different days (no shared active date)
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
                TEST_CASE_NO_SHARED_DATE_WITH_WAITING_TIME,
                activeDatesMap
        );
    }

    private void setupTestCaseWithoutWaitingTime() {
        // Setup test case with different journeys on different days
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
                TEST_CASE_NO_SHARED_DATE_WITH_WAITING_TIME,
                activeDatesMap
        );
    }

    private void setupServiceJourneyStopData() {
        // Setup service journey stop data with arrival times
        Map<ServiceJourneyId, List<ServiceJourneyStop>> journeyStopMap = Map.ofEntries(
                Map.entry(
                        ServiceJourneyId.ofValidId(SERVICE_JOURNEY_1),
                        List.of(createServiceJourneyStop(STOP_POINT_1, 14, 0, 0))
                ),
                Map.entry(
                        ServiceJourneyId.ofValidId(SERVICE_JOURNEY_2),
                        List.of(createServiceJourneyStop(STOP_POINT_2, 14, 0, 0))
                )
        );

        netexDataRepository.putServiceJourneyStop(
                TEST_CASE_NO_SHARED_DATE_WITH_WAITING_TIME,
                journeyStopMap
        );
    }

    private ServiceJourneyStop createServiceJourneyStop(int stopPointId, int hour, int minute, int second) {
        ScheduledStopPointId stopId = ScheduledStopPointId.of(
                NetexEntitiesTestFactory.createScheduledStopPointRef(stopPointId)
        );

        TimetabledPassingTime passingTime = new TimetabledPassingTime()
                .withArrivalTime(LocalTime.of(hour, minute, second));

        return ServiceJourneyStop.of(stopId, passingTime);
    }

    @Test
    void testNoSharedActiveDateWithUnsatisfiedWaitingTimeGivesValidationError() {
        InterchangeWaitingTimeValidator validator = new InterchangeWaitingTimeValidator(new SimpleValidationEntryFactory(), netexDataRepository);
        ValidationReport validationReport = new ValidationReport(CODESPACE, TEST_CASE_NO_SHARED_DATE_WITH_WAITING_TIME);
        ValidationReport resultingValidationReport = validator.validate(validationReport);
        assertEquals(2, resultingValidationReport.getValidationReportEntries().size());
    }
}