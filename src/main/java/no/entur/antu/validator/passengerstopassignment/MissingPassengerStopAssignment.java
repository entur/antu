package no.entur.antu.validator.passengerstopassignment;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import no.entur.antu.commondata.CommonDataRepository;
import no.entur.antu.exception.AntuException;
import no.entur.antu.validator.AntuNetexValidator;
import no.entur.antu.validator.RuleCode;
import no.entur.antu.validator.ValidationContextWithNetexEntitiesIndex;
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
public class MissingPassengerStopAssignment extends AntuNetexValidator {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    MissingPassengerStopAssignment.class
  );
  private final CommonDataRepository commonDataRepository;

  @Override
  protected RuleCode[] getRuleCodes() {
    return MissingPassengerStopAssignmentError.RuleCode.values();
  }

  public MissingPassengerStopAssignment(
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

      MissingPassengerStopAssignmentContext.Builder builder =
        new MissingPassengerStopAssignmentContext.Builder(
          validationReport.getValidationReportId(),
          commonDataRepository,
          index
        );

      // Build MissingPassengerStopAssignmentContexts for all the StopPoints in the given JourneyPattern
      List<MissingPassengerStopAssignmentContext> stopPointInJourneyPatternContexts =
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
            MissingPassengerStopAssignmentContext::hasPassengerStopAssignment
          )
        )
        .filter(missingPassengerStopAssignmentContext ->
          !validateStopPointInJourneyPattern(
            index,
            missingPassengerStopAssignmentContext
          )
        )
        .forEach(stopPointInJourneyPattern ->
          addValidationReportEntry(
            validationReport,
            validationContext,
            new MissingPassengerStopAssignmentError(
              stopPointInJourneyPattern.stopPointInJourneyPatternRef(),
              stopPointInJourneyPattern.scheduledStopPointRef(),
              MissingPassengerStopAssignmentError.RuleCode.MISSING_SCHEDULED_STOP_ASSIGNMENT
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
    MissingPassengerStopAssignmentContext missingPassengerStopAssignmentContext
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
        missingPassengerStopAssignmentContext.journeyPatternRef()
      ) &&
      isNotAssignedToAnyServiceJourney(
        serviceJourneys,
        missingPassengerStopAssignmentContext.journeyPatternRef()
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