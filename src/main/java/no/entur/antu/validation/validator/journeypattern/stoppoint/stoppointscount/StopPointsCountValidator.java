package no.entur.antu.validation.validator.journeypattern.stoppoint.stoppointscount;

import java.util.Objects;
import java.util.function.Predicate;
import no.entur.antu.commondata.CommonDataRepository;
import no.entur.antu.stop.StopPlaceRepository;
import no.entur.antu.validation.AntuNetexData;
import no.entur.antu.validation.AntuNetexValidator;
import no.entur.antu.validation.RuleCode;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntryFactory;
import org.entur.netex.validation.validator.xpath.ValidationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates that the number of stop points in a journey pattern
 * should and should only be 1 more than the service links in
 * the journey pattern.
 */
public class StopPointsCountValidator extends AntuNetexValidator {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    StopPointsCountValidator.class
  );

  public StopPointsCountValidator(
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
    return StopPointsCountError.RuleCode.values();
  }

  @Override
  public void validateLineFile(
    ValidationReport validationReport,
    ValidationContext validationContext
  ) {
    LOGGER.debug("Validating Stop points or service links In Journey Patterns");

    AntuNetexData antuNetexData = createAntuNetexData(
      validationReport,
      validationContext
    );

    antuNetexData
      .journeyPatterns()
      .map(StopPointsCountContext::of)
      .filter(Objects::nonNull)
      .filter(Predicate.not(StopPointsCountContext::isValid))
      .forEach(stopPointsCountContext ->
        addValidationReportEntry(
          validationReport,
          validationContext,
          new StopPointsCountError(
            StopPointsCountError.RuleCode.INVALID_NUMBER_OF_STOP_POINTS_IN_JOURNEY_PATTERN,
            stopPointsCountContext.journeyPatternId()
          )
        )
      );
  }

  @Override
  protected void validateCommonFile(
    ValidationReport validationReport,
    ValidationContext validationContext
  ) {
    // JourneyPatterns only appear in the Line file.
  }
}
