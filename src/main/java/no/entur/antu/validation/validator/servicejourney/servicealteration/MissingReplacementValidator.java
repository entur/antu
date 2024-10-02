package no.entur.antu.validation.validator.servicejourney.servicealteration;

import java.util.List;
import java.util.function.Predicate;
import no.entur.antu.validation.AntuNetexData;
import no.entur.antu.validation.AntuNetexValidator;
import no.entur.antu.validation.RuleCode;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntryFactory;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
import org.rutebanken.netex.model.VersionOfObjectRefStructure;

/**
 * Validates that the service alteration of a service journey is correct.
 * This means that the service alteration of a service journey that got replaced must be 'replaced'.
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
    JAXBValidationContext validationContext,
    AntuNetexData antuNetexData
  ) {
    List<String> datedServiceJourneyRefsToReplacedDatedServiceJourneys =
      antuNetexData
        .datedServiceJourneysWithReferenceToReplaced()
        .map(antuNetexData::datedServiceJourneyRef)
        .map(VersionOfObjectRefStructure::getRef)
        .toList();

    // Validate that those DSJs that got replaced have a replacement.
    antuNetexData
      .replacedDatedServiceJourneys()
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
