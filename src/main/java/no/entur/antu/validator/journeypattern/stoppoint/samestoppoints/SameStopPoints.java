package no.entur.antu.validator.journeypattern.stoppoint.samestoppoints;

import java.util.function.Function;
import java.util.stream.Collectors;
import no.entur.antu.exception.AntuException;
import no.entur.antu.validator.AntuNetexValidator;
import no.entur.antu.validator.RuleCode;
import no.entur.antu.validator.ValidationContextWithNetexEntitiesIndex;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntryFactory;
import org.entur.netex.validation.validator.xpath.ValidationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SameStopPoints extends AntuNetexValidator {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    SameStopPoints.class
  );

  public SameStopPoints(
    ValidationReportEntryFactory validationReportEntryFactory
  ) {
    super(validationReportEntryFactory);
  }

  @Override
  protected RuleCode[] getRuleCodes() {
    return SameStopPointsError.RuleCode.values();
  }

  @Override
  public void validate(
    ValidationReport validationReport,
    ValidationContext validationContext
  ) {
    if (validationContext.isCommonFile()) {
      return;
    }

    LOGGER.debug("Validating Same Stops In Journey Patterns");

    if (
      validationContext instanceof ValidationContextWithNetexEntitiesIndex validationContextWithNetexEntitiesIndex
    ) {
      NetexEntitiesIndex index =
        validationContextWithNetexEntitiesIndex.getNetexEntitiesIndex();

      index
        .getJourneyPatternIndex()
        .getAll()
        .stream()
        .map(SameStopPointsContext::of)
        .collect(
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
    } else {
      throw new AntuException(
        "Received invalid validation context in Validating same stop points in journey patterns"
      );
    }
  }
}
