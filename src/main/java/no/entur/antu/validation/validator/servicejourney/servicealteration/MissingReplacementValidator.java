package no.entur.antu.validation.validator.servicejourney.servicealteration;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import no.entur.antu.validation.validator.servicejourney.servicealteration.support.ServiceAlterationUtils;
import org.entur.netex.validation.validator.Severity;
import org.entur.netex.validation.validator.ValidationIssue;
import org.entur.netex.validation.validator.ValidationRule;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
import org.entur.netex.validation.validator.jaxb.JAXBValidator;
import org.entur.netex.validation.validator.jaxb.support.DatedServiceJourneyUtils;
import org.rutebanken.netex.model.ServiceAlterationEnumeration;

/**
 * Validates that the replacement for the service alteration exists.
 * This means that the service alteration of a service journey that got replaced must have a replacement.
 */
public class MissingReplacementValidator implements JAXBValidator {

  static final ValidationRule RULE = new ValidationRule(
    "MISSING_REPLACEMENT",
    "Missing replacement for ServiceAlteration",
    "No replacement found",
    Severity.WARNING
  );

  @Override
  public List<ValidationIssue> validate(
    JAXBValidationContext validationContext
  ) {
    List<String> datedServiceJourneyRefsToReplacedDatedServiceJourneys =
      ServiceAlterationUtils
        .datedServiceJourneysWithReferenceToOriginal(validationContext)
        .stream()
        .map(DatedServiceJourneyUtils::originalDatedServiceJourneyRef)
        .toList();

    // Validate that those DSJs that got replaced have a replacement.
    return validationContext
      .datedServiceJourneysByServiceAlteration(
        ServiceAlterationEnumeration.REPLACED
      )
      .stream()
      .filter(
        Predicate.not(dsj ->
          datedServiceJourneyRefsToReplacedDatedServiceJourneys.contains(
            dsj.getId()
          )
        )
      )
      .map(dsj ->
        new ValidationIssue(RULE, validationContext.dataLocation(dsj.getId()))
      )
      .toList();
  }

  @Override
  public Set<ValidationRule> getRules() {
    return Set.of(RULE);
  }
}
