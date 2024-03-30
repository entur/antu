package no.entur.antu.validation.validator.journeypattern.stoppoint.identicalstoppoints;

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
 * Chouette reference: 3-JourneyPattern-rutebanken-3
 */
public class IdenticalStopPointsValidator extends AntuNetexValidator {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    IdenticalStopPointsValidator.class
  );

  public IdenticalStopPointsValidator(
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
    return IdenticalStopPointsError.RuleCode.values();
  }

  @Override
  public void validateLineFile(
    ValidationReport validationReport,
    ValidationContext validationContext
  ) {
    LOGGER.debug("Validating identical Journey Patterns");

    NetexEntitiesIndex index = getNetexEntitiesIndex(validationContext);

    AntuNetexData antuNetexData = createAntuNetexData(
      validationReport,
      validationContext
    );

    IdenticalStopPointsContext.Builder builder =
      IdenticalStopPointsContext.builder(antuNetexData);

    index
      .getJourneyPatternIndex()
      .getAll()
      .stream()
      .map(builder::build)
      .collect(
        // Two IdenticalStopPointsContexts are equal if their StopPointsContexts are equal
        Collectors.groupingBy(Function.identity(), Collectors.toList())
      )
      .entrySet()
      .stream()
      .filter(entry -> entry.getValue().size() > 1)
      .forEach(entry ->
        addValidationReportEntry(
          validationReport,
          validationContext,
          new IdenticalStopPointsError(
            IdenticalStopPointsError.RuleCode.IDENTICAL_STOP_POINTS_IN_JOURNEY_PATTERNS,
            entry
              .getValue()
              .stream()
              .map(IdenticalStopPointsContext::journeyPatternId)
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
