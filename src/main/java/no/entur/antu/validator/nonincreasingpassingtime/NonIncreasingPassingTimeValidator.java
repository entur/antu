package no.entur.antu.validator.nonincreasingpassingtime;

import no.entur.antu.exception.AntuException;
import no.entur.antu.validator.ValidationContextWithNetexEntitiesIndex;
import no.entur.antu.stoptime.SortedStopTimes;
import no.entur.antu.stoptime.StopTime;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.*;
import org.entur.netex.validation.validator.xpath.ValidationContext;
import org.rutebanken.netex.model.ServiceJourney;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

public class NonIncreasingPassingTimeValidator extends AbstractNetexValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(NonIncreasingPassingTimeValidator.class);

    enum RuleCode {
        TIMETABLED_PASSING_TIME_INCOMPLETE_TIME,
        TIMETABLED_PASSING_TIME_INCONSISTENT_TIME,
        TIMETABLED_PASSING_TIME_NON_INCREASING_TIME
    }

    public NonIncreasingPassingTimeValidator(ValidationReportEntryFactory validationReportEntryFactory) {
        super(validationReportEntryFactory);
    }

    @Override
    public void validate(ValidationReport validationReport, ValidationContext validationContext) {

        LOGGER.debug("Validating ServiceJourney non-increasing passing time");

        if (validationContext.isCommonFile()) {
            return;
        }

        if (validationContext instanceof ValidationContextWithNetexEntitiesIndex validationContextWithNetexEntitiesIndex) {
            NetexEntitiesIndex index = validationContextWithNetexEntitiesIndex.getNetexEntitiesIndex();
            List<ServiceJourney> serviceJourneys =
                    index.getTimetableFrames().stream()
                            .flatMap(timetableFrame -> timetableFrame
                                    .getVehicleJourneys()
                                    .getVehicleJourneyOrDatedVehicleJourneyOrNormalDatedVehicleJourney().stream())
                            .filter(ServiceJourney.class::isInstance)
                            .map(ServiceJourney.class::cast)
                            .toList();


            serviceJourneys.forEach(serviceJourney -> validateServiceJourney(
                    serviceJourney,
                    index,
                    ((stopTime, validationCode) ->
                            addValidationReportEntry(
                                    validationReport, validationContext, serviceJourney, stopTime, validationCode
                            )
                    )
            ));

        } else {
            throw new AntuException("Received invalid validation context in " +
                                    "Validating ServiceJourney non-increasing passing time");
        }
    }

    public void validateServiceJourney(ServiceJourney serviceJourney,
                                       NetexEntitiesIndex netexEntitiesIndex,
                                       BiConsumer<StopTime, RuleCode> validationError) {

        List<StopTime> sortedTimetabledPassingTime = SortedStopTimes.from(serviceJourney, netexEntitiesIndex);

        var previousPassingTime = sortedTimetabledPassingTime.get(0);
        if (!previousPassingTime.isComplete()) {
            validationError.accept(previousPassingTime, RuleCode.TIMETABLED_PASSING_TIME_INCOMPLETE_TIME);
            return;
        }
        if (!previousPassingTime.isConsistent()) {
            validationError.accept(previousPassingTime, RuleCode.TIMETABLED_PASSING_TIME_INCONSISTENT_TIME);
            return;
        }

        for (int i = 1; i < sortedTimetabledPassingTime.size(); i++) {
            var currentPassingTime = sortedTimetabledPassingTime.get(i);

            if (!currentPassingTime.isComplete()) {
                validationError.accept(previousPassingTime, RuleCode.TIMETABLED_PASSING_TIME_INCOMPLETE_TIME);
                return;
            }
            if (!currentPassingTime.isConsistent()) {
                validationError.accept(previousPassingTime, RuleCode.TIMETABLED_PASSING_TIME_INCONSISTENT_TIME);
                return;
            }

            if (!previousPassingTime.isStopTimesIncreasing(currentPassingTime)) {
                validationError.accept(previousPassingTime, RuleCode.TIMETABLED_PASSING_TIME_NON_INCREASING_TIME);
                return;
            }

            previousPassingTime = currentPassingTime;
        }
    }

    private void addValidationReportEntry(ValidationReport validationReport,
                                          ValidationContext validationContext,
                                          ServiceJourney serviceJourney,
                                          StopTime stopTime,
                                          RuleCode ruleCode) {

        String fileName = validationContext.getFileName();
        ValidationReportEntry validationReportEntry = createValidationReportEntry(
                ruleCode.toString(),
                findDataLocation(validationContext, serviceJourney, fileName),
                String.format(
                        "%s. ServiceJourney = %s, TimetabledPassingTime = %s",
                        getValidationMessage(ruleCode),
                        serviceJourney.getId(),
                        stopTime.timetabledPassingTimeId()
                )
        );

        validationReport.addValidationReportEntry(validationReportEntry);
    }

    private static DataLocation findDataLocation(ValidationContext validationContext,
                                                 ServiceJourney serviceJourney,
                                                 String fileName) {
        return validationContext.getLocalIds().stream()
                .filter(localId -> localId.getId().equals(serviceJourney.getId()))
                .findFirst()
                .map(idVersion ->
                        new DataLocation(
                                idVersion.getId(),
                                fileName,
                                idVersion.getLineNumber(),
                                idVersion.getColumnNumber()
                        ))
                .orElse(new DataLocation(serviceJourney.getId(), fileName, 0, 0));
    }

    @Override
    public Set<String> getRuleDescriptions() {
        return Set.of(
                createRuleDescription(RuleCode.TIMETABLED_PASSING_TIME_INCOMPLETE_TIME.toString(),
                        getValidationMessage(RuleCode.TIMETABLED_PASSING_TIME_INCOMPLETE_TIME)),
                createRuleDescription(RuleCode.TIMETABLED_PASSING_TIME_INCONSISTENT_TIME.toString(),
                        getValidationMessage(RuleCode.TIMETABLED_PASSING_TIME_INCONSISTENT_TIME)),
                createRuleDescription(RuleCode.TIMETABLED_PASSING_TIME_NON_INCREASING_TIME.toString(),
                        getValidationMessage(RuleCode.TIMETABLED_PASSING_TIME_NON_INCREASING_TIME))
        );
    }

    private String getValidationMessage(RuleCode ruleCode) {
        return switch (ruleCode) {
            case TIMETABLED_PASSING_TIME_INCOMPLETE_TIME -> "ServiceJourney has incomplete TimetabledPassingTime";
            case TIMETABLED_PASSING_TIME_INCONSISTENT_TIME -> "ServiceJourney has inconsistent TimetabledPassingTime";
            case TIMETABLED_PASSING_TIME_NON_INCREASING_TIME ->
                    "ServiceJourney has non-increasing TimetabledPassingTime";
        };
    }
}
