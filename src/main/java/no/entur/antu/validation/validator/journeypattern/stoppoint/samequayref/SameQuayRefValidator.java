package no.entur.antu.validation.validator.journeypattern.stoppoint.samequayref;

import static org.entur.netex.validation.validator.Severity.WARNING;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.entur.netex.validation.validator.ValidationIssue;
import org.entur.netex.validation.validator.ValidationRule;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
import org.entur.netex.validation.validator.jaxb.JAXBValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validate that the scheduled stop points of two consecutive stop points in journey patterns,
 * does not assigned to same quay.
 * Chouette reference: 3-JourneyPattern-rutebanken-2
 */
public class SameQuayRefValidator implements JAXBValidator {

  static final ValidationRule RULE = new ValidationRule(
    "SAME_QUAY_REF_IN_CONSECUTIVE_STOP_POINTS_IN_JOURNEY_PATTERN",
    "Same quay refs in consecutive stop points",
    "Same quay refs in consecutive stop points [%s, %s] in journey pattern %s",
    WARNING
  );

  private static final Logger LOGGER = LoggerFactory.getLogger(
    SameQuayRefValidator.class
  );

  @Override
  public List<ValidationIssue> validate(
    JAXBValidationContext validationContext
  ) {
    LOGGER.debug(
      "Validating Same quayRefs in two consecutive Stop points In Journey Patterns"
    );

    SameQuayRefContext.Builder builder = SameQuayRefContext.builder(
      validationContext
    );

    return validationContext
      .journeyPatterns()
      .stream()
      .map(builder::build)
      .map(sameQuayRefContexts ->
        validateSameQuayRefs(validationContext, sameQuayRefContexts)
      )
      .flatMap(Collection::stream)
      .toList();
  }

  @Override
  public Set<ValidationRule> getRules() {
    return Set.of(RULE);
  }

  private List<ValidationIssue> validateSameQuayRefs(
    JAXBValidationContext validationContext,
    List<SameQuayRefContext> contextForJourneyPattern
  ) {
    List<ValidationIssue> issues = new ArrayList<>();

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
          issues.add(
            new ValidationIssue(
              RULE,
              validationContext.dataLocation(currentContext.journeyPatternId()),
              validationContext.stopPointName(
                previousContext.scheduledStopPointId()
              ),
              validationContext.stopPointName(
                currentContext.scheduledStopPointId()
              ),
              currentContext.journeyPatternId()
            )
          );
        }
      });
    return issues;
  }
}
