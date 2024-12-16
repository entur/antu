package no.entur.antu.validation.validator.journeypattern.stoppoint.samestoppoints;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.entur.netex.validation.validator.Severity;
import org.entur.netex.validation.validator.ValidationIssue;
import org.entur.netex.validation.validator.ValidationRule;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
import org.entur.netex.validation.validator.jaxb.JAXBValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validate that the same stop points are not used
 * in multiple journey patterns.
 * If the same stop points are used in multiple journey patterns,
 * it is an error.
 * Chouette reference: 3-JourneyPattern-1
 */
public class SameStopPointsValidator implements JAXBValidator {

  static final ValidationRule RULE = new ValidationRule(
    "SAME_STOP_POINT_IN_JOURNEY_PATTERNS",
    "JourneyPatterns have same StopPoints",
    "JourneyPatterns have same StopPoints: [%s]",
    Severity.WARNING
  );

  private static final Logger LOGGER = LoggerFactory.getLogger(
    SameStopPointsValidator.class
  );

  @Override
  public List<ValidationIssue> validate(
    JAXBValidationContext validationContext
  ) {
    LOGGER.debug("Validating Same Stops In Journey Patterns");

    return validationContext
      .journeyPatterns()
      .stream()
      .map(SameStopPointsContext::of)
      .collect(
        // Two SameStopPointsContexts are equal if their Stop points are equal
        Collectors.groupingBy(Function.identity(), Collectors.toList())
      )
      .values()
      .stream()
      .filter(sameStopPointsContexts -> sameStopPointsContexts.size() > 1)
      .map(sameStopPointsContexts ->
        new ValidationIssue(
          RULE,
          validationContext.dataLocation(
            sameStopPointsContexts.get(0).journeyPatternId()
          ),
          String.join(
            ", ",
            sameStopPointsContexts
              .stream()
              .map(SameStopPointsContext::journeyPatternId)
              .toList()
          )
        )
      )
      .toList();
  }

  @Override
  public Set<ValidationRule> getRules() {
    return Set.of(RULE);
  }
}
