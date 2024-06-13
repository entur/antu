package no.entur.antu.validation.validator.passengerstopassignment;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import no.entur.antu.validation.AntuNetexData;
import no.entur.antu.validation.AntuNetexValidator;
import no.entur.antu.validation.RuleCode;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntryFactory;
import org.entur.netex.validation.validator.xpath.ValidationContext;
import org.rutebanken.netex.model.DeadRun;
import org.rutebanken.netex.model.Journey_VersionStructure;
import org.rutebanken.netex.model.ServiceJourney;
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
 *
 * Chouette reference: rutebanken_3-StopPoint-1
 */
public class MissingPassengerStopAssignmentValidator
  extends AntuNetexValidator {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    MissingPassengerStopAssignmentValidator.class
  );

  @Override
  protected RuleCode[] getRuleCodes() {
    return MissingPassengerStopAssignmentError.RuleCode.values();
  }

  public MissingPassengerStopAssignmentValidator(
    ValidationReportEntryFactory validationReportEntryFactory
  ) {
    super(validationReportEntryFactory);
  }

  @Override
  public void validateLineFile(
    ValidationReport validationReport,
    ValidationContext validationContext,
    AntuNetexData antuNetexData
  ) {
    LOGGER.debug("Validating Stop place in journey pattern");

    MissingPassengerStopAssignmentContext.Builder builder =
      new MissingPassengerStopAssignmentContext.Builder(antuNetexData);

    antuNetexData
      .journeyPatterns()
      .map(builder::build)
      .flatMap(List::stream)
      .filter(
        Predicate.not(
          MissingPassengerStopAssignmentContext::hasPassengerStopAssignment
        )
      )
      .filter(context ->
        !validateStopPointInJourneyPattern(antuNetexData, context)
      )
      .forEach(context ->
        addValidationReportEntry(
          validationReport,
          validationContext,
          new MissingPassengerStopAssignmentError(
            context.stopPointInJourneyPatternRef(),
            antuNetexData.stopPointName(context.scheduledStopPointId()),
            MissingPassengerStopAssignmentError.RuleCode.MISSING_SCHEDULED_STOP_ASSIGNMENT
          )
        )
      );
  }

  @Override
  protected void validateCommonFile(
    ValidationReport validationReport,
    ValidationContext validationContext,
    AntuNetexData antuNetexData
  ) {
    // StopPoints and JourneyPatterns only appear in the Line file.
  }

  private boolean validateStopPointInJourneyPattern(
    AntuNetexData antuNetexData,
    MissingPassengerStopAssignmentContext missingPassengerStopAssignmentContext
  ) {
    Map<Boolean, List<Journey_VersionStructure>> deadRunsAndRestOfServiceJourneys =
      antuNetexData
        .netexEntitiesIndex()
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
