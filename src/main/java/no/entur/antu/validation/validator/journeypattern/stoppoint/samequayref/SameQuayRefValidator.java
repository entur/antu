package no.entur.antu.validation.validator.journeypattern.stoppoint.samequayref;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import no.entur.antu.validation.AntuNetexValidator;
import no.entur.antu.validation.RuleCode;
import no.entur.antu.validation.ValidationError;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntryFactory;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validate that the scheduled stop points of two consecutive stop points in journey patterns,
 * does not assigned to same quay.
 * Chouette reference: 3-JourneyPattern-rutebanken-2
 */
public class SameQuayRefValidator extends AntuNetexValidator {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    SameQuayRefValidator.class
  );

  public SameQuayRefValidator(
    ValidationReportEntryFactory validationReportEntryFactory
  ) {
    super(validationReportEntryFactory);
  }

  @Override
  protected RuleCode[] getRuleCodes() {
    return SameQuayRefError.RuleCode.values();
  }

  @Override
  public void validateLineFile(
    ValidationReport validationReport,
    JAXBValidationContext validationContext
  ) {
    LOGGER.debug(
      "Validating Same quayRefs in two consecutive Stop points In Journey Patterns"
    );

    SameQuayRefContext.Builder builder = SameQuayRefContext.builder(
      validationContext
    );

    validationContext
      .journeyPatterns()
      .stream()
      .map(builder::build)
      .forEach(sameQuayRefContexts ->
        validateSameQuayRefs(
          validationContext,
          sameQuayRefContexts,
          error ->
            addValidationReportEntry(validationReport, validationContext, error)
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

  private void validateSameQuayRefs(
    JAXBValidationContext validationContext,
    List<SameQuayRefContext> contextForJourneyPattern,
    Consumer<ValidationError> reportError
  ) {
    IntStream
      .range(1, contextForJourneyPattern.size())
      .forEach(i -> {
        SameQuayRefContext currentContext = contextForJourneyPattern.get(i);
        SameQuayRefContext previousContext = contextForJourneyPattern.get(
          i - 1
        );

        if (!currentContext.isValid() || !previousContext.isValid()) {
          LOGGER.debug(
            "Either scheduled stop point id or quay id missing. Ignoring the validation"
          );
          return;
        }

        if (currentContext.quayId().equals(previousContext.quayId())) {
          reportError.accept(
            new SameQuayRefError(
              SameQuayRefError.RuleCode.SAME_QUAY_REF_IN_CONSECUTIVE_STOP_POINTS_IN_JOURNEY_PATTERN,
              currentContext.journeyPatternId(),
              validationContext.stopPointName(
                previousContext.scheduledStopPointId()
              ),
              validationContext.stopPointName(
                currentContext.scheduledStopPointId()
              )
            )
          );
        }
      });
  }
}
