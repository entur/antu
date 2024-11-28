package no.entur.antu.validation.validator.interchange.distance;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import no.entur.antu.validation.utilities.SphericalDistanceLibrary;
import org.entur.netex.validation.validator.Severity;
import org.entur.netex.validation.validator.ValidationIssue;
import org.entur.netex.validation.validator.ValidationRule;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
import org.entur.netex.validation.validator.jaxb.JAXBValidator;

/**
 * Validates that the distance between stop points in interchange is within the expected range.
 * Chouette reference: 3-Interchange-7-1, 3-Interchange-7-2
 */
public class UnexpectedInterchangeDistanceValidator implements JAXBValidator {

  static final ValidationRule RULE_MAX_LIMIT = new ValidationRule(
    "DISTANCE_BETWEEN_STOP_POINTS_IN_INTERCHANGE_IS_MORE_THAN_MAX_LIMIT",
    "Distance between stop points in interchange is more than maximum limit",
    "Distance between stop points (%s - %s) is more than expected. Expected: %s, actual: %s.",
    Severity.WARNING
  );

  static final ValidationRule RULE_WARN_LIMIT = new ValidationRule(
    "DISTANCE_BETWEEN_STOP_POINTS_IN_INTERCHANGE_IS_MORE_THAN_WARNING_LIMIT",
    "Distance between stop points in interchange is more than warning limit",
    "Distance between stop points (%s - %s) is more than expected. Expected: %s, actual: %s.",
    Severity.WARNING
  );

  private static final double INTERCHANGE_EXPECTED_DISTANCE = 1000;

  @Override
  public List<ValidationIssue> validate(
    JAXBValidationContext validationContext
  ) {
    return validationContext
      .serviceJourneyInterchanges()
      .stream()
      .map(serviceJourneyInterchange ->
        UnexpectedInterchangeDistanceContext.of(
          validationContext,
          serviceJourneyInterchange
        )
      )
      .filter(UnexpectedInterchangeDistanceContext::isValid)
      .map(context -> validateDistance(validationContext, context))
      .filter(Objects::nonNull)
      .toList();
  }

  @Override
  public Set<ValidationRule> getRules() {
    return Set.of(RULE_MAX_LIMIT, RULE_WARN_LIMIT);
  }

  @Nullable
  private ValidationIssue validateDistance(
    JAXBValidationContext validationContext,
    UnexpectedInterchangeDistanceContext distanceContext
  ) {
    double distance = SphericalDistanceLibrary.distance(
      distanceContext.fromStopPointCoordinates().quayCoordinates(),
      distanceContext.toStopPointCoordinates().quayCoordinates()
    );

    if (distance > INTERCHANGE_EXPECTED_DISTANCE) {
      if (distance > (3 * INTERCHANGE_EXPECTED_DISTANCE)) {
        return new ValidationIssue(
          RULE_MAX_LIMIT,
          validationContext.dataLocation(distanceContext.interchangeId()),
          validationContext.stopPointName(
            distanceContext.fromStopPointCoordinates().scheduledStopPointId()
          ),
          validationContext.stopPointName(
            distanceContext.toStopPointCoordinates().scheduledStopPointId()
          ),
          3 * INTERCHANGE_EXPECTED_DISTANCE,
          distance
        );
      } else {
        return new ValidationIssue(
          RULE_WARN_LIMIT,
          validationContext.dataLocation(distanceContext.interchangeId()),
          validationContext.stopPointName(
            distanceContext.fromStopPointCoordinates().scheduledStopPointId()
          ),
          validationContext.stopPointName(
            distanceContext.toStopPointCoordinates().scheduledStopPointId()
          ),
          INTERCHANGE_EXPECTED_DISTANCE,
          distance
        );
      }
    }
    return null;
  }
}
