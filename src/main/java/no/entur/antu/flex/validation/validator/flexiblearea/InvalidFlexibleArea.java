package no.entur.antu.flex.validation.validator.flexiblearea;

import static no.entur.antu.flex.validation.validator.flexiblearea.InvalidFlexibleAreaError.RuleCode.INVALID_FLEXIBLE_AREA;

import java.util.List;
import java.util.function.Consumer;
import no.entur.antu.exception.AntuException;
import no.entur.antu.validator.AntuNetexValidator;
import no.entur.antu.validator.RuleCode;
import no.entur.antu.validator.ValidationContextWithNetexEntitiesIndex;
import no.entur.antu.validator.servicelink.GeometryUtils;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntryFactory;
import org.entur.netex.validation.validator.xpath.ValidationContext;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
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
public class InvalidFlexibleArea extends AntuNetexValidator {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    InvalidFlexibleArea.class
  );

  public InvalidFlexibleArea(
    ValidationReportEntryFactory validationReportEntryFactory
  ) {
    super(validationReportEntryFactory);
  }

  @Override
  protected RuleCode[] getRuleCodes() {
    return InvalidFlexibleAreaError.RuleCode.values();
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

      InvalidFlexibleAreaContextBuilder invalidFlexibleAreaContextBuilder =
        new InvalidFlexibleAreaContextBuilder();

      List<InvalidFlexibleAreaContextBuilder.InvalidFlexibleAreaContext> invalidFlexibleAreaContexts =
        invalidFlexibleAreaContextBuilder.build(index);

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
    } else {
      throw new AntuException(
        "Received invalid validation context in flexible area validator"
      );
    }
  }

  private void validateFlexibleArea(
    InvalidFlexibleAreaContextBuilder.InvalidFlexibleAreaContext invalidFlexibleAreaContext,
    Consumer<InvalidFlexibleAreaError> flexibleAreaError
  ) {
    if (!invalidFlexibleAreaContext.isEvenCoordinates()) {
      flexibleAreaError.accept(
        new InvalidFlexibleAreaError(
          INVALID_FLEXIBLE_AREA,
          invalidFlexibleAreaContext.flexibleAreaId(),
          invalidFlexibleAreaContext.flexibleStopPlaceId(),
          "Incomplete coordinates"
        )
      );
      return;
    }

    Coordinate[] jtsCoordinates =
      invalidFlexibleAreaContext.getJTSCoordinates();
    CoordinateSequence coordinateSequence = GeometryUtils
      .getGeometryFactory()
      .getCoordinateSequenceFactory()
      .create(jtsCoordinates);

    try {
      LinearRing linearRing = GeometryUtils
        .getGeometryFactory()
        .createLinearRing(coordinateSequence);

      IsValidOp isValidOp = new IsValidOp(linearRing);
      if (!isValidOp.isValid()) {
        flexibleAreaError.accept(
          new InvalidFlexibleAreaError(
            INVALID_FLEXIBLE_AREA,
            invalidFlexibleAreaContext.flexibleAreaId(),
            invalidFlexibleAreaContext.flexibleStopPlaceId(),
            isValidOp.getValidationError().toString()
          )
        );
      }
    } catch (Exception ex) {
      flexibleAreaError.accept(
        new InvalidFlexibleAreaError(
          INVALID_FLEXIBLE_AREA,
          invalidFlexibleAreaContext.flexibleAreaId(),
          invalidFlexibleAreaContext.flexibleStopPlaceId(),
          ex.getMessage()
        )
      );
    }
  }
}
