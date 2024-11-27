package no.entur.antu.validation.validator.journeypattern.stoppoint.distance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import no.entur.antu.validation.utilities.SphericalDistanceLibrary;
import org.entur.netex.validation.validator.Severity;
import org.entur.netex.validation.validator.ValidationIssue;
import org.entur.netex.validation.validator.ValidationRule;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
import org.entur.netex.validation.validator.jaxb.JAXBValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validate that the distance between stops in journey patterns is as expected.
 * The distance between stops in a journey pattern should be within a configured 'MIN' and 'MAX' limits.
 * If the distance is less than the 'MIN' limit, a warning is added to the validation report.
 * If the distance is more than the 'MAX' limit, an error is added to the validation report.
 * Chouette Reference: 3-JourneyPattern-rutebanken-1
 */
public class UnexpectedDistanceBetweenStopPointsValidator
  implements JAXBValidator {

  static final ValidationRule RULE_DISTANCE_BETWEEN_STOP_POINTS_LESS_THAN_EXPECTED =
    new ValidationRule(
      "DISTANCE_BETWEEN_STOP_POINTS_LESS_THAN_EXPECTED",
      "Distance between stop points is less than expected",
      "Distance between stop points (%s - %s) is less than expected. Expected: %s, actual: %s.",
      Severity.WARNING
    );

  static final ValidationRule RULE_DISTANCE_BETWEEN_STOP_POINTS_MORE_THAN_EXPECTED =
    new ValidationRule(
      "DISTANCE_BETWEEN_STOP_POINTS_MORE_THAN_EXPECTED",
      "Distance between stop points is more than expected",
      "Distance between stop points (%s - %s) is more than expected. Expected: %s, actual: %s.",
      Severity.WARNING
    );

  private static final Logger LOGGER = LoggerFactory.getLogger(
    UnexpectedDistanceBetweenStopPointsValidator.class
  );

  @Override
  public List<ValidationIssue> validate(
    JAXBValidationContext validationContext
  ) {
    LOGGER.debug("Validating distance between stops in journey patterns");

    UnexpectedDistanceBetweenStopPointsContext.Builder builder =
      new UnexpectedDistanceBetweenStopPointsContext.Builder(validationContext);

    return validationContext
      .journeyPatterns()
      .stream()
      .map(builder::build)
      .filter(UnexpectedDistanceBetweenStopPointsContext::isValid)
      .map(context -> validateDistance(validationContext, context))
      .flatMap(Collection::stream)
      .toList();
  }

  @Override
  public Set<ValidationRule> getRules() {
    return Set.of(
      RULE_DISTANCE_BETWEEN_STOP_POINTS_MORE_THAN_EXPECTED,
      RULE_DISTANCE_BETWEEN_STOP_POINTS_LESS_THAN_EXPECTED
    );
  }

  private List<ValidationIssue> validateDistance(
    JAXBValidationContext validationContext,
    UnexpectedDistanceBetweenStopPointsContext distanceContext
  ) {
    List<ValidationIssue> issues = new ArrayList<>();

    ExpectedDistance expectedDistance = ExpectedDistance.of(
      distanceContext.transportMode()
    );

    UnexpectedDistanceBetweenStopPointsContext.ScheduledStopPointCoordinates previous =
      distanceContext.scheduledStopPointCoordinates().get(0);

    for (
      int i = 1;
      i < distanceContext.scheduledStopPointCoordinates().size();
      i++
    ) {
      var current = distanceContext.scheduledStopPointCoordinates().get(i);

      long distance = (long) SphericalDistanceLibrary.distance(
        previous.quayCoordinates(),
        current.quayCoordinates()
      );

      if (distance < expectedDistance.minDistance()) {
        issues.add(
          new ValidationIssue(
            RULE_DISTANCE_BETWEEN_STOP_POINTS_LESS_THAN_EXPECTED,
            validationContext.dataLocation(distanceContext.journeyPatternRef()),
            validationContext.stopPointName(previous.scheduledStopPointId()),
            validationContext.stopPointName(current.scheduledStopPointId()),
            expectedDistance.minDistance(),
            distance
          )
        );
      } else if (distance > expectedDistance.maxDistance()) {
        issues.add(
          new ValidationIssue(
            RULE_DISTANCE_BETWEEN_STOP_POINTS_MORE_THAN_EXPECTED,
            validationContext.dataLocation(distanceContext.journeyPatternRef()),
            validationContext.stopPointName(previous.scheduledStopPointId()),
            validationContext.stopPointName(current.scheduledStopPointId()),
            expectedDistance.maxDistance(),
            distance
          )
        );
      }
      previous = current;
    }
    return issues;
  }
}
