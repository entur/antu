package no.entur.antu.validation.validator.interchange.duplicate;

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
 * Validate and warn for the duplicate interchanges.
 * Two interchanges are considered duplicate if they have the same
 * FromPointRef, ToPointRef, FromJourneyRef, ToJourneyRef
 * <p>
 * Chouette reference: 3-Interchange-5
 *
 *
 */
public class DuplicateInterchangesValidator implements JAXBValidator {

  static final ValidationRule RULE = new ValidationRule(
    "DUPLICATE_INTERCHANGES",
    "Duplicate Interchanges found",
    "Duplicate interchanges found at %s",
    Severity.WARNING
  );

  @Override
  public List<ValidationIssue> validate(
    JAXBValidationContext validationContext
  ) {
    return validationContext
      .serviceJourneyInterchanges()
      .stream()
      .map(DuplicateInterchangesContext::of)
      .collect(
        // Two DuplicateInterchangesContext are equal if their InterchangeContexts are equal
        Collectors.groupingBy(Function.identity(), Collectors.toList())
      )
      .entrySet()
      .stream()
      .filter(entry -> entry.getValue().size() > 1)
      .map(duplicateContext ->
        new ValidationIssue(
          RULE,
          validationContext.dataLocation(
            duplicateContext.getKey().interchangeId()
          ),
          String.join(
            ", ",
            duplicateContext
              .getValue()
              .stream()
              .map(DuplicateInterchangesContext::interchangeId)
              .filter(id ->
                !id.equals(duplicateContext.getKey().interchangeId())
              )
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
