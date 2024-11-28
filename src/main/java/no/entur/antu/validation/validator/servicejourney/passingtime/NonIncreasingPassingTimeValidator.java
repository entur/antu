package no.entur.antu.validation.validator.servicejourney.passingtime;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import no.entur.antu.stoptime.SortStopTimesUtil;
import no.entur.antu.stoptime.StopTime;
import org.entur.netex.validation.validator.Severity;
import org.entur.netex.validation.validator.ValidationIssue;
import org.entur.netex.validation.validator.ValidationRule;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
import org.entur.netex.validation.validator.jaxb.JAXBValidator;
import org.rutebanken.netex.model.ServiceJourney;

/**
 * Validates that the passing times of a service journey are non-decreasing.
 * This means that the time between each stop must be greater than or equal to zero.
 * Chouette reference: 3-VehicleJourney-5
 */
public class NonIncreasingPassingTimeValidator implements JAXBValidator {

  static final ValidationRule RULE_NON_INCREASING_TIME = new ValidationRule(
    "TIMETABLED_PASSING_TIME_NON_INCREASING_TIME",
    "ServiceJourney has non-increasing TimetabledPassingTime",
    "ServiceJourney has non-increasing TimetabledPassingTime at: %s",
    Severity.ERROR
  );

  static final ValidationRule RULE_INCOMPLETE_TIME = new ValidationRule(
    "TIMETABLED_PASSING_TIME_INCOMPLETE_TIME",
    "ServiceJourney has incomplete TimetabledPassingTime",
    "ServiceJourney has incomplete TimetabledPassingTime at: %s",
    Severity.ERROR
  );

  static final ValidationRule RULE_INCONSISTENT_TIME = new ValidationRule(
    "TIMETABLED_PASSING_TIME_INCONSISTENT_TIME",
    "ServiceJourney has inconsistent TimetabledPassingTime",
    "ServiceJourney has inconsistent TimetabledPassingTime at: %s",
    Severity.ERROR
  );

  @Override
  public List<ValidationIssue> validate(
    JAXBValidationContext validationContext
  ) {
    return validationContext
      .serviceJourneys()
      .stream()
      .map(serviceJourney ->
        validateServiceJourney(serviceJourney, validationContext)
      )
      .filter(Objects::nonNull)
      .toList();
  }

  @Override
  public Set<ValidationRule> getRules() {
    return Set.of(RULE_NON_INCREASING_TIME);
  }

  @Nullable
  public ValidationIssue validateServiceJourney(
    ServiceJourney serviceJourney,
    JAXBValidationContext validationContext
  ) {
    List<StopTime> sortedTimetabledPassingTime =
      SortStopTimesUtil.getSortedStopTimes(serviceJourney, validationContext);
    var previousPassingTime = sortedTimetabledPassingTime.get(0);
    ValidationIssue issueOnFirstStop = validateStopTime(
      serviceJourney,
      validationContext,
      previousPassingTime
    );
    if (issueOnFirstStop != null) {
      return issueOnFirstStop;
    }

    for (int i = 1; i < sortedTimetabledPassingTime.size(); i++) {
      var currentPassingTime = sortedTimetabledPassingTime.get(i);

      ValidationIssue issue = validateStopTime(
        serviceJourney,
        validationContext,
        currentPassingTime
      );
      if (issue != null) {
        return issue;
      }

      if (!previousPassingTime.isStopTimesIncreasing(currentPassingTime)) {
        return new ValidationIssue(
          RULE_NON_INCREASING_TIME,
          validationContext.dataLocation(serviceJourney.getId()),
          validationContext.stopPointName(
            previousPassingTime.scheduledStopPointId()
          )
        );
      }

      previousPassingTime = currentPassingTime;
    }
    return null;
  }

  @Nullable
  private static ValidationIssue validateStopTime(
    ServiceJourney serviceJourney,
    JAXBValidationContext validationContext,
    StopTime stopTime
  ) {
    if (!stopTime.isComplete()) {
      return new ValidationIssue(
        RULE_INCOMPLETE_TIME,
        validationContext.dataLocation(serviceJourney.getId()),
        validationContext.stopPointName(stopTime.scheduledStopPointId())
      );
    }
    if (!stopTime.isConsistent()) {
      return new ValidationIssue(
        RULE_INCONSISTENT_TIME,
        validationContext.dataLocation(serviceJourney.getId()),
        validationContext.stopPointName(stopTime.scheduledStopPointId())
      );
    }
    return null;
  }
}
