package no.entur.antu.validation.validator.passengerstopassignment;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import no.entur.antu.validation.AntuNetexValidator;
import no.entur.antu.validation.RuleCode;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntryFactory;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
import org.rutebanken.netex.model.DeadRun;
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
    JAXBValidationContext validationContext
  ) {
    LOGGER.debug("Validating Stop place in journey pattern");

    MissingPassengerStopAssignmentContext.Builder builder =
      new MissingPassengerStopAssignmentContext.Builder(validationContext);

    validationContext
      .journeyPatterns()
      .stream()
      .map(builder::build)
      .flatMap(List::stream)
      .filter(
        Predicate.not(
          MissingPassengerStopAssignmentContext::hasPassengerStopAssignment
        )
      )
      .filter(context ->
        !validateStopPointInJourneyPattern(validationContext, context)
      )
      .forEach(context ->
        addValidationReportEntry(
          validationReport,
          validationContext,
          new MissingPassengerStopAssignmentError(
            context.stopPointInJourneyPatternRef(),
            validationContext.stopPointName(context.scheduledStopPointId()),
            MissingPassengerStopAssignmentError.RuleCode.MISSING_SCHEDULED_STOP_ASSIGNMENT
          )
        )
      );
  }

  @Override
  protected void validateCommonFile(
    ValidationReport validationReport,
    JAXBValidationContext validationContext
  ) {
    // StopPoints and JourneyPatterns only appear in the Line file.
  }

  private boolean validateStopPointInJourneyPattern(
    JAXBValidationContext validationContext,
    MissingPassengerStopAssignmentContext missingPassengerStopAssignmentContext
  ) {
    Collection<DeadRun> deadRuns = validationContext.deadRuns();
    Collection<ServiceJourney> serviceJourneys =
      validationContext.serviceJourneys();

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
    Collection<DeadRun> deadRuns,
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
    Collection<ServiceJourney> serviceJourneys,
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
