package no.entur.antu.validation.validator.servicelink.stoppoints;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.entur.netex.validation.validator.Severity;
import org.entur.netex.validation.validator.ValidationIssue;
import org.entur.netex.validation.validator.ValidationRule;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
import org.entur.netex.validation.validator.jaxb.JAXBValidator;
import org.entur.netex.validation.validator.model.FromToScheduledStopPointId;
import org.entur.netex.validation.validator.model.ScheduledStopPointId;

/**
 * Validates that the stop points in the service link matches the journey pattern.
 * StopPoints and Links in journey pattern are checked in order.
 * Example of valid Service link:
 * StopPoints and journey pattern matches the stop points in service link.
 * <p>
 * Journey Pattern:
 * ---------------
 * StopPoint(1)---|
 *                | --> ServiceLink(1)
 * StopPoint(2)---|
 * <p>
 * Service Link:
 * ------------
 *                   | --> From --> StopPoint1
 * ServiceLink(1) -- |
 *                   | --> To --> StopPoint2
 * <p>
 * Chouette refrerence: 3-RouteSection-2-3
 */
public class MismatchedStopPointsValidator implements JAXBValidator {

  static final ValidationRule RULE = new ValidationRule(
    "STOP_POINTS_IN_SERVICE_LINK_DOES_NOT_MATCH_THE_JOURNEY_PATTERN",
    "Stop points in service link does not match the journey pattern",
    "Journey pattern id = %s, expected = [%s - %s], actual = [%s - %s]",
    Severity.WARNING
  );

  @Override
  public List<ValidationIssue> validate(
    JAXBValidationContext validationContext
  ) {
    MismatchedStopPointsContext.Builder contextBuilder =
      new MismatchedStopPointsContext.Builder(validationContext);

    return validationContext
      .journeyPatterns()
      .stream()
      .map(contextBuilder::build)
      .map(context -> validateServiceLink(validationContext, context))
      .flatMap(Collection::stream)
      .toList();
  }

  @Override
  public Set<ValidationRule> getRules() {
    return Set.of(RULE);
  }

  private List<ValidationIssue> validateServiceLink(
    JAXBValidationContext validationContext,
    MismatchedStopPointsContext context
  ) {
    BiFunction<FromToScheduledStopPointId, Function<FromToScheduledStopPointId, ScheduledStopPointId>, String> stopPointName =
      (scheduledStopPointIds, getStopPointId) ->
        Optional
          .ofNullable(scheduledStopPointIds)
          .map(getStopPointId)
          .map(validationContext::stopPointName)
          .orElse("unknown");

    return context
      .linksInJourneyPattern()
      .stream()
      .filter(serviceLinkId ->
        !context
          .stopPointsForServiceLinksInJourneyPattern()
          .get(serviceLinkId)
          .equals(context.stopPointsInServiceLink().get(serviceLinkId))
      )
      .map(serviceLinkId ->
        new ValidationIssue(
          RULE,
          validationContext.dataLocation(serviceLinkId.id()),
          context.journeyPatternId(),
          stopPointName.apply(
            context.stopPointsInServiceLink().get(serviceLinkId),
            FromToScheduledStopPointId::from
          ),
          stopPointName.apply(
            context.stopPointsInServiceLink().get(serviceLinkId),
            FromToScheduledStopPointId::to
          ),
          stopPointName.apply(
            context
              .stopPointsForServiceLinksInJourneyPattern()
              .get(serviceLinkId),
            FromToScheduledStopPointId::from
          ),
          stopPointName.apply(
            context
              .stopPointsForServiceLinksInJourneyPattern()
              .get(serviceLinkId),
            FromToScheduledStopPointId::to
          )
        )
      )
      .toList();
  }
}
