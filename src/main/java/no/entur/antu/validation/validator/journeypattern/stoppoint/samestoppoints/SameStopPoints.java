package no.entur.antu.validation.validator.journeypattern.stoppoint.samestoppoints;

import java.util.function.Function;
import java.util.stream.Collectors;
import no.entur.antu.commondata.CommonDataRepository;
import no.entur.antu.stop.StopPlaceRepository;
import no.entur.antu.validation.AntuNetexData;
import no.entur.antu.validation.AntuNetexValidator;
import no.entur.antu.validation.RuleCode;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntryFactory;
import org.entur.netex.validation.validator.xpath.ValidationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validate that the same stop points are not used
 * in multiple journey patterns.
 * If the same stop points are used in multiple journey patterns,
 * it is an error.
 */
public class SameStopPoints extends AntuNetexValidator {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    SameStopPoints.class
  );

  public SameStopPoints(
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
    return SameStopPointsError.RuleCode.values();
  }

  @Override
  public void validateLineFile(
    ValidationReport validationReport,
    ValidationContext validationContext
  ) {
    LOGGER.debug("Validating Same Stops In Journey Patterns");

    AntuNetexData antuNetexData = createAntuNetexData(
      validationReport,
      validationContext
    );

    antuNetexData
      .journeyPatterns()
      .map(SameStopPointsContext::of)
      .collect(
        // Two SameStopPointsContexts are equal if their Stop points are equal
        Collectors.groupingBy(Function.identity(), Collectors.toList())
      )
      .entrySet()
      .stream()
      .filter(entry -> entry.getValue().size() > 1)
      .forEach(entry ->
        addValidationReportEntry(
          validationReport,
          validationContext,
          new SameStopPointsError(
            SameStopPointsError.RuleCode.SAME_STOP_POINT_IN_JOURNEY_PATTERNS,
            entry
              .getValue()
              .stream()
              .map(SameStopPointsContext::journeyPatternId)
              .toList()
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
