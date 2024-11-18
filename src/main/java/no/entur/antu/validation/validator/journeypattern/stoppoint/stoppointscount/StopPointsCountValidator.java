package no.entur.antu.validation.validator.journeypattern.stoppoint.stoppointscount;

import java.util.Objects;
import java.util.function.Predicate;
import no.entur.antu.validation.AntuNetexValidator;
import no.entur.antu.validation.RuleCode;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntryFactory;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates that the number of stop points in a journey pattern
 * should and should only be 1 more than the service links in
 * the journey pattern.
 * Chouette reference: 3-JourneyPattern-2
 */
public class StopPointsCountValidator extends AntuNetexValidator {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    StopPointsCountValidator.class
  );

  public StopPointsCountValidator(
    ValidationReportEntryFactory validationReportEntryFactory
  ) {
    super(validationReportEntryFactory);
  }

  @Override
  protected RuleCode[] getRuleCodes() {
    return StopPointsCountError.RuleCode.values();
  }

  @Override
  public void validateLineFile(
    ValidationReport validationReport,
    JAXBValidationContext validationContext
  ) {
    LOGGER.debug("Validating Stop points or service links In Journey Patterns");

    validationContext
      .journeyPatterns()
      .stream()
      .map(StopPointsCountContext::of)
      .filter(Objects::nonNull)
      .filter(Predicate.not(StopPointsCountContext::isValid))
      .forEach(stopPointsCountContext ->
        addValidationReportEntry(
          validationReport,
          validationContext,
          new StopPointsCountError(
            StopPointsCountError.RuleCode.INVALID_NUMBER_OF_STOP_POINTS_OR_LINKS_IN_JOURNEY_PATTERN,
            stopPointsCountContext.journeyPatternId(),
            stopPointsCountContext.stopPointsCount(),
            stopPointsCountContext.serviceLinksCount()
          )
        )
      );
  }

  @Override
  protected void validateCommonFile(
    ValidationReport validationReport,
    JAXBValidationContext validationContext
  ) {
    // JourneyPatterns only appear in the Line file.
  }
}
