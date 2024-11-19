package no.entur.antu.validation.validator.servicejourney.servicealteration;

import java.util.Objects;
import no.entur.antu.validation.AntuNetexValidator;
import no.entur.antu.validation.RuleCode;
import no.entur.antu.validation.validator.servicejourney.servicealteration.support.ServiceAlterationUtils;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntryFactory;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
import org.rutebanken.netex.model.ServiceAlterationEnumeration;

/**
 * Validates that the service alteration of a service journey is correct.
 * This means that the service alteration of a service journey that got replaced must be 'replaced'.
 */
public class InvalidServiceAlterationValidator extends AntuNetexValidator {

  public InvalidServiceAlterationValidator(
    ValidationReportEntryFactory validationReportEntryFactory
  ) {
    super(validationReportEntryFactory);
  }

  @Override
  protected RuleCode[] getRuleCodes() {
    return InvalidServiceAlterationError.RuleCode.values();
  }

  @Override
  protected void validateLineFile(
    ValidationReport validationReport,
    JAXBValidationContext validationContext
  ) {
    // Validate that those DSJs that got replaced have the 'replaced' service alteration.
    ServiceAlterationUtils
      .datedServiceJourneysWithReferenceToOriginal(validationContext)
      .stream()
      .map(validationContext::originalDatedServiceJourney)
      .filter(Objects::nonNull)
      .filter(dsj ->
        dsj.getServiceAlteration() != ServiceAlterationEnumeration.REPLACED
      )
      .forEach(dsj ->
        addValidationReportEntry(
          validationReport,
          validationContext,
          new InvalidServiceAlterationError(
            dsj.getId(),
            InvalidServiceAlterationError.RuleCode.INVALID_SERVICE_ALTERATION,
            dsj.getServiceAlteration()
          )
        )
      );
  }
}
