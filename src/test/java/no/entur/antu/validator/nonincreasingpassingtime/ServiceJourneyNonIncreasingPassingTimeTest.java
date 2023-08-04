package no.entur.antu.validator.nonincreasingpassingtime;

import no.entur.antu.validator.ValidationContextWithNetexEntitiesIndex;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.index.impl.NetexEntitiesIndexImpl;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.entur.netex.validation.validator.ValidationReportEntrySeverity;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.*;

import java.time.LocalTime;

import static no.entur.antu.validator.nonincreasingpassingtime.ServiceJourneyNonIncreasingPassingTime.RuleCode;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ServiceJourneyNonIncreasingPassingTimeTest {

    @Test
    void testValidateServiceJourneyWithRegularStop() {
        NetexTestDataSample sample = new NetexTestDataSample();
        ServiceJourney serviceJourney = sample.getServiceJourney();
        JourneyPattern journeyPattern = sample.getJourneyPattern();

        NetexEntitiesIndex netexEntitiesIndex = createNetexEntitiesIndex(journeyPattern, serviceJourney);

        ValidationReport validationReport = setupAndRunValidation(netexEntitiesIndex);

        assertThat(validationReport.getValidationReportEntries().size(), is(0));
    }

    @Test
    void testValidateServiceJourneyWithRegularStopMissingTime() {
        NetexTestDataSample sample = new NetexTestDataSample();
        ServiceJourney serviceJourney = sample.getServiceJourney();
        JourneyPattern journeyPattern = sample.getJourneyPattern();

        // remove arrival time and departure time
        TimetabledPassingTime timetabledPassingTime = getFirstPassingTime(serviceJourney);
        timetabledPassingTime.withArrivalTime(null).withDepartureTime(null);

        NetexEntitiesIndex netexEntitiesIndex = createNetexEntitiesIndex(journeyPattern, serviceJourney);

        ValidationReport validationReport = setupAndRunValidation(netexEntitiesIndex);

        assertThat(validationReport.getValidationReportEntries().size(), is(1));
        assertThat(validationReport.getNumberOfValidationEntriesPerRule()
                .containsKey(RuleCode.TIMETABLED_PASSING_TIME_INCOMPLETE_TIME.toString()), is(true));
    }

    @Test
    void testValidateServiceJourneyWithRegularStopInconsistentTime() {
        NetexTestDataSample sample = new NetexTestDataSample();
        ServiceJourney serviceJourney = sample.getServiceJourney();
        JourneyPattern journeyPattern = sample.getJourneyPattern();

        // set arrival time after departure time
        TimetabledPassingTime timetabledPassingTime = getFirstPassingTime(serviceJourney);
        timetabledPassingTime.withArrivalTime(timetabledPassingTime.getDepartureTime().plusMinutes(1));

        NetexEntitiesIndex netexEntitiesIndex = createNetexEntitiesIndex(journeyPattern, serviceJourney);
        ValidationReport validationReport = setupAndRunValidation(netexEntitiesIndex);

        assertThat(validationReport.getValidationReportEntries().size(), is(1));
        assertThat(validationReport.getNumberOfValidationEntriesPerRule()
                .containsKey(RuleCode.TIMETABLED_PASSING_TIME_INCONSISTENT_TIME.toString()), is(true));
    }

    @Test
    void testValidateServiceJourneyWithAreaStop() {
        NetexTestDataSample sample = new NetexTestDataSample();
        ServiceJourney serviceJourney = sample.getServiceJourney();
        JourneyPattern journeyPattern = sample.getJourneyPattern();

        // remove arrival time and departure time and add flex window
        getFirstPassingTime(serviceJourney)
                .withArrivalTime(null)
                .withDepartureTime(null)
                .withEarliestDepartureTime(LocalTime.MIDNIGHT)
                .withLatestArrivalTime(LocalTime.MIDNIGHT.plusMinutes(1));

        NetexEntitiesIndex netexEntitiesIndex = createNetexEntitiesIndex(journeyPattern, serviceJourney);

        netexEntitiesIndex.getFlexibleStopPlaceIdByStopPointRefIndex().put(getFirstScheduledStopPointRef(journeyPattern), "");

        ValidationReport validationReport = setupAndRunValidation(netexEntitiesIndex);

        assertThat(validationReport.getValidationReportEntries().size(), is(0));
    }

    @Test
    void testValidateServiceJourneyWithAreaStopMissingTimeWindow() {
        NetexTestDataSample sample = new NetexTestDataSample();
        ServiceJourney serviceJourney = sample.getServiceJourney();
        JourneyPattern journeyPattern = sample.getJourneyPattern();

        // remove arrival time and departure time and add flex window
        getFirstPassingTime(serviceJourney)
                .withArrivalTime(null)
                .withDepartureTime(null);

        NetexEntitiesIndex netexEntitiesIndex = createNetexEntitiesIndex(journeyPattern, serviceJourney);

        netexEntitiesIndex.getFlexibleStopPlaceIdByStopPointRefIndex().put(getFirstScheduledStopPointRef(journeyPattern), "");

        ValidationReport validationReport = setupAndRunValidation(netexEntitiesIndex);

        assertThat(validationReport.getValidationReportEntries().size(), is(1));
        assertThat(validationReport.getNumberOfValidationEntriesPerRule()
                .containsKey(RuleCode.TIMETABLED_PASSING_TIME_INCOMPLETE_TIME.toString()), is(true));
    }

    @Test
    void testValidateServiceJourneyWithAreaStopInconsistentTimeWindow() {
        NetexTestDataSample sample = new NetexTestDataSample();
        ServiceJourney serviceJourney = sample.getServiceJourney();
        JourneyPattern journeyPattern = sample.getJourneyPattern();

        // remove arrival time and departure time and add flex window
        getFirstPassingTime(serviceJourney)
                .withArrivalTime(null)
                .withDepartureTime(null)
                .withEarliestDepartureTime(LocalTime.MIDNIGHT.plusMinutes(1))
                .withLatestArrivalTime(LocalTime.MIDNIGHT);

        NetexEntitiesIndex netexEntitiesIndex = createNetexEntitiesIndex(journeyPattern, serviceJourney);

        netexEntitiesIndex.getFlexibleStopPlaceIdByStopPointRefIndex()
                .put(getFirstScheduledStopPointRef(journeyPattern), "");

        ValidationReport validationReport = setupAndRunValidation(netexEntitiesIndex);

        assertThat(validationReport.getValidationReportEntries().size(), is(1));
        assertThat(validationReport.getNumberOfValidationEntriesPerRule()
                .containsKey(RuleCode.TIMETABLED_PASSING_TIME_INCONSISTENT_TIME.toString()), is(true));
    }

    @Test
    void testValidateServiceJourneyWithRegularStopFollowedByRegularStopNonIncreasingTime() {
        NetexTestDataSample sample = new NetexTestDataSample();
        ServiceJourney serviceJourney = sample.getServiceJourney();
        JourneyPattern journeyPattern = sample.getJourneyPattern();

        // remove arrival time and departure time and add flex window on second stop
        TimetabledPassingTime firstPassingTime = getFirstPassingTime(serviceJourney);
        TimetabledPassingTime secondPassingTime = getSecondPassingTime(serviceJourney);
        secondPassingTime.withArrivalTime(firstPassingTime.getDepartureTime().minusMinutes(1));

        NetexEntitiesIndex netexEntitiesIndex = createNetexEntitiesIndex(journeyPattern, serviceJourney);

        ValidationReport validationReport = setupAndRunValidation(netexEntitiesIndex);

        assertThat(validationReport.getValidationReportEntries().size(), is(1));
        assertThat(validationReport.getNumberOfValidationEntriesPerRule()
                .containsKey(RuleCode.TIMETABLED_PASSING_TIME_NON_INCREASING_TIME.toString()), is(true));
    }

    /**
     * This test makes sure all passing times are complete and consistent, before it checks for
     * increasing times.
     */
    @Test
    void testValidateWithRegularStopFollowedByRegularStopWithMissingTime() {
        var sample = new NetexTestDataSample();
        var serviceJourney = sample.getServiceJourney();
        var journeyPattern = sample.getJourneyPattern();

        // Set arrivalTime AFTER departure time (not valid)
        getSecondPassingTime(serviceJourney)
                .withDepartureTime(null)
                .withArrivalTime(null);

        NetexEntitiesIndex netexEntitiesIndex = createNetexEntitiesIndex(journeyPattern, serviceJourney);

        ValidationReport validationReport = setupAndRunValidation(netexEntitiesIndex);

        assertThat(validationReport.getValidationReportEntries().size(), is(1));
        assertThat(validationReport.getNumberOfValidationEntriesPerRule()
                .containsKey(RuleCode.TIMETABLED_PASSING_TIME_INCOMPLETE_TIME.toString()), is(true));
    }

    @Test
    void testValidateServiceJourneyWithRegularStopFollowedByStopArea() {
        NetexTestDataSample sample = new NetexTestDataSample();
        ServiceJourney serviceJourney = sample.getServiceJourney();
        JourneyPattern journeyPattern = sample.getJourneyPattern();

        // remove arrival time and departure time and add flex window on second stop
        TimetabledPassingTime timetabledPassingTime = getSecondPassingTime(serviceJourney);
        timetabledPassingTime
                .withEarliestDepartureTime(timetabledPassingTime.getDepartureTime())
                .withLatestArrivalTime(timetabledPassingTime.getDepartureTime().plusMinutes(1))
                .withArrivalTime(null)
                .withDepartureTime(null);

        NetexEntitiesIndex netexEntitiesIndex = createNetexEntitiesIndex(journeyPattern, serviceJourney);

        netexEntitiesIndex.getFlexibleStopPlaceIdByStopPointRefIndex()
                .put(getSecondScheduledStopPointRef(journeyPattern), "");

        ValidationReport validationReport = setupAndRunValidation(netexEntitiesIndex);

        assertThat(validationReport.getValidationReportEntries().size(), is(0));
    }

    @Test
    void testValidateServiceJourneyWithRegularStopFollowedByStopAreaNonIncreasingTime() {
        NetexTestDataSample sample = new NetexTestDataSample();
        ServiceJourney serviceJourney = sample.getServiceJourney();
        JourneyPattern journeyPattern = sample.getJourneyPattern();

        // remove arrival time and departure time and add flex window with decreasing time on second stop
        TimetabledPassingTime firstPassingTime = getFirstPassingTime(serviceJourney);
        TimetabledPassingTime secondPassingTime = getSecondPassingTime(serviceJourney);
        secondPassingTime
                .withEarliestDepartureTime(firstPassingTime.getDepartureTime().minusMinutes(1))
                .withArrivalTime(null)
                .withDepartureTime(null);

        NetexEntitiesIndex netexEntitiesIndex = createNetexEntitiesIndex(journeyPattern, serviceJourney);

        netexEntitiesIndex.getFlexibleStopPlaceIdByStopPointRefIndex()
                .put(getFirstScheduledStopPointRef(journeyPattern), "");

        ValidationReport validationReport = setupAndRunValidation(netexEntitiesIndex);

        assertThat(validationReport.getValidationReportEntries().size(), is(1));
        assertThat(validationReport.getNumberOfValidationEntriesPerRule()
                .containsKey(RuleCode.TIMETABLED_PASSING_TIME_INCOMPLETE_TIME.toString()), is(true));
    }

    private ValidationReport setupAndRunValidation(NetexEntitiesIndex netexEntitiesIndex) {
        ServiceJourneyNonIncreasingPassingTime serviceJourneyNonIncreasingPassingTime =
                new ServiceJourneyNonIncreasingPassingTime(
                        (code, message, dataLocation) ->
                                new ValidationReportEntry(message, code, ValidationReportEntrySeverity.ERROR)
                );

        ValidationReport testValidationReport = new ValidationReport("TST", "Test1122");

        ValidationContextWithNetexEntitiesIndex validationContext = mock(ValidationContextWithNetexEntitiesIndex.class);
        when(validationContext.getNetexEntitiesIndex()).thenReturn(netexEntitiesIndex);

        serviceJourneyNonIncreasingPassingTime.validate(testValidationReport, validationContext);

        return testValidationReport;
    }

    private NetexEntitiesIndex createNetexEntitiesIndex(JourneyPattern journeyPattern,
                                                        ServiceJourney serviceJourney) {
        NetexEntitiesIndex netexEntitiesIndex = new NetexEntitiesIndexImpl();
        netexEntitiesIndex.getJourneyPatternIndex().put(journeyPattern.getId(), journeyPattern);

        netexEntitiesIndex.getTimetableFrames().add(
                new TimetableFrame()
                        .withVehicleJourneys(
                                new JourneysInFrame_RelStructure()
                                        .withId("JR:123")
                                        .withVehicleJourneyOrDatedVehicleJourneyOrNormalDatedVehicleJourney(serviceJourney)
                        )
        );

        return netexEntitiesIndex;
    }

    private TimetabledPassingTime getPassingTime(ServiceJourney serviceJourney, int order) {
        return serviceJourney.getPassingTimes().getTimetabledPassingTime().get(order);
    }

    private TimetabledPassingTime getFirstPassingTime(ServiceJourney serviceJourney) {
        return getPassingTime(serviceJourney, 0);
    }

    private TimetabledPassingTime getSecondPassingTime(ServiceJourney serviceJourney) {
        return getPassingTime(serviceJourney, 1);
    }

    private String getScheduledStopPointRef(JourneyPattern_VersionStructure journeyPattern, int order) {
        StopPointInJourneyPattern stopPointInJourneyPattern = (StopPointInJourneyPattern) journeyPattern
                .getPointsInSequence()
                .getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern()
                .get(order);

        return stopPointInJourneyPattern.getScheduledStopPointRef()
                .getValue()
                .getRef();
    }

    private String getFirstScheduledStopPointRef(JourneyPattern_VersionStructure journeyPattern) {
        return getScheduledStopPointRef(journeyPattern, 0);
    }

    private String getSecondScheduledStopPointRef(JourneyPattern_VersionStructure journeyPattern) {
        return getScheduledStopPointRef(journeyPattern, 1);
    }
}