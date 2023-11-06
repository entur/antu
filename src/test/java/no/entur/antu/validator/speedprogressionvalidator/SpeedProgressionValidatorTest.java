package no.entur.antu.validator.speedprogressionvalidator;

import no.entur.antu.commondata.CommonDataRepository;
import no.entur.antu.stop.StopPlaceRepository;
import no.entur.antu.model.QuayId;
import no.entur.antu.model.StopPlaceCoordinates;
import no.entur.antu.validator.ValidationContextWithNetexEntitiesIndex;
import no.entur.antu.validator.nonincreasingpassingtime.NetexTestDataSample;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.index.impl.NetexEntitiesIndexImpl;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.entur.netex.validation.validator.ValidationReportEntrySeverity;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.rutebanken.netex.model.*;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpeedProgressionValidatorTest {

    @Test
    void normalSpeedProgressionShouldNotReturnAnyValidationEntry() {
        NetexTestDataSample sample = new NetexTestDataSample();
        ServiceJourney serviceJourney = sample.getServiceJourney();
        JourneyPattern journeyPattern = sample.getJourneyPattern();

        serviceJourney.withTransportMode(AllVehicleModesOfTransportEnumeration.BUS);

        CommonDataRepository commonDataRepository = Mockito.mock(CommonDataRepository.class);
        StopPlaceRepository stopPlaceRepository = Mockito.mock(StopPlaceRepository.class);

        List<StopPlaceCoordinates> stopPlaceCoordinates = List.of(
                new StopPlaceCoordinates(6.621791, 60.424023),
                new StopPlaceCoordinates(6.612112, 60.471748),
                new StopPlaceCoordinates(6.622312, 60.481548),
                new StopPlaceCoordinates(6.632312, 60.491548)
        );

        for (int i = 0; i < 4; i++) {
            QuayId testQuayId = new QuayId("TST:Quay:" + (i + 1));

            Mockito.when(commonDataRepository.findQuayId(eq("RUT:ScheduledStopPoint:" + (i + 1)), anyString()))
                    .thenReturn(testQuayId);
            Mockito.when(stopPlaceRepository.getCoordinatesForQuayId(testQuayId))
                    .thenReturn(stopPlaceCoordinates.get(i));
        }

        NetexEntitiesIndex netexEntitiesIndex = createNetexEntitiesIndex(journeyPattern, serviceJourney);

        ValidationReport validationReport = setupAndRunValidation(
                netexEntitiesIndex, commonDataRepository, stopPlaceRepository
        );

        assertThat(validationReport.getValidationReportEntries().size(), is(0));
    }

    @Test
    void lowSpeedProgressionShouldReturnValidationEntryForLowSpeed() {
        NetexTestDataSample sample = new NetexTestDataSample();
        ServiceJourney serviceJourney = sample.getServiceJourney();
        JourneyPattern journeyPattern = sample.getJourneyPattern();

        serviceJourney.withTransportMode(AllVehicleModesOfTransportEnumeration.BUS);

        CommonDataRepository commonDataRepository = Mockito.mock(CommonDataRepository.class);
        StopPlaceRepository stopPlaceRepository = Mockito.mock(StopPlaceRepository.class);

        List<StopPlaceCoordinates> stopPlaceCoordinates = List.of(
                new StopPlaceCoordinates(6.621791, 60.424023),
                new StopPlaceCoordinates(6.612112, 60.471748),
                new StopPlaceCoordinates(6.612312, 60.471548),
                new StopPlaceCoordinates(6.632312, 60.491548)
        );

        for (int i = 0; i < 4; i++) {
            QuayId testQuayId = new QuayId("TST:Quay:" + (i + 1));

            Mockito.when(commonDataRepository.findQuayId(eq("RUT:ScheduledStopPoint:" + (i + 1)), anyString()))
                    .thenReturn(testQuayId);
            Mockito.when(stopPlaceRepository.getCoordinatesForQuayId(testQuayId))
                    .thenReturn(stopPlaceCoordinates.get(i));
        }

        NetexEntitiesIndex netexEntitiesIndex = createNetexEntitiesIndex(journeyPattern, serviceJourney);

        ValidationReport validationReport = setupAndRunValidation(
                netexEntitiesIndex, commonDataRepository, stopPlaceRepository
        );

        assertThat(validationReport.getValidationReportEntries().size(), is(1));
        assertThat(
                validationReport.getValidationReportEntries().stream().map(ValidationReportEntry::getName).findFirst(),
                is(SpeedProgressionValidator.SpeedProgressionRuleCode.LOW_SPEED_PROGRESSION.name())
        );
    }

    private static NetexEntitiesIndex createNetexEntitiesIndex(JourneyPattern journeyPattern,
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

    private static ValidationReport setupAndRunValidation(NetexEntitiesIndex netexEntitiesIndex,
                                                          CommonDataRepository commonDataRepository,
                                                          StopPlaceRepository stopPlaceRepository) {

        SpeedProgressionValidator speedProgressionValidator =
                new SpeedProgressionValidator(
                        (code, message, dataLocation) ->
                                new ValidationReportEntry(message, code, ValidationReportEntrySeverity.ERROR),
                        commonDataRepository, stopPlaceRepository);

        ValidationReport testValidationReport = new ValidationReport("TST", "Test1122");

        ValidationContextWithNetexEntitiesIndex validationContext = mock(ValidationContextWithNetexEntitiesIndex.class);
        when(validationContext.getNetexEntitiesIndex()).thenReturn(netexEntitiesIndex);

        speedProgressionValidator.validate(testValidationReport, validationContext);

        return testValidationReport;
    }
}