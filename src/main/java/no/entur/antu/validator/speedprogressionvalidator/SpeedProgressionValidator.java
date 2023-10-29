package no.entur.antu.validator.speedprogressionvalidator;

import no.entur.antu.commondata.CommonDataRepository;
import no.entur.antu.exception.AntuException;
import no.entur.antu.stop.StopPlaceRepository;
import no.entur.antu.stop.model.QuayId;
import no.entur.antu.stop.model.StopPlaceCoordinates;
import no.entur.antu.validator.ValidationContextWithNetexEntitiesIndex;
import no.entur.antu.validator.nonincreasingpassingtime.stoptime.SortedStopTimes;
import no.entur.antu.validator.nonincreasingpassingtime.stoptime.StopTime;
import org.apache.logging.log4j.util.TriConsumer;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.*;
import org.entur.netex.validation.validator.xpath.ValidationContext;
import org.rutebanken.netex.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class SpeedProgressionValidator extends AbstractNetexValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpeedProgressionValidator.class);
    private final CommonDataRepository commonDataRepository;
    private final StopPlaceRepository stopPlaceRepository;

    record Context(
            ServiceJourney serviceJourney,
            AllVehicleModesOfTransportEnumeration transportMode,
            Map<String, StopPlaceCoordinates> stopPlaceCoordinatesPerTimetabledPassingTimeId) {
    }

    record ErrorContext(
            RuleCode ruleCode,
            String exceptedSpeed,
            String calculatedSpeed,
            ServiceJourney serviceJourney,
            StopTime from,
            StopTime to) {
    }

    enum RuleCode {
        LOW_SPEED_PROGRESSION,
        HIGH_SPEED_PROGRESSION,
        WARNING_SPEED_PROGRESSION
    }

    public SpeedProgressionValidator(ValidationReportEntryFactory validationReportEntryFactory,
                                     CommonDataRepository commonDataRepository,
                                     StopPlaceRepository stopPlaceRepository) {
        super(validationReportEntryFactory);
        this.commonDataRepository = commonDataRepository;
        this.stopPlaceRepository = stopPlaceRepository;
    }

    @Override
    public void validate(ValidationReport validationReport, ValidationContext validationContext) {

        LOGGER.debug("Validating Speed progression");

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

            serviceJourneys.stream()
                    .map(serviceJourney ->
                            createContext(
                                    index,
                                    serviceJourney,
                                    validationReport.getValidationReportId()
                            ))
                    .forEach(context -> validateServiceJourney(
                                    context,
                                    index,
                                    errorContext ->
                                            addValidationReportEntry(
                                                    validationReport,
                                                    validationContext,
                                                    errorContext
                                            )
                            )
                    );

        } else {
            throw new AntuException(
                    "Received invalid validation context in Speed Progression validator"
            );
        }
    }

    public void validateServiceJourney(Context context,
                                       NetexEntitiesIndex netexEntitiesIndex,
                                       Consumer<ErrorContext> validationError) {

        List<StopTime> sortedTimetabledPassingTime =
                SortedStopTimes.from(context.serviceJourney(), netexEntitiesIndex);

        DistanceCalculator distanceCalculator = new DistanceCalculator();
        for (int i = 1; i < sortedTimetabledPassingTime.size(); i++) {
            var previousPassingTime = sortedTimetabledPassingTime.get(i - 1);
            if (!previousPassingTime.isComplete() || !previousPassingTime.isConsistent()) {
                return;
            }
            var currentPassingTime = sortedTimetabledPassingTime.get(i);

            if (currentPassingTime.isComplete()
                || currentPassingTime.isConsistent()
                || previousPassingTime.isStopTimesIncreasing(currentPassingTime)) {

                // validate speed progression
                int stopTimeDiff = previousPassingTime.getStopTimeDiff(currentPassingTime);
                if (stopTimeDiff >= 0) {
                    StopPlaceCoordinates previousStopPlaceCoordinates =
                            context.stopPlaceCoordinatesPerTimetabledPassingTimeId()
                                    .get(previousPassingTime.timetabledPassingTimeId());

                    StopPlaceCoordinates currentStopPlaceCoordinates =
                            context.stopPlaceCoordinatesPerTimetabledPassingTimeId()
                                    .get(currentPassingTime.timetabledPassingTimeId());

                    if (previousStopPlaceCoordinates == null || currentStopPlaceCoordinates == null) {
                        // TODO: What if current is null, next iteration the current will be previous,
                        //  then it will be null again, should handle it intelligently.
                        continue;
                    }

                    double distance = distanceCalculator.calculateDistance(
                            previousPassingTime.timetabledPassingTimeId(),
                            previousStopPlaceCoordinates,
                            currentPassingTime.timetabledPassingTimeId(),
                            currentStopPlaceCoordinates
                    );

                    if (distance < 1) {
                        // superimposed stops, speed not calculable
                        continue;
                    }
                    validateDistance(
                            distance,
                            stopTimeDiff,
                            previousPassingTime.isDepartureInMinutesResolution()
                            && currentPassingTime.isArrivalInMinutesResolution(),
                            TransportModeParameters.of(context.transportMode()),
                            (ruleCode, expectedSpeed, calculatedSpeed) -> validationError.accept(
                                    new ErrorContext(
                                            ruleCode,
                                            expectedSpeed,
                                            calculatedSpeed,
                                            context.serviceJourney(),
                                            previousPassingTime,
                                            currentPassingTime
                                    )
                            )
                    );
                }
            }
        }
    }

    private void validateDistance(double distance,
                                  int stopTimeDiff,
                                  boolean hasMinutesResolution,
                                  TransportModeParameters transportModeParameters,
                                  TriConsumer<RuleCode, String, String> speedError) {

        // Times are often with minute resolution.
        // Assume max error (120 sec) when comparing with min and max allowed speed.

        double minPossibleDiffTime = hasMinutesResolution ? Math.max(stopTimeDiff - 120, 1) : stopTimeDiff;
        double maxPossibleDiffTime = hasMinutesResolution ? stopTimeDiff + 120 : stopTimeDiff;
        double optimisticSpeed = distance / minPossibleDiffTime * 36 / 10; // (km/h)
        double pessimisticSpeed = distance / maxPossibleDiffTime * 36 / 10; // (km/h)

        if (optimisticSpeed < transportModeParameters.minSpeed()) {
            // too slow
            String calculatedSpeed = Integer.toString((int) optimisticSpeed);
            speedError.accept(
                    RuleCode.LOW_SPEED_PROGRESSION,
                    Integer.toString((int) transportModeParameters.minSpeed()),
                    calculatedSpeed);
        } else if (pessimisticSpeed > transportModeParameters.warningSpeed()) {
            // too fast
            String calculatedSpeed = Integer.toString((int) pessimisticSpeed);
            if (pessimisticSpeed > transportModeParameters.maxSpeed()) {
                speedError.accept(
                        RuleCode.HIGH_SPEED_PROGRESSION,
                        Integer.toString((int) transportModeParameters.maxSpeed()),
                        calculatedSpeed);
            } else {
                speedError.accept(
                        RuleCode.WARNING_SPEED_PROGRESSION,
                        Integer.toString((int) transportModeParameters.warningSpeed()),
                        calculatedSpeed);
            }
        }
    }

    private void addValidationReportEntry(ValidationReport validationReport,
                                          ValidationContext validationContext,
                                          ErrorContext errorContext) {

        String fileName = validationContext.getFileName();
        ValidationReportEntry validationReportEntry = createValidationReportEntry(
                errorContext.ruleCode().toString(),
                findDataLocation(validationContext, errorContext.serviceJourney(), fileName),
                // TODO: update description to include the expected and calculated speed
                String.format(
                        "%s. ServiceJourney = %s, " +
                        "from TimetabledPassingTime = %s, " +
                        "to TimetabledPassingTime = %s",
                        getValidationMessage(errorContext.ruleCode()),
                        errorContext.serviceJourney().getId(),
                        errorContext.from().timetabledPassingTimeId(),
                        errorContext.to().timetabledPassingTimeId()
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

    private Context createContext(NetexEntitiesIndex index, ServiceJourney serviceJourney, String validationReportId) {
        String journeyPatternRef = serviceJourney.getJourneyPatternRef().getValue().getRef();
        Map<String, StopPlaceCoordinates> stopPlaceCoordinatesPerTimetabledPassingTimeId =
                serviceJourney.getPassingTimes().getTimetabledPassingTime().stream()
                        .map(timetabledPassingTime ->
                                findStopPlaceCoordinates(index, timetabledPassingTime, journeyPatternRef, validationReportId))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                (previous, latest) -> latest
                        ));
        return new Context(
                serviceJourney,
                findTransportMode(index, serviceJourney),
                stopPlaceCoordinatesPerTimetabledPassingTimeId
        );
    }

    private AllVehicleModesOfTransportEnumeration findTransportMode(NetexEntitiesIndex index, ServiceJourney serviceJourney) {
        AllVehicleModesOfTransportEnumeration transportMode = serviceJourney.getTransportMode();
        if (transportMode == null) {
            JourneyPattern journeyPattern = index.getJourneyPatternIndex().get(
                    serviceJourney.getJourneyPatternRef().getValue().getRef());
            Route route = index.getRouteIndex().get(journeyPattern.getRouteRef().getRef());
            Line line = index.getLineIndex().get(route.getLineRef().getValue().getRef());
            return line.getTransportMode();
        }
        return transportMode;
    }

    private Map.Entry<String, StopPlaceCoordinates> findStopPlaceCoordinates(NetexEntitiesIndex index,
                                                                             TimetabledPassingTime timetabledPassingTime,
                                                                             String journeyPatternRef,
                                                                             String validationReportId) {
        String stopPointInJourneyPatternRef = timetabledPassingTime.getPointInJourneyPatternRef().getValue().getRef();
        StopPointInJourneyPattern stopPointInJourneyPattern =
                getStopPointInJourneyPattern(stopPointInJourneyPatternRef, journeyPatternRef, index);
        if (stopPointInJourneyPattern != null) {
            String scheduledStopPointRef = stopPointInJourneyPattern.getScheduledStopPointRef().getValue().getRef();
            QuayId quayId = commonDataRepository.findQuayId(scheduledStopPointRef, validationReportId);
            StopPlaceCoordinates coordinatesForQuayId = stopPlaceRepository.getCoordinatesForQuayId(quayId);
            return Map.entry(timetabledPassingTime.getId(), coordinatesForQuayId);
        }
        return null;
    }

    private StopPointInJourneyPattern getStopPointInJourneyPattern(String stopPointInJourneyPatternRef,
                                                                   String journeyPatternRef,
                                                                   NetexEntitiesIndex index) {
        return index.getJourneyPatternIndex()
                .get(journeyPatternRef)
                .getPointsInSequence()
                .getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern()
                .stream()
                .filter(StopPointInJourneyPattern.class::isInstance)
                .map(StopPointInJourneyPattern.class::cast)
                .filter(stopPointInJourneyPattern -> stopPointInJourneyPattern.getId().equals(stopPointInJourneyPatternRef))
                .findFirst()
                .orElse(null);
    }

    @Override
    public Set<String> getRuleDescriptions() {
        return Set.of(
                createRuleDescription(RuleCode.LOW_SPEED_PROGRESSION.toString(),
                        getValidationMessage(RuleCode.LOW_SPEED_PROGRESSION)),
                createRuleDescription(RuleCode.HIGH_SPEED_PROGRESSION.toString(),
                        getValidationMessage(RuleCode.HIGH_SPEED_PROGRESSION)),
                createRuleDescription(RuleCode.WARNING_SPEED_PROGRESSION.toString(),
                        getValidationMessage(RuleCode.WARNING_SPEED_PROGRESSION))
        );
    }

    private String getValidationMessage(RuleCode ruleCode) {
        return switch (ruleCode) {
            case LOW_SPEED_PROGRESSION -> "ServiceJourney has low speed progression";
            case HIGH_SPEED_PROGRESSION -> "ServiceJourney has high speed progression";
            case WARNING_SPEED_PROGRESSION -> "ServiceJourney has unexpected speed progression";
            // TODO: unexpected or warning??
        };
    }
}
