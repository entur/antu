package no.entur.antu.validation.validator.journeypattern.stoppoint.samequayref;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import no.entur.antu.commondata.CommonDataRepository;
import no.entur.antu.stop.StopPlaceRepository;
import no.entur.antu.validation.AntuNetexData;
import no.entur.antu.validation.AntuNetexValidator;
import no.entur.antu.validation.RuleCode;
import no.entur.antu.validation.ValidationError;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntryFactory;
import org.entur.netex.validation.validator.xpath.ValidationContext;
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
    ValidationReportEntryFactory validationReportEntryFactory,
    CommonDataRepository commonDataRepository,
    StopPlaceRepository stopPlaceRepository
  ) {
    super(
      validationReportEntryFactory,
      commonDataRepository,
      stopPlaceRepository
    );
  }

  @Override
  protected RuleCode[] getRuleCodes() {
    return SameQuayRefError.RuleCode.values();
  }

  @Override
  public void validateLineFile(
    ValidationReport validationReport,
    ValidationContext validationContext,
    AntuNetexData antuNetexData
  ) {
    LOGGER.debug(
      "Validating Same quayRefs in two consecutive Stop points In Journey Patterns"
    );

    SameQuayRefContext.Builder builder = SameQuayRefContext.builder(
      antuNetexData
    );

    antuNetexData
      .journeyPatterns()
      .map(builder::build)
      .forEach(sameQuayRefContexts ->
        validateSameQuayRefs(
          antuNetexData,
          sameQuayRefContexts,
          error ->
            addValidationReportEntry(validationReport, validationContext, error)
        )
      );
  }

  @Override
  protected void validateCommonFile(
    ValidationReport validationReport,
    ValidationContext validationContext,
    AntuNetexData antuNetexData
  ) {
    // JourneyPatterns only appear in the Line file.
  }

  private void validateSameQuayRefs(
    AntuNetexData antuNetexData,
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
              antuNetexData.getStopPointName(
                previousContext.scheduledStopPointId()
              ),
              antuNetexData.getStopPointName(
                currentContext.scheduledStopPointId()
              )
            )
          );
        }
      });
  }
}
