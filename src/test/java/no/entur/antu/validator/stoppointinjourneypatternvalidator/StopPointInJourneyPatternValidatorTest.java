package no.entur.antu.validator.stoppointinjourneypatternvalidator;

import no.entur.antu.commondata.CommonDataRepository;
import no.entur.antu.model.QuayId;
import no.entur.antu.validator.ValidationContextWithNetexEntitiesIndex;
import no.entur.antu.validator.nonincreasingpassingtime.NetexTestDataSample;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.index.impl.NetexEntitiesIndexImpl;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.entur.netex.validation.validator.ValidationReportEntrySeverity;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.JourneysInFrame_RelStructure;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.TimetableFrame;

import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StopPointInJourneyPatternValidatorTest {

    @Test
    void testAllStopPlaceAssignmentsExists() {
        NetexTestDataSample sample = new NetexTestDataSample();
        ServiceJourney serviceJourney = sample.getServiceJourney();
        JourneyPattern journeyPattern = sample.getJourneyPattern();

        NetexEntitiesIndex netexEntitiesIndex = createNetexEntitiesIndex(journeyPattern, serviceJourney);
        CommonDataRepository commonDataRepository = Mockito.mock(CommonDataRepository.class);

        IntStream.range(0, journeyPattern.getPointsInSequence().getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern().size())
                .forEach(index -> {
                    QuayId testQuayId = new QuayId("TST:Quay:" + (index + 1));
                    Mockito.when(commonDataRepository.findQuayIdForScheduledStopPoint(eq("RUT:ScheduledStopPoint:" + (index + 1)), anyString()))
                            .thenReturn(testQuayId);
                });

        ValidationReport validationReport = setupAndRunValidation(netexEntitiesIndex, commonDataRepository);

        assertThat(validationReport.getValidationReportEntries().size(), is(0));
    }

    @Test
    void testMissingStopPlaceAssignmentsButServiceJourneyExists() {
        NetexTestDataSample sample = new NetexTestDataSample();
        ServiceJourney serviceJourney = sample.getServiceJourney();
        JourneyPattern journeyPattern = sample.getJourneyPattern();

        NetexEntitiesIndex netexEntitiesIndex = createNetexEntitiesIndex(journeyPattern, serviceJourney);
        CommonDataRepository commonDataRepository = Mockito.mock(CommonDataRepository.class);

        IntStream.range(0, journeyPattern.getPointsInSequence().getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern().size() - 1)
                .forEach(index -> {
                    QuayId testQuayId = new QuayId("TST:Quay:" + (index + 1));
                    Mockito.when(commonDataRepository.findQuayIdForScheduledStopPoint(eq("RUT:ScheduledStopPoint:" + (index + 1)), anyString()))
                            .thenReturn(testQuayId);
                });

        ValidationReport validationReport = setupAndRunValidation(netexEntitiesIndex, commonDataRepository);

        assertThat(validationReport.getValidationReportEntries().size(), is(1));
    }

    @Test
    void testMissingSingleStopPlaceAssignmentsUsedInMultipleJourneyPatternsButServiceJourneyExists() {
        NetexTestDataSample sample = new NetexTestDataSample();
        ServiceJourney serviceJourney = sample.getServiceJourney();
        JourneyPattern journeyPattern = sample.getJourneyPattern();

        NetexEntitiesIndex netexEntitiesIndex = createNetexEntitiesIndex(journeyPattern, serviceJourney);
        CommonDataRepository commonDataRepository = Mockito.mock(CommonDataRepository.class);

        String scheduledStopPointRef = "RUT:ScheduledStopPoint:1234";
        IntStream.range(0, journeyPattern.getPointsInSequence().getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern().size())
                .forEach(index -> {
                    Mockito.when(commonDataRepository.findQuayIdForScheduledStopPoint(eq(scheduledStopPointRef), anyString()))
                            .thenReturn(null);
                });

        ValidationReport validationReport = setupAndRunValidation(netexEntitiesIndex, commonDataRepository);

        assertThat(validationReport.getValidationReportEntries().size(), is(2));
    }

    @Test
    void testMissingMultipleStopPlaceAssignmentsButServiceJourneyExists() {
        NetexTestDataSample sample = new NetexTestDataSample();
        ServiceJourney serviceJourney = sample.getServiceJourney();
        JourneyPattern journeyPattern = sample.getJourneyPattern();

        NetexEntitiesIndex netexEntitiesIndex = createNetexEntitiesIndex(journeyPattern, serviceJourney);
        CommonDataRepository commonDataRepository = Mockito.mock(CommonDataRepository.class);

        IntStream.range(0, journeyPattern.getPointsInSequence().getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern().size() - 2)
                .forEach(index -> {
                    QuayId testQuayId = new QuayId("TST:Quay:" + (index + 1));
                    Mockito.when(commonDataRepository.findQuayIdForScheduledStopPoint(eq("RUT:ScheduledStopPoint:" + (index + 1)), anyString()))
                            .thenReturn(testQuayId);
                });

        ValidationReport validationReport = setupAndRunValidation(netexEntitiesIndex, commonDataRepository);

        assertThat(validationReport.getValidationReportEntries().size(), is(2));
    }

    private static ValidationReport setupAndRunValidation(NetexEntitiesIndex netexEntitiesIndex,
                                                          CommonDataRepository commonDataRepository) {
        StopPointInJourneyPatternValidator nonIncreasingPassingTimeValidator =
                new StopPointInJourneyPatternValidator(
                        (code, message, dataLocation) ->
                                new ValidationReportEntry(message, code, ValidationReportEntrySeverity.ERROR),
                        commonDataRepository
                );

        ValidationReport testValidationReport = new ValidationReport("TST", "Test1122");

        ValidationContextWithNetexEntitiesIndex validationContext = mock(ValidationContextWithNetexEntitiesIndex.class);
        when(validationContext.getNetexEntitiesIndex()).thenReturn(netexEntitiesIndex);

        nonIncreasingPassingTimeValidator.validate(testValidationReport, validationContext);

        return testValidationReport;
    }

    private static NetexEntitiesIndex createNetexEntitiesIndex(JourneyPattern journeyPattern, ServiceJourney serviceJourney) {
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
}