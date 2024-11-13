package no.entur.antu.validation.validator.interchange.duplicate;

import java.util.function.Function;
import java.util.stream.Collectors;
import no.entur.antu.validation.AntuNetexValidator;
import no.entur.antu.validation.RuleCode;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntryFactory;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;

/**
 * Validate and warn for the duplicate interchanges.
 * Two interchanges are considered duplicate if they have the same
 * FromPointRef, ToPointRef, FromJourneyRef, ToJourneyRef
 * <p>
 * Chouette reference: 3-Interchange-5
 *
 * TODO: the validator should report the two identical interchanges, not only the extra one.
 *
 */
public class DuplicateInterchangesValidator extends AntuNetexValidator {

  public DuplicateInterchangesValidator(
    ValidationReportEntryFactory validationReportEntryFactory
  ) {
    super(validationReportEntryFactory);
  }

  @Override
  protected RuleCode[] getRuleCodes() {
    return DuplicateInterchangesError.RuleCode.values();
  }

  @Override
  protected void validateCommonFile(
    ValidationReport validationReport,
    JAXBValidationContext validationContext
  ) {
    // ServiceJourneyInterchanges exists only in line files,
    // as they have reference to serviceJourneys.
  }

  @Override
  protected void validateLineFile(
    ValidationReport validationReport,
    JAXBValidationContext validationContext
  ) {
    validationContext
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
      .forEach(duplicateContexts ->
        addValidationReportEntry(
          validationReport,
          validationContext,
          new DuplicateInterchangesError(
            DuplicateInterchangesError.RuleCode.DUPLICATE_INTERCHANGES,
            duplicateContexts.getKey().interchangeId(),
            duplicateContexts
              .getValue()
              .stream()
              .map(DuplicateInterchangesContext::interchangeId)
              .filter(id ->
                !id.equals(duplicateContexts.getKey().interchangeId())
              )
              .toList()
          )
        )
      );
  }
}
