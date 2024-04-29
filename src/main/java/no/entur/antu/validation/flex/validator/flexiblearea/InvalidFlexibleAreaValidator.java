package no.entur.antu.validation.flex.validator.flexiblearea;

import java.util.List;
import java.util.function.Consumer;
import no.entur.antu.commondata.CommonDataRepository;
import no.entur.antu.stop.StopPlaceRepository;
import no.entur.antu.validation.AntuNetexValidator;
import no.entur.antu.validation.RuleCode;
import no.entur.antu.validation.utilities.GeometryUtilities;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntryFactory;
import org.entur.netex.validation.validator.xpath.ValidationContext;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.operation.valid.IsValidOp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates the flexible areas.
 *   This rule checks for the multiple issues in the flexible areas.
 *   for example:
 *    Non-Closed Line string.
 *    Line string with too few coordinates.
 *    Self interacting linear ring.
 *    Incomplete coordinates.
 *    etc.
 */
public class InvalidFlexibleAreaValidator extends AntuNetexValidator {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    InvalidFlexibleAreaValidator.class
  );

  public InvalidFlexibleAreaValidator(
    ValidationReportEntryFactory validationReportEntryFactory,
    CommonDataRepository commonDataRepository,
    StopPlaceRepository stopPlaceRepository
  ) {
    super(
      validationReportEntryFactory,
      commonDataRepository,
      stopPlaceRepository
    );
  }

  @Override
  protected RuleCode[] getRuleCodes() {
    return InvalidFlexibleAreaError.RuleCode.values();
  }

  @Override
  public void validateCommonFile(
    ValidationReport validationReport,
    ValidationContext validationContext
  ) {
    LOGGER.debug("Validating flexible area");

    NetexEntitiesIndex netexEntitiesIndex = getNetexEntitiesIndex(
      validationContext
    );

    InvalidFlexibleAreaContextBuilder invalidFlexibleAreaContextBuilder =
      new InvalidFlexibleAreaContextBuilder();

    List<InvalidFlexibleAreaContextBuilder.InvalidFlexibleAreaContext> invalidFlexibleAreaContexts =
      invalidFlexibleAreaContextBuilder.build(netexEntitiesIndex);

    invalidFlexibleAreaContexts.forEach(invalidFlexibleAreaContext ->
      validateFlexibleArea(
        invalidFlexibleAreaContext,
        invalidFlexibleAreaError ->
          addValidationReportEntry(
            validationReport,
            validationContext,
            invalidFlexibleAreaError
          )
      )
    );
  }

  @Override
  protected void validateLineFile(
    ValidationReport validationReport,
    ValidationContext validationContext
  ) {
    // Flexible areas only appear in the Common file.
  }

  private void validateFlexibleArea(
    InvalidFlexibleAreaContextBuilder.InvalidFlexibleAreaContext invalidFlexibleAreaContext,
    Consumer<InvalidFlexibleAreaError> flexibleAreaError
  ) {
    if (
      !GeometryUtilities.isValidCoordinatesList(
        invalidFlexibleAreaContext.coordinates()
      )
    ) {
      flexibleAreaError.accept(
        new InvalidFlexibleAreaError(
          InvalidFlexibleAreaError.RuleCode.INVALID_FLEXIBLE_AREA,
          invalidFlexibleAreaContext.flexibleAreaId(),
          invalidFlexibleAreaContext.flexibleStopPlaceId(),
          "Incomplete coordinates"
        )
      );
      return;
    }

    try {
      LinearRing linearRing = GeometryUtilities.createLinerRing(
        invalidFlexibleAreaContext.coordinates()
      );

      IsValidOp isValidOp = new IsValidOp(linearRing);
      if (!isValidOp.isValid()) {
        flexibleAreaError.accept(
          new InvalidFlexibleAreaError(
            InvalidFlexibleAreaError.RuleCode.INVALID_FLEXIBLE_AREA,
            invalidFlexibleAreaContext.flexibleAreaId(),
            invalidFlexibleAreaContext.flexibleStopPlaceId(),
            isValidOp.getValidationError().toString()
          )
        );
      }
    } catch (Exception ex) {
      flexibleAreaError.accept(
        new InvalidFlexibleAreaError(
          InvalidFlexibleAreaError.RuleCode.INVALID_FLEXIBLE_AREA,
          invalidFlexibleAreaContext.flexibleAreaId(),
          invalidFlexibleAreaContext.flexibleStopPlaceId(),
          ex.getMessage()
        )
      );
    }
  }
}
