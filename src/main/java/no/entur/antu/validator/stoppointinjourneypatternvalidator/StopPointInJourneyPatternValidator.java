package no.entur.antu.validator.stoppointinjourneypatternvalidator;

import no.entur.antu.commondata.CommonDataRepository;
import no.entur.antu.exception.AntuException;
import no.entur.antu.model.QuayId;
import no.entur.antu.stop.StopPlaceRepository;
import no.entur.antu.stoptime.SortedStopTimes;
import no.entur.antu.stoptime.StopTime;
import no.entur.antu.validator.ValidationContextWithNetexEntitiesIndex;
import no.entur.antu.validator.speedprogressionvalidator.ExpectedSpeed;
import no.entur.antu.validator.speedprogressionvalidator.PassingTimes;
import no.entur.antu.validator.speedprogressionvalidator.SpeedProgressionError;
import no.entur.antu.validator.stoppointinjourneypatternvalidator.StopPointInJourneyPatternContextBuilder.StopPointInJourneyPatternContext;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.index.api.NetexEntityIndex;
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
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static no.entur.antu.validator.speedprogressionvalidator.ServiceJourneyContextBuilder.ServiceJourneyContext;

public class StopPointInJourneyPatternValidator extends AbstractNetexValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(StopPointInJourneyPatternValidator.class);
    private final CommonDataRepository commonDataRepository;
    private final StopPlaceRepository stopPlaceRepository;

    private enum ErrorCode {
        ASSIGNED_TO_DEAD_RUN("ScheduledStopAssignment to DeadRun"),
        ASSIGNED_TO_NO_VEHICLE_JOURNEY("ScheduledStopAssignment to no vehicle journey");

        private final String errorMessage;

        ErrorCode(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

    }

    public StopPointInJourneyPatternValidator(ValidationReportEntryFactory validationReportEntryFactory,
                                              CommonDataRepository commonDataRepository,
                                              StopPlaceRepository stopPlaceRepository) {
        super(validationReportEntryFactory);
        this.commonDataRepository = commonDataRepository;
        this.stopPlaceRepository = stopPlaceRepository;
    }

    private void validateScheduledStopAssignment(NetexEntitiesIndex index, Consumer<ErrorCode> reportError) {
    }

    private Predicate<String> isAssignedToDeadRun(List<DeadRun> deadRuns, NetexEntitiesIndex index) {
        return scheduledStopPointRef -> deadRuns.stream()
                .map(deadRun -> deadRun.getJourneyPatternRef().getValue().getRef())
                .flatMap(getScheduledStopPointRefsInJourneyPattern(index.getJourneyPatternIndex()))
                .anyMatch(scheduledStopPointRefInJourneyPattern -> scheduledStopPointRefInJourneyPattern.equals(scheduledStopPointRef));
    }

    private Predicate<String> isNotAssignedToAnyServiceJourney(List<ServiceJourney> serviceJourneys, NetexEntitiesIndex index) {
        return scheduledStopPointRef -> serviceJourneys.stream()
                .map(serviceJourney -> serviceJourney.getJourneyPatternRef().getValue().getRef())
                .flatMap(getScheduledStopPointRefsInJourneyPattern(index.getJourneyPatternIndex()))
                .noneMatch(scheduledStopPointRefInJourneyPattern -> scheduledStopPointRefInJourneyPattern.equals(scheduledStopPointRef));
    }

    private Function<String, Stream<String>> getScheduledStopPointRefsInJourneyPattern(NetexEntityIndex<JourneyPattern> journeyPatternIndex) {
        return journeyPatternRef -> journeyPatternIndex.get(journeyPatternRef)
                .getPointsInSequence()
                .getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern().stream()
                .filter(StopPointInJourneyPattern.class::isInstance)
                .map(StopPointInJourneyPattern.class::cast)
                .map(stopPointInJourneyPattern -> stopPointInJourneyPattern.getScheduledStopPointRef().getValue().getRef());
    }



    private void validateStopPointInJourneyPattern(StopPointInJourneyPatternContext stopPointInJourneyPatternContext) {

    }

    @Override
    public void validate(ValidationReport validationReport, ValidationContext validationContext) {

        LOGGER.debug("Validating Scheduled stop assignment");

        if (validationContext.isCommonFile()) {
            return;
        }

        if (validationContext instanceof ValidationContextWithNetexEntitiesIndex validationContextWithNetexEntitiesIndex) {
            NetexEntitiesIndex index = validationContextWithNetexEntitiesIndex.getNetexEntitiesIndex();

            StopPointInJourneyPatternContextBuilder builder = new StopPointInJourneyPatternContextBuilder(commonDataRepository);
            List<StopPointInJourneyPatternContext> stopPointInJourneyPatternContexts = index.getJourneyPatternIndex().getAll().stream()
                    .flatMap(journeyPattern -> builder.build(journeyPattern, validationReport.getValidationReportId()).stream())
                    .toList();


            Map<String, QuayId> scheduledStopAssignments =
                    commonDataRepository.getScheduledStopPointToQuayIdMap(validationReport.getValidationReportId());

            Map<Boolean, List<Journey_VersionStructure>> deadRunsAndRestOfServiceJourneys = index.getTimetableFrames().stream()
                    .flatMap(timetableFrame -> timetableFrame
                            .getVehicleJourneys()
                            .getVehicleJourneyOrDatedVehicleJourneyOrNormalDatedVehicleJourney().stream())
                    .collect(Collectors.partitioningBy(DeadRun.class::isInstance));

            List<DeadRun> deadRuns = deadRunsAndRestOfServiceJourneys.get(Boolean.TRUE).stream()
                    .filter(DeadRun.class::isInstance) // Don't need it
                    .map(DeadRun.class::cast)
                    .toList();

            List<ServiceJourney> serviceJourneys = deadRunsAndRestOfServiceJourneys.get(Boolean.FALSE).stream()
                    .filter(ServiceJourney.class::isInstance)
                    .map(ServiceJourney.class::cast)
                    .toList();

            scheduledStopAssignments.entrySet().stream()
                    .filter(isAssignedToDeadRun(deadRuns, index)
                            .or(isNotAssignedToAnyServiceJourney(serviceJourneys, index)))



            serviceJourneys.stream()
                    .map(serviceJourney ->
                            contextBuilder.build(
                                    index,
                                    serviceJourney,
                                    validationReport.getValidationReportId()
                            ))
                    .forEach(context -> validateServiceJourney(
                                    context,
                                    index,
                                    error ->
                                            addValidationReportEntry(
                                                    validationReport,
                                                    validationContext,
                                                    context.serviceJourney(),
                                                    error
                                            )
                            )
                    );

        } else {
            throw new AntuException(
                    "Received invalid validation context in Speed Progression validator"
            );
        }
    }


    private void validateServiceJourney(ServiceJourneyContext context,
                                        NetexEntitiesIndex netexEntitiesIndex,
                                        Consumer<SpeedProgressionError> reportError) {

        List<StopTime> sortedTimetabledPassingTime =
                SortedStopTimes.from(context.serviceJourney(), netexEntitiesIndex);

        IntStream.range(1, sortedTimetabledPassingTime.size())
                .mapToObj(i -> new PassingTimes(
                        sortedTimetabledPassingTime.get(i - 1),
                        sortedTimetabledPassingTime.get(i)))
                .takeWhile(PassingTimes::isValid) // TODO: Can we filter it instead of takeWhile, Test it.
                .filter(PassingTimes::hasValidTimeDifference)
                .filter(context::hasValidCoordinates)
                .map(passingTimes -> validateSpeedProgression(context, passingTimes))
                .filter(Objects::nonNull)
                .forEach(reportError);
    }

    private SpeedProgressionError validateSpeedProgression(ServiceJourneyContext context, PassingTimes passingTimes) {

        double distance = context.calculateDistance(passingTimes);

        if (distance < 1) {
            // superimposed stops, speed not calculable
            return null;
        }

        return validateSpeed(distance, ExpectedSpeed.of(context.transportMode()), passingTimes);
    }

    /**
     * Why multiply by 3.6?
     * <p>
     * To convert m/s to km/h, we have to convert two units i.e. distance and time
     * First km to meters, 1 km = 1000 meters
     * And hour to seconds, 1 hour = 1×60×60 = 3600,
     * <p>
     * So 1 m/s = 1000/3600 = 10/36 = 0.277
     * So to convert km/h to m/s, we have to multiply by 0.277 approx.
     * <p>
     * Similarly, to convert m/s to km/h,
     * To convert meters to kilometers divide by 1000
     * To Convert seconds to hours divide by 3600 i.e. for 1 second 1/3600,
     * <p>
     * So 1 km/h = (1/1000) ÷ (1÷3600) = (1×3600)/1000 = 3.6
     * So to convert m/s to km/h, we have to multiply by 3.6
     */
    private double calculateSpeedInKilometerPerHour(double distanceInMeters, DoubleSupplier timeInSeconds) {
        return distanceInMeters / timeInSeconds.getAsDouble() * 3.6;
    }

    private SpeedProgressionError validateSpeed(double distance, ExpectedSpeed expectedSpeed, PassingTimes passingTimes) {

        // Assume max error (120 sec) when comparing with min and max expected speed.
        double optimisticSpeed = calculateSpeedInKilometerPerHour(
                distance,
                () -> passingTimes.minimumPossibleTimeDifference(120));

        double pessimisticSpeed = calculateSpeedInKilometerPerHour(
                distance,
                () -> passingTimes.maximumPossibleTimeDifference(120));

        if (optimisticSpeed < expectedSpeed.minSpeed()) {
            // too slow
            return new SpeedProgressionError(
                    passingTimes,
                    SpeedProgressionError.RuleCode.LOW_SPEED_PROGRESSION,
                    Long.toString(expectedSpeed.minSpeed()),
                    Double.toString(optimisticSpeed));
        } else if (pessimisticSpeed > expectedSpeed.warningSpeed()) {
            // too fast
            if (pessimisticSpeed > expectedSpeed.maxSpeed()) {
                return new SpeedProgressionError(
                        passingTimes,
                        SpeedProgressionError.RuleCode.HIGH_SPEED_PROGRESSION,
                        Long.toString(expectedSpeed.maxSpeed()),
                        Double.toString(pessimisticSpeed));
            } else {
                return new SpeedProgressionError(
                        passingTimes,
                        SpeedProgressionError.RuleCode.WARNING_SPEED_PROGRESSION,
                        Long.toString(expectedSpeed.warningSpeed()),
                        Double.toString(pessimisticSpeed));
            }
        }
        return null;
    }

    private void addValidationReportEntry(ValidationReport validationReport,
                                          ValidationContext validationContext,
                                          ServiceJourney serviceJourney,
                                          SpeedProgressionError speedProgressionError) {

        ValidationReportEntry validationReportEntry = createValidationReportEntry(
                speedProgressionError.ruleCode().toString(),
                findDataLocation(validationContext, serviceJourney),
                speedProgressionError.validationReportEntryMessage(serviceJourney.getId())
        );

        validationReport.addValidationReportEntry(validationReportEntry);
    }

    private static DataLocation findDataLocation(ValidationContext validationContext, ServiceJourney serviceJourney) {
        String fileName = validationContext.getFileName();
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
                createRuleDescription(SpeedProgressionError.RuleCode.LOW_SPEED_PROGRESSION.toString(),
                        SpeedProgressionError.RuleCode.LOW_SPEED_PROGRESSION.getErrorMessage()),
                createRuleDescription(SpeedProgressionError.RuleCode.HIGH_SPEED_PROGRESSION.toString(),
                        SpeedProgressionError.RuleCode.HIGH_SPEED_PROGRESSION.getErrorMessage()),
                createRuleDescription(SpeedProgressionError.RuleCode.WARNING_SPEED_PROGRESSION.toString(),
                        SpeedProgressionError.RuleCode.WARNING_SPEED_PROGRESSION.getErrorMessage())
        );
    }
}
