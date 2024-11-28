package no.entur.antu.validation.validator.servicelink.distance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import no.entur.antu.validation.utilities.SphericalDistanceLibrary;
import org.entur.netex.validation.validator.Severity;
import org.entur.netex.validation.validator.ValidationIssue;
import org.entur.netex.validation.validator.ValidationRule;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
import org.entur.netex.validation.validator.jaxb.JAXBValidator;
import org.locationtech.jts.geom.Coordinate;

/**
 * Validates ServiceLinks, by checking the distance between the stop points and the line string.
 * Stop points are the start and end point of the service link, and the line string is the geometry of the service link.
 * The distance is expected to be within a configured 'WARNING' and 'MAX' limits, and if it exceeds the limit,
 * a warning or an error is added to the validation report.
 * Chouette references: 3-RouteSection-2-1, 3-RouteSection-2-11, 3-RouteSection-2-2, 3-RouteSection-2-22
 */
public class UnexpectedDistanceInServiceLinkValidator implements JAXBValidator {

  static final ValidationRule RULE_DISTANCE_TO_START_ABOVE_LIMIT =
    new ValidationRule(
      "DISTANCE_BETWEEN_STOP_POINT_AND_START_OF_LINE_STRING_EXCEEDS_MAX_LIMIT",
      "Distance between stop point and start of linestring, exceeds max limit, in LinkSequenceProjection",
      "ScheduledStopPoint = %s, expected = %s, actual = %s",
      Severity.ERROR
    );

  static final ValidationRule RULE_DISTANCE_TO_START_ABOVE_WARNING =
    new ValidationRule(
      "DISTANCE_BETWEEN_STOP_POINT_AND_START_OF_LINE_STRING_EXCEEDS_WARNING_LIMIT",
      "Distance between stop point and start of linestring, exceeds warning limit, in LinkSequenceProjection",
      "ScheduledStopPoint = %s, expected = %s, actual = %s",
      Severity.WARNING
    );

  static final ValidationRule RULE_DISTANCE_TO_END_ABOVE_LIMIT =
    new ValidationRule(
      "DISTANCE_BETWEEN_STOP_POINT_AND_END_OF_LINE_STRING_EXCEEDS_MAX_LIMIT",
      "Distance between stop point and end of linestring, exceeds max limit, in LinkSequenceProjection",
      "ScheduledStopPoint = %s, expected = %s, actual = %s",
      Severity.ERROR
    );

  static final ValidationRule RULE_DISTANCE_TO_END_ABOVE_WARNING =
    new ValidationRule(
      "DISTANCE_BETWEEN_STOP_POINT_AND_END_OF_LINE_STRING_EXCEEDS_WARNING_LIMIT",
      "Distance between stop point and end of linestring, exceeds warning limit, in LinkSequenceProjection",
      "ScheduledStopPoint = %s, expected = %s, actual = %s",
      Severity.WARNING
    );

  private static final long DISTANCE_WARNING = 20;
  private static final long DISTANCE_MAX = 100;

  @Override
  public List<ValidationIssue> validate(
    JAXBValidationContext validationContext
  ) {
    UnexpectedDistanceInServiceLinkContext.Builder contextBuilder =
      new UnexpectedDistanceInServiceLinkContext.Builder(validationContext);

    return validationContext
      .serviceLinks()
      .stream()
      .map(contextBuilder::build)
      .filter(Objects::nonNull)
      .map(unexpectedDistanceInServiceLinkContext ->
        validateServiceLink(
          validationContext,
          unexpectedDistanceInServiceLinkContext
        )
      )
      .flatMap(Collection::stream)
      .filter(Objects::nonNull)
      .toList();
  }

  @Override
  public Set<ValidationRule> getRules() {
    return Set.of(
      RULE_DISTANCE_TO_START_ABOVE_WARNING,
      RULE_DISTANCE_TO_START_ABOVE_LIMIT,
      RULE_DISTANCE_TO_END_ABOVE_WARNING,
      RULE_DISTANCE_TO_END_ABOVE_LIMIT
    );
  }

  /**
   * Validates the distance between the stop points and the line string.
   * If the distance exceeds the warning limit, a warning is added to the validation report.
   * If the distance exceeds the max limit, an error is added to the validation report.
   */
  private List<ValidationIssue> validateServiceLink(
    JAXBValidationContext validationContext,
    UnexpectedDistanceInServiceLinkContext context
  ) {
    List<ValidationIssue> issues = new ArrayList<>();

    Coordinate startCoordinate = context
      .fromQuayCoordinates()
      .asJtsCoordinate();
    Coordinate endCoordinate = context.toQuayCoordinates().asJtsCoordinate();

    Coordinate geometryStartCoordinate = context
      .lineString()
      .getStartPoint()
      .getCoordinate();
    Coordinate geometryEndCoordinate = context
      .lineString()
      .getEndPoint()
      .getCoordinate();

    long distanceFromStart = (long) SphericalDistanceLibrary.fastDistance(
      startCoordinate,
      geometryStartCoordinate
    );
    long distanceFromEnd = (long) SphericalDistanceLibrary.fastDistance(
      endCoordinate,
      geometryEndCoordinate
    );

    issues.add(
      checkDistanceAndReportError(
        validationContext,
        distanceFromStart,
        true,
        context
      )
    );

    issues.add(
      checkDistanceAndReportError(
        validationContext,
        distanceFromEnd,
        false,
        context
      )
    );
    return issues;
  }

  private ValidationIssue checkDistanceAndReportError(
    JAXBValidationContext validationContext,
    long distance,
    boolean isStart,
    UnexpectedDistanceInServiceLinkContext context
  ) {
    if (distance > DISTANCE_MAX) {
      ValidationRule rule = isStart
        ? RULE_DISTANCE_TO_START_ABOVE_LIMIT
        : RULE_DISTANCE_TO_END_ABOVE_LIMIT;
      return new ValidationIssue(
        rule,
        validationContext.dataLocation(context.serviceLinkId()),
        isStart
          ? validationContext.stopPointName(context.fromScheduledStopPointId())
          : validationContext.stopPointName(context.toScheduledStopPointId()),
        DISTANCE_MAX,
        distance
      );
    }
    if (distance > DISTANCE_WARNING) {
      ValidationRule rule = isStart
        ? RULE_DISTANCE_TO_START_ABOVE_WARNING
        : RULE_DISTANCE_TO_END_ABOVE_WARNING;
      return new ValidationIssue(
        rule,
        validationContext.dataLocation(context.serviceLinkId()),
        isStart
          ? validationContext.stopPointName(context.fromScheduledStopPointId())
          : validationContext.stopPointName(context.toScheduledStopPointId()),
        DISTANCE_MAX,
        distance
      );
    }
    return null;
  }
}
