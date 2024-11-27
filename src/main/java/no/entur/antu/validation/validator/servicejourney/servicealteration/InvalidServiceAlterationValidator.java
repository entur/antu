package no.entur.antu.validation.validator.servicejourney.servicealteration;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import no.entur.antu.validation.validator.servicejourney.servicealteration.support.ServiceAlterationUtils;
import org.entur.netex.validation.validator.Severity;
import org.entur.netex.validation.validator.ValidationIssue;
import org.entur.netex.validation.validator.ValidationRule;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
import org.entur.netex.validation.validator.jaxb.JAXBValidator;
import org.rutebanken.netex.model.ServiceAlterationEnumeration;

/**
 * Validates that the service alteration of a service journey is correct.
 * This means that the service alteration of a service journey that got replaced must be 'replaced'.
 */
public class InvalidServiceAlterationValidator implements JAXBValidator {

  static final ValidationRule RULE = new ValidationRule(
    "INVALID_SERVICE_ALTERATION",
    "Invalid/Missing ServiceAlteration",
    "Invalid ServiceAlteration. . Expected 'replaced', but was %s",
    Severity.WARNING
  );

  @Override
  public List<ValidationIssue> validate(
    JAXBValidationContext validationContext
  ) {
    // Validate that those DSJs that got replaced have the 'replaced' service alteration.
    return ServiceAlterationUtils
      .datedServiceJourneysWithReferenceToOriginal(validationContext)
      .stream()
      .map(validationContext::originalDatedServiceJourney)
      .filter(Objects::nonNull)
      .filter(dsj ->
        dsj.getServiceAlteration() != ServiceAlterationEnumeration.REPLACED
      )
      .map(dsj ->
        new ValidationIssue(
          RULE,
          validationContext.dataLocation(dsj.getId()),
          dsj.getServiceAlteration() == null
            ? "not provided"
            : dsj.getServiceAlteration().value()
        )
      )
      .toList();
  }

  @Override
  public Set<ValidationRule> getRules() {
    return Set.of(RULE);
  }
}
