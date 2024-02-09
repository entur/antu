package no.entur.antu.flex.validation.validator.flexibleareavalidator;

import java.util.Collection;
import no.entur.antu.exception.AntuException;
import no.entur.antu.validator.AntuNetexValidator;
import no.entur.antu.validator.RuleCode;
import no.entur.antu.validator.ValidationContextWithNetexEntitiesIndex;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntryFactory;
import org.entur.netex.validation.validator.xpath.ValidationContext;
import org.rutebanken.netex.model.FlexibleArea;
import org.rutebanken.netex.model.FlexibleStopPlace_VersionStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlexibleAreaValidator extends AntuNetexValidator {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    FlexibleAreaValidator.class
  );

  protected FlexibleAreaValidator(ValidationReportEntryFactory validationReportEntryFactory) {
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

      index.getFlexibleStopPlaceIndex().getAll()
           .stream()
           .map(FlexibleStopPlace_VersionStructure::getAreas)
           .map(FlexibleStopPlace_VersionStructure.Areas::getFlexibleAreaOrFlexibleAreaRefOrHailAndRideArea)
           .flatMap(Collection::stream)
           .filter(FlexibleArea.class::isInstance)
           .map(FlexibleArea.class::cast)
           .map(flexibleArea ->
                  flexibleArea.getPolygon()
                              .getExterior()
                              .getAbstractRing()
                              .getValue()
           ).toList();
    } else {
      throw new AntuException(
        "Received invalid validation context in Speed validator"
      );
    }
  }
}
