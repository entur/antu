package no.entur.antu.validation.flex.validator.flexiblearea;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import no.entur.antu.validation.utilities.GeometryUtilities;
import org.entur.netex.validation.validator.Severity;
import org.entur.netex.validation.validator.ValidationIssue;
import org.entur.netex.validation.validator.ValidationRule;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
import org.entur.netex.validation.validator.jaxb.JAXBValidator;
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
public class InvalidFlexibleAreaValidator implements JAXBValidator {

  static final ValidationRule RULE = new ValidationRule(
    "INVALID_FLEXIBLE_AREA",
    "Invalid flexible area",
    "Invalid flexible area: %s",
    Severity.ERROR
  );

  private static final Logger LOGGER = LoggerFactory.getLogger(
    InvalidFlexibleAreaValidator.class
  );

  @Override
  public List<ValidationIssue> validate(
    JAXBValidationContext validationContext
  ) {
    LOGGER.debug("Validating flexible area");

    return validationContext
      .flexibleStopPlaces()
      .stream()
      .map(InvalidFlexibleAreaContext::of)
      .filter(Objects::nonNull)
      .map(invalidFlexibleAreaContext ->
        validateFlexibleArea(validationContext, invalidFlexibleAreaContext)
      )
      .filter(Objects::nonNull)
      .toList();
  }

  @Override
  public Set<ValidationRule> getRules() {
    return Set.of(RULE);
  }

  @Nullable
  private ValidationIssue validateFlexibleArea(
    JAXBValidationContext validationContext,
    InvalidFlexibleAreaContext invalidFlexibleAreaContext
  ) {
    if (
      !GeometryUtilities.isValidCoordinatesList(
        invalidFlexibleAreaContext.coordinates()
      )
    ) {
      return new ValidationIssue(
        RULE,
        validationContext.dataLocation(
          invalidFlexibleAreaContext.flexibleAreaId()
        ),
        "Incomplete coordinates"
      );
    }

    try {
      LinearRing linearRing = GeometryUtilities.createLinerRing(
        invalidFlexibleAreaContext.coordinates()
      );

      IsValidOp isValidOp = new IsValidOp(linearRing);
      if (!isValidOp.isValid()) {
        return new ValidationIssue(
          RULE,
          validationContext.dataLocation(
            invalidFlexibleAreaContext.flexibleAreaId()
          ),
          isValidOp.getValidationError().toString()
        );
      }
    } catch (Exception ex) {
      return new ValidationIssue(
        RULE,
        validationContext.dataLocation(
          invalidFlexibleAreaContext.flexibleAreaId()
        ),
        ex.getMessage()
      );
    }
    return null;
  }
}
