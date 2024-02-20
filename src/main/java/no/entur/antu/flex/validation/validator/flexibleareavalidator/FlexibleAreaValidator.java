package no.entur.antu.flex.validation.validator.flexibleareavalidator;

import static no.entur.antu.flex.validation.validator.flexibleareavalidator.FlexibleAreaError.RuleCode.INVALID_FLEXIBLE_AREA;

import java.util.List;
import java.util.function.Consumer;
import no.entur.antu.exception.AntuException;
import no.entur.antu.validator.AntuNetexValidator;
import no.entur.antu.validator.RuleCode;
import no.entur.antu.validator.ValidationContextWithNetexEntitiesIndex;
import no.entur.antu.validator.servicelinksvalidator.GeometryUtils;
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
        flexibleAreaContextBuilder.build(index);

      flexibleAreaContexts.forEach(flexibleAreaContext ->
        validateFlexibleArea(
          flexibleAreaContext,
          flexibleAreaError ->
            addValidationReportEntry(
              validationReport,
              validationContext,
              flexibleAreaError
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
    FlexibleAreaContextBuilder.FlexibleAreaContext flexibleAreaContext,
    Consumer<FlexibleAreaError> flexibleAreaError
  ) {
    if (!flexibleAreaContext.isEvenCoordinates()) {
      flexibleAreaError.accept(
        new FlexibleAreaError(
          INVALID_FLEXIBLE_AREA,
          flexibleAreaContext.flexibleAreaId(),
          flexibleAreaContext.flexibleStopPlaceId(),
          "Incomplete coordinates"
        )
      );
      return;
    }

    Coordinate[] jtsCoordinates = flexibleAreaContext.getJTSCoordinates();
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
          new FlexibleAreaError(
            INVALID_FLEXIBLE_AREA,
            flexibleAreaContext.flexibleAreaId(),
            flexibleAreaContext.flexibleStopPlaceId(),
            isValidOp.getValidationError().toString()
          )
        );
      }
    } catch (Exception ex) {
      flexibleAreaError.accept(
        new FlexibleAreaError(
          INVALID_FLEXIBLE_AREA,
          flexibleAreaContext.flexibleAreaId(),
          flexibleAreaContext.flexibleStopPlaceId(),
          ex.getMessage()
        )
      );
    }
  }
}
