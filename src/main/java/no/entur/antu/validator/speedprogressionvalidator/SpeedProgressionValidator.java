package no.entur.antu.validator.speedprogressionvalidator;

import no.entur.antu.exception.AntuException;
import no.entur.antu.validator.ValidationContextWithNetexEntitiesIndex;
import no.entur.antu.validator.nonincreasingpassingtime.stoptime.SortedStopTimes;
import no.entur.antu.validator.nonincreasingpassingtime.stoptime.StopTime;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.*;
import org.entur.netex.validation.validator.xpath.ValidationContext;
import org.rutebanken.netex.model.ServiceJourney;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

public class SpeedProgressionValidator extends AbstractNetexValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpeedProgressionValidator.class);

    enum RuleCode {
        LOW_SPEED_PROGRESSION,
        HIGH_SPEED_PROGRESSION
    }

    public SpeedProgressionValidator(ValidationReportEntryFactory validationReportEntryFactory) {
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
            throw new AntuException("Received invalid validation context in Speed Progression validator");
        }
    }

    public void validateServiceJourney(ServiceJourney serviceJourney,
                                       NetexEntitiesIndex netexEntitiesIndex,
                                       BiConsumer<StopTime, RuleCode> validationError) {

        List<StopTime> sortedTimetabledPassingTime = SortedStopTimes.from(serviceJourney, netexEntitiesIndex);

        var previousPassingTime = sortedTimetabledPassingTime.get(0);
        if (!previousPassingTime.isComplete() || !previousPassingTime.isConsistent()) {
            return;
        }

        for (int i = 1; i < sortedTimetabledPassingTime.size(); i++) {
            var currentPassingTime = sortedTimetabledPassingTime.get(i);

            if (currentPassingTime.isComplete() || currentPassingTime.isConsistent() || previousPassingTime.isStopTimesIncreasing(currentPassingTime)) {
                // validate speed progression
                int diff = previousPassingTime.getStopTimeDiff(currentPassingTime);
                if (diff >= 0) {
//                    double distance = getDistance(stopArea0, stopArea1);

                }

            }

            previousPassingTime = currentPassingTime;
        }
    }
/*
    private double getDistance(StopArea stop1, StopArea stop2) {
        if (stop1 == null || stop2 == null) {
            return 0;  // Cannot compute distance when either stop is missing
        }

        String key = stop1.getObjectId() + "#" + stop2.getObjectId();

        if (distances.containsKey(key)) {
            return distances.get(key).doubleValue();
        }

        key = stop2.getObjectId() + "#" + stop1.getObjectId();

        if (distances.containsKey(key)) {
            return distances.get(key).doubleValue();
        }

        double distance = distance(stop1, stop2);

        distances.put(key, distance);

        return distance;
    }

    protected static double distance(NeptuneLocalizedObject obj1, NeptuneLocalizedObject obj2) {
        if (obj1.hasCoordinates() && obj2.hasCoordinates()) {
            return computeHaversineFormula(obj1, obj2);
        } else
            return 0;
    }

 */

    /**
     * @see http://mathforum.org/library/drmath/view/51879.html
     */
/*    private static double computeHaversineFormula(NeptuneLocalizedObject obj1, NeptuneLocalizedObject obj2) {

        double R = 6371008.8; // Earth radius
        double toRad = 0.017453292519943; // degree/rad ratio

        double lon1 = obj1.getLongitude().doubleValue() * toRad;
        double lat1 = obj1.getLatitude().doubleValue() * toRad;
        double lon2 = obj2.getLongitude().doubleValue() * toRad;
        double lat2 = obj2.getLatitude().doubleValue() * toRad;


        double dlon = Math.sin((lon2 - lon1) / 2);
        double dlat = Math.sin((lat2 - lat1) / 2);
        double a = (dlat * dlat) + Math.cos(lat1) * Math.cos(lat2)
                                   * (dlon * dlon);
        double c = 2. * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double d = R * c;
        return d;
    }
*/
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
                createRuleDescription(RuleCode.LOW_SPEED_PROGRESSION.toString(),
                        getValidationMessage(RuleCode.LOW_SPEED_PROGRESSION)),
                createRuleDescription(RuleCode.HIGH_SPEED_PROGRESSION.toString(),
                        getValidationMessage(RuleCode.HIGH_SPEED_PROGRESSION))
        );
    }

    private String getValidationMessage(RuleCode ruleCode) {
        return switch (ruleCode) {
            case LOW_SPEED_PROGRESSION -> "ServiceJourney has low speed progression";
            case HIGH_SPEED_PROGRESSION -> "ServiceJourney has high speed progression";
        };
    }
}
