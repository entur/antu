package no.entur.antu.validation.validator.interchange.mandatoryfields;

import java.util.function.Consumer;
import no.entur.antu.validation.AntuNetexData;
import no.entur.antu.validation.AntuNetexValidator;
import no.entur.antu.validation.RuleCode;
import no.entur.antu.validation.ValidationError;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntryFactory;
import org.entur.netex.validation.validator.xpath.ValidationContext;

/**
 * Validator for mandatory fields in interchange.
 * Chouette reference:
 *  3-Interchange-1,
 *  3-Interchange-2,
 *  3-Interchange-3,
 *  3-Interchange-4
 */
public class MandatoryFieldsValidator extends AntuNetexValidator {

  public MandatoryFieldsValidator(
    ValidationReportEntryFactory validationReportEntryFactory
  ) {
    super(validationReportEntryFactory);
  }

  @Override
  protected RuleCode[] getRuleCodes() {
    return MandatoryFieldsError.RuleCode.values();
  }

  @Override
  protected void validateCommonFile(
    ValidationReport validationReport,
    ValidationContext validationContext,
    AntuNetexData antuNetexData
  ) {
    // ServiceJourneyInterchanges exists only in line files,
    // as they have reference to serviceJourneys.
  }

  @Override
  protected void validateLineFile(
    ValidationReport validationReport,
    ValidationContext validationContext,
    AntuNetexData antuNetexData
  ) {
    antuNetexData
      .serviceJourneyInterchanges()
      .map(MandatoryFieldsContext::of)
      .forEach(serviceJourneyInterchange ->
        validateMandatoryFields(
          serviceJourneyInterchange,
          validationError ->
            addValidationReportEntry(
              validationReport,
              validationContext,
              validationError
            )
        )
      );
  }

  private void validateMandatoryFields(
    MandatoryFieldsContext context,
    Consumer<ValidationError> reportError
  ) {
    // 3-Interchange-1
    if (context.fromPointRef() == null) {
      reportError.accept(
        new MandatoryFieldsError(
          MandatoryFieldsError.RuleCode.MISSING_FROM_STOP_POINT_IN_INTERCHANGE,
          context.interchangeId()
        )
      );
    }

    // 3-Interchange-2
    if (context.toPointRef() == null) {
      reportError.accept(
        new MandatoryFieldsError(
          MandatoryFieldsError.RuleCode.MISSING_TO_STOP_POINT_IN_INTERCHANGE,
          context.interchangeId()
        )
      );
    }

    // 3-Interchange-3
    if (context.fromJourneyRef() == null) {
      reportError.accept(
        new MandatoryFieldsError(
          MandatoryFieldsError.RuleCode.MISSING_FROM_SERVICE_JOURNEY_IN_INTERCHANGE,
          context.interchangeId()
        )
      );
    }

    // 3-Interchange-4
    if (context.toJourneyRef() == null) {
      reportError.accept(
        new MandatoryFieldsError(
          MandatoryFieldsError.RuleCode.MISSING_TO_SERVICE_JOURNEY_IN_INTERCHANGE,
          context.interchangeId()
        )
      );
    }
  }
}
