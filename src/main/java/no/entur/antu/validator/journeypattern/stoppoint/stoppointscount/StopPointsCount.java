package no.entur.antu.validator.journeypattern.stoppoint.stoppointscount;

import java.util.Objects;
import java.util.function.Predicate;
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

public class StopPointsCount extends AntuNetexValidator {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    StopPointsCount.class
  );

  public StopPointsCount(
    ValidationReportEntryFactory validationReportEntryFactory
  ) {
    super(validationReportEntryFactory);
  }

  @Override
  protected RuleCode[] getRuleCodes() {
    return StopPointsCountError.RuleCode.values();
  }

  @Override
  public void validate(
    ValidationReport validationReport,
    ValidationContext validationContext
  ) {
    if (validationContext.isCommonFile()) {
      return;
    }

    LOGGER.debug("Validating Stop points or service links In Journey Patterns");

    if (
      validationContext instanceof ValidationContextWithNetexEntitiesIndex validationContextWithNetexEntitiesIndex
    ) {
      NetexEntitiesIndex index =
        validationContextWithNetexEntitiesIndex.getNetexEntitiesIndex();

      index
        .getJourneyPatternIndex()
        .getAll()
        .stream()
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
    } else {
      throw new AntuException(
        "Received invalid validation context in Validating stop points count"
      );
    }
  }
}
