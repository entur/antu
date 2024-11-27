package no.entur.antu.validation.validator.passengerstopassignment;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.entur.netex.validation.validator.Severity;
import org.entur.netex.validation.validator.ValidationIssue;
import org.entur.netex.validation.validator.ValidationRule;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
import org.entur.netex.validation.validator.jaxb.JAXBValidator;
import org.rutebanken.netex.model.DeadRun;
import org.rutebanken.netex.model.ServiceJourney;

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
public class MissingPassengerStopAssignmentValidator implements JAXBValidator {

  static final ValidationRule RULE = new ValidationRule(
    "MISSING_SCHEDULED_STOP_ASSIGNMENT",
    "Missing ScheduledStopAssignment for StopPointInJourneyPattern",
    "Missing ScheduledStopAssignment for StopPoint: %s",
    Severity.ERROR
  );

  @Override
  public List<ValidationIssue> validate(
    JAXBValidationContext validationContext
  ) {
    MissingPassengerStopAssignmentContext.Builder builder =
      new MissingPassengerStopAssignmentContext.Builder(validationContext);

    return validationContext
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
      .map(context ->
        new ValidationIssue(
          RULE,
          validationContext.dataLocation(
            context.stopPointInJourneyPatternRef()
          ),
          validationContext.stopPointName(context.scheduledStopPointId())
        )
      )
      .toList();
  }

  @Override
  public Set<ValidationRule> getRules() {
    return Set.of(RULE);
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
