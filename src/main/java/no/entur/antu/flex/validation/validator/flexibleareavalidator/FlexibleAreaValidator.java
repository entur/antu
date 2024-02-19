package no.entur.antu.flex.validation.validator.flexibleareavalidator;

import static no.entur.antu.flex.validation.validator.flexibleareavalidator.FlexibleAreaError.RuleCode.INVALID_FLEXIBLE_AREA;

import java.util.List;
import no.entur.antu.exception.AntuException;
import no.entur.antu.validator.AntuNetexValidator;
import no.entur.antu.validator.RuleCode;
import no.entur.antu.validator.ValidationContextWithNetexEntitiesIndex;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntryFactory;
import org.entur.netex.validation.validator.xpath.ValidationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlexibleAreaValidator extends AntuNetexValidator {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    FlexibleAreaValidator.class
  );

  protected FlexibleAreaValidator(
    ValidationReportEntryFactory validationReportEntryFactory
  ) {
    super(validationReportEntryFactory);
  }

  @Override
  protected RuleCode[] getRuleCodes() {
    return FlexibleAreaError.RuleCode.values();
  }

  @Override
  public void validate(
    ValidationReport validationReport,
    ValidationContext validationContext
  ) {
    LOGGER.debug("Validating flexible area");

    if (!validationContext.isCommonFile()) {
      return;
    }

    if (
      validationContext instanceof ValidationContextWithNetexEntitiesIndex validationContextWithNetexEntitiesIndex
    ) {
      NetexEntitiesIndex index =
        validationContextWithNetexEntitiesIndex.getNetexEntitiesIndex();

      FlexibleAreaContextBuilder flexibleAreaContextBuilder =
        new FlexibleAreaContextBuilder();

      List<FlexibleAreaContextBuilder.FlexibleAreaContext> flexibleAreaContexts =
        flexibleAreaContextBuilder.build(index.getFlexibleStopPlaceIndex());

      flexibleAreaContexts
        .stream()
        .filter(flexibleAreaContext ->
          !flexibleAreaContext.linearRing().isValid()
        )
        .map(flexibleAreaContext ->
          new FlexibleAreaError(
            INVALID_FLEXIBLE_AREA,
            flexibleAreaContext.flexibleAreaId(),
            flexibleAreaContext.flexibleStopPlaceId()
          )
        )
        .forEach(flexibleAreaError ->
          addValidationReportEntry(
            validationReport,
            validationContext,
            flexibleAreaError
          )
        );
    } else {
      throw new AntuException(
        "Received invalid validation context in flexible area validator"
      );
    }
  }
}
