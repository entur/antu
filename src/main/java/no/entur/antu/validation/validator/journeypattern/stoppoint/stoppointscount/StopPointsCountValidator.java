package no.entur.antu.validation.validator.journeypattern.stoppoint.stoppointscount;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import org.entur.netex.validation.validator.Severity;
import org.entur.netex.validation.validator.ValidationIssue;
import org.entur.netex.validation.validator.ValidationRule;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
import org.entur.netex.validation.validator.jaxb.JAXBValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates that the number of stop points in a journey pattern
 * should and should only be 1 more than the service links in
 * the journey pattern.
 * Chouette reference: 3-JourneyPattern-2
 */
public class StopPointsCountValidator implements JAXBValidator {

  static final ValidationRule RULE = new ValidationRule(
    "INVALID_NUMBER_OF_STOP_POINTS_OR_LINKS_IN_JOURNEY_PATTERN",
    "Invalid number of stop points or links in JourneyPattern",
    "Invalid number of Stop points or links in JourneyPattern, Number of stop points = %d, Number of links = %d",
    Severity.ERROR
  );

  private static final Logger LOGGER = LoggerFactory.getLogger(
    StopPointsCountValidator.class
  );

  @Override
  public List<ValidationIssue> validate(
    JAXBValidationContext validationContext
  ) {
    LOGGER.debug("Validating Stop points or service links In Journey Patterns");

    return validationContext
      .journeyPatterns()
      .stream()
      .map(StopPointsCountContext::of)
      .filter(Objects::nonNull)
      .filter(Predicate.not(StopPointsCountContext::isValid))
      .map(stopPointsCountContext ->
        new ValidationIssue(
          RULE,
          validationContext.dataLocation(
            stopPointsCountContext.journeyPatternId()
          ),
          stopPointsCountContext.stopPointsCount(),
          stopPointsCountContext.serviceLinksCount()
        )
      )
      .toList();
  }

  @Override
  public Set<ValidationRule> getRules() {
    return Set.of(RULE);
  }
}
