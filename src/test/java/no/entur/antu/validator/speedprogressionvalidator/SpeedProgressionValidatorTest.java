package no.entur.antu.validator.speedprogressionvalidator;

import no.entur.antu.validator.ValidationContextWithNetexEntitiesIndex;
import no.entur.antu.validator.nonincreasingpassingtime.NetexTestDataSample;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.index.impl.NetexEntitiesIndexImpl;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.entur.netex.validation.validator.ValidationReportEntrySeverity;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpeedProgressionValidatorTest {

    @Test
    void testRun() {
        NetexTestDataSample sample = new NetexTestDataSample();
        ServiceJourney serviceJourney = sample.getServiceJourney();
        JourneyPattern journeyPattern = sample.getJourneyPattern();

        NetexEntitiesIndex netexEntitiesIndex = createNetexEntitiesIndex(journeyPattern, serviceJourney);

        ValidationReport validationReport = setupAndRunValidation(netexEntitiesIndex);

        assertThat(validationReport.getValidationReportEntries().size(), is(0));
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

    private static ValidationReport setupAndRunValidation(NetexEntitiesIndex netexEntitiesIndex) {
        SpeedProgressionValidator speedProgressionValidator =
                new SpeedProgressionValidator(
                        (code, message, dataLocation) ->
                                new ValidationReportEntry(message, code, ValidationReportEntrySeverity.ERROR)
                );

        ValidationReport testValidationReport = new ValidationReport("TST", "Test1122");

        ValidationContextWithNetexEntitiesIndex validationContext = mock(ValidationContextWithNetexEntitiesIndex.class);
        when(validationContext.getNetexEntitiesIndex()).thenReturn(netexEntitiesIndex);

        speedProgressionValidator.validate(testValidationReport, validationContext);

        return testValidationReport;
    }
}