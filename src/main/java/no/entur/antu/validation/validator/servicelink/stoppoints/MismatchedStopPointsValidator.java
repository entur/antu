package no.entur.antu.validation.validator.servicelink.stoppoints;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import no.entur.antu.validation.AntuNetexValidator;
import no.entur.antu.validation.RuleCode;
import no.entur.antu.validation.ValidationError;
import no.entur.antu.validation.utilities.Comparison;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntryFactory;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
import org.entur.netex.validation.validator.model.FromToScheduledStopPointId;
import org.entur.netex.validation.validator.model.ScheduledStopPointId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class MismatchedStopPointsValidator extends AntuNetexValidator {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    MismatchedStopPointsValidator.class
  );

  public MismatchedStopPointsValidator(
    ValidationReportEntryFactory validationReportEntryFactory
  ) {
    super(validationReportEntryFactory);
  }

  @Override
  protected RuleCode[] getRuleCodes() {
    return MismatchedStopPointsError.RuleCode.values();
  }

  @Override
  public void validateCommonFile(
    ValidationReport validationReport,
    JAXBValidationContext validationContext
  ) {
    // Journey pattern are only in line file.
  }

  @Override
  protected void validateLineFile(
    ValidationReport validationReport,
    JAXBValidationContext validationContext
  ) {
    LOGGER.debug("Validating ServiceLinks");

    MismatchedStopPointsContext.Builder contextBuilder =
      new MismatchedStopPointsContext.Builder(validationContext);

    validationContext
      .journeyPatterns()
      .stream()
      .map(contextBuilder::build)
      .forEach(context ->
        validateServiceLink(
          validationContext,
          context,
          error ->
            addValidationReportEntry(validationReport, validationContext, error)
        )
      );
  }

  private void validateServiceLink(
    JAXBValidationContext validationContext,
    MismatchedStopPointsContext context,
    Consumer<ValidationError> reportError
  ) {
    BiFunction<FromToScheduledStopPointId, Function<FromToScheduledStopPointId, ScheduledStopPointId>, String> stopPointName =
      (scheduledStopPointIds, getStopPointId) ->
        Optional
          .ofNullable(scheduledStopPointIds)
          .map(getStopPointId)
          .map(validationContext::stopPointName)
          .orElse("unknown");

    context
      .linksInJourneyPattern()
      .stream()
      .filter(serviceLinkId ->
        !context
          .stopPointsForServiceLinksInJourneyPattern()
          .get(serviceLinkId)
          .equals(context.stopPointsInServiceLink().get(serviceLinkId))
      )
      .forEach(serviceLinkId ->
        reportError.accept(
          new MismatchedStopPointsError(
            MismatchedStopPointsError.RuleCode.STOP_POINTS_IN_SERVICE_LINK_DOES_NOT_MATCH_THE_JOURNEY_PATTERN,
            serviceLinkId,
            context.journeyPatternId(),
            Comparison.of(
              stopPointName.apply(
                context.stopPointsInServiceLink().get(serviceLinkId),
                FromToScheduledStopPointId::from
              ),
              stopPointName.apply(
                context
                  .stopPointsForServiceLinksInJourneyPattern()
                  .get(serviceLinkId),
                FromToScheduledStopPointId::from
              )
            ),
            Comparison.of(
              stopPointName.apply(
                context.stopPointsInServiceLink().get(serviceLinkId),
                FromToScheduledStopPointId::to
              ),
              stopPointName.apply(
                context
                  .stopPointsForServiceLinksInJourneyPattern()
                  .get(serviceLinkId),
                FromToScheduledStopPointId::to
              )
            )
          )
        )
      );
  }
}
