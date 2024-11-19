package no.entur.antu.validation.validator.servicejourney.servicealteration;

import java.util.List;
import java.util.function.Predicate;
import no.entur.antu.validation.AntuNetexValidator;
import no.entur.antu.validation.RuleCode;
import no.entur.antu.validation.validator.servicejourney.servicealteration.support.ServiceAlterationUtils;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntryFactory;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
import org.entur.netex.validation.validator.jaxb.support.DatedServiceJourneyUtils;
import org.rutebanken.netex.model.ServiceAlterationEnumeration;

/**
 * Validates that the replacement for the service alteration exists.
 * This means that the service alteration of a service journey that got replaced must have a replacement.
 */
public class MissingReplacementValidator extends AntuNetexValidator {

  public MissingReplacementValidator(
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
    List<String> datedServiceJourneyRefsToReplacedDatedServiceJourneys =
      ServiceAlterationUtils
        .datedServiceJourneysWithReferenceToOriginal(validationContext)
        .stream()
        .map(DatedServiceJourneyUtils::originalDatedServiceJourneyRef)
        .toList();

    // Validate that those DSJs that got replaced have a replacement.
    validationContext
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
      .forEach(dsj ->
        addValidationReportEntry(
          validationReport,
          validationContext,
          new MissingReplacementError(
            dsj.getId(),
            MissingReplacementError.RuleCode.MISSING_REPLACEMENT
          )
        )
      );
  }
}
