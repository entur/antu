package no.entur.antu.validator.stoppointinjourneypatternvalidator;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import no.entur.antu.commondata.CommonDataRepository;
import no.entur.antu.exception.AntuException;
import no.entur.antu.validator.AntuNetexValidator;
import no.entur.antu.validator.RuleCode;
import no.entur.antu.validator.ValidationContextWithNetexEntitiesIndex;
import no.entur.antu.validator.stoppointinjourneypatternvalidator.StopPointInJourneyPatternContextBuilder.StopPointInJourneyPatternContext;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.*;
import org.entur.netex.validation.validator.xpath.ValidationContext;
import org.rutebanken.netex.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates that StopPointInJourneyPattern has a ScheduledStopAssignment.
 * The only valid case for missing ScheduledStopAssignment is when the
 * StopPointInJourneyPattern is assigned to a DeadRun, and not to any ServiceJourney.
 * <p>
 * Missing SPA -> No DeadRun -> No SJ -> Error
 * Missing SPA -> No DeadRun -> Yes SJ -> Error
 * Missing SPA -> Yes DeadRun -> Yes SJ -> Error
 * Missing SPA -> Yes DeadRun -> No SJ -> OK
 */
public class StopPointInJourneyPatternValidator extends AntuNetexValidator {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    StopPointInJourneyPatternValidator.class
  );
  private final CommonDataRepository commonDataRepository;

  @Override
  protected RuleCode[] getRuleCodes() {
    return StopPointInJourneyPatternError.RuleCode.values();
  }

  public StopPointInJourneyPatternValidator(
    ValidationReportEntryFactory validationReportEntryFactory,
    CommonDataRepository commonDataRepository
  ) {
    super(validationReportEntryFactory);
    this.commonDataRepository = commonDataRepository;
  }

  @Override
  public void validate(
    ValidationReport validationReport,
    ValidationContext validationContext
  ) {
    LOGGER.debug("Validating Stop place in journey pattern");

    if (validationContext.isCommonFile()) {
      return;
    }

    if (
      validationContext instanceof ValidationContextWithNetexEntitiesIndex validationContextWithNetexEntitiesIndex
    ) {
      NetexEntitiesIndex index =
        validationContextWithNetexEntitiesIndex.getNetexEntitiesIndex();

      StopPointInJourneyPatternContextBuilder builder =
        new StopPointInJourneyPatternContextBuilder(
          validationReport.getValidationReportId(),
          commonDataRepository,
          index
        );

      // Get all StopPointInJourneyPatternContexts
      List<StopPointInJourneyPatternContext> stopPointInJourneyPatternContexts =
        index
          .getJourneyPatternIndex()
          .getAll()
          .stream()
          .flatMap(journeyPattern -> builder.build(journeyPattern).stream())
          .toList();

      stopPointInJourneyPatternContexts
        .stream()
        .filter(
          Predicate.not(
            StopPointInJourneyPatternContext::hasPassengerStopAssignment
          )
        )
        .filter(stopPointInJourneyPatternContext ->
          !validateStopPointInJourneyPattern(
            index,
            stopPointInJourneyPatternContext
          )
        )
        .forEach(stopPointInJourneyPattern ->
          addValidationReportEntry(
            validationReport,
            validationContext,
            new StopPointInJourneyPatternError(
              stopPointInJourneyPattern.stopPointInJourneyPatternRef(),
              stopPointInJourneyPattern.scheduledStopPointRef(),
              StopPointInJourneyPatternError.RuleCode.MISSING_SCHEDULED_STOP_ASSIGNMENT
            )
          )
        );
    } else {
      throw new AntuException(
        "Received invalid validation context in Stop Point Validation"
      );
    }
  }

  private boolean validateStopPointInJourneyPattern(
    NetexEntitiesIndex index,
    StopPointInJourneyPatternContext stopPointInJourneyPatternContext
  ) {
    Map<Boolean, List<Journey_VersionStructure>> deadRunsAndRestOfServiceJourneys =
      index
        .getTimetableFrames()
        .stream()
        .flatMap(timetableFrame ->
          timetableFrame
            .getVehicleJourneys()
            .getVehicleJourneyOrDatedVehicleJourneyOrNormalDatedVehicleJourney()
            .stream()
        )
        .collect(Collectors.partitioningBy(DeadRun.class::isInstance));

    List<DeadRun> deadRuns = deadRunsAndRestOfServiceJourneys
      .get(Boolean.TRUE)
      .stream()
      .filter(DeadRun.class::isInstance)
      .map(DeadRun.class::cast)
      .toList();

    List<ServiceJourney> serviceJourneys = deadRunsAndRestOfServiceJourneys
      .get(Boolean.FALSE)
      .stream()
      .filter(ServiceJourney.class::isInstance)
      .map(ServiceJourney.class::cast)
      .toList();

    return (
      isAssignedToDeadRun(
        deadRuns,
        stopPointInJourneyPatternContext.journeyPatternRef()
      ) &&
      isNotAssignedToAnyServiceJourney(
        serviceJourneys,
        stopPointInJourneyPatternContext.journeyPatternRef()
      )
    );
  }

  private boolean isAssignedToDeadRun(
    List<DeadRun> deadRuns,
    String journeyPatternRef
  ) {
    return deadRuns
      .stream()
      .map(deadRun -> deadRun.getJourneyPatternRef().getValue().getRef())
      .anyMatch(deadRunJourneyPatternRef ->
        deadRunJourneyPatternRef.equals(journeyPatternRef)
      );
  }

  private boolean isNotAssignedToAnyServiceJourney(
    List<ServiceJourney> serviceJourneys,
    String journeyPatternRef
  ) {
    return serviceJourneys
      .stream()
      .map(serviceJourney ->
        serviceJourney.getJourneyPatternRef().getValue().getRef()
      )
      .noneMatch(serviceJourneyJourneyPatternRef ->
        serviceJourneyJourneyPatternRef.equals(journeyPatternRef)
      );
  }
}
