package no.entur.antu.validation.validator.journeypattern.stoppoint.identicalstoppoints;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.entur.netex.validation.validator.Severity;
import org.entur.netex.validation.validator.ValidationIssue;
import org.entur.netex.validation.validator.ValidationRule;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
import org.entur.netex.validation.validator.jaxb.JAXBValidator;

/**
 * Validate that the same stop points are not used
 * in multiple journey patterns.
 * If the same stop points are used in multiple journey patterns,
 * it is an error.
 * Chouette reference: 3-JourneyPattern-rutebanken-3
 */
public class IdenticalStopPointsValidator implements JAXBValidator {

  static final ValidationRule RULE = new ValidationRule(
    "IDENTICAL_STOP_POINTS_IN_JOURNEY_PATTERNS",
    "Identical stop points in journey patterns",
    "Identical stop points in journey patterns:  [%s]",
    Severity.INFO
  );

  @Override
  public List<ValidationIssue> validate(
    JAXBValidationContext validationContext
  ) {
    IdenticalStopPointsContext.Builder builder =
      IdenticalStopPointsContext.builder(validationContext);

    return validationContext
      .journeyPatterns()
      .stream()
      .map(builder::build)
      .collect(
        // Two IdenticalStopPointsContexts are equal if their StopPointsContexts are equal
        Collectors.groupingBy(Function.identity(), Collectors.toList())
      )
      .values()
      .stream()
      .filter(identicalStopPointsContexts ->
        identicalStopPointsContexts.size() > 1
      )
      .map(identicalStopPointsContexts ->
        new ValidationIssue(
          RULE,
          validationContext.dataLocation(
            identicalStopPointsContexts.get(0).journeyPatternId()
          ),
          String.join(
            ", ",
            identicalStopPointsContexts
              .stream()
              .map(IdenticalStopPointsContext::journeyPatternId)
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
