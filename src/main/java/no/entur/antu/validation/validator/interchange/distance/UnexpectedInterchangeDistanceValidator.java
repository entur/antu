package no.entur.antu.validation.validator.interchange.distance;

import java.util.function.Consumer;
import no.entur.antu.validation.AntuNetexData;
import no.entur.antu.validation.AntuNetexValidator;
import no.entur.antu.validation.RuleCode;
import no.entur.antu.validation.ValidationError;
import no.entur.antu.validation.utilities.Comparison;
import no.entur.antu.validation.utilities.SphericalDistanceLibrary;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntryFactory;
import org.entur.netex.validation.validator.xpath.ValidationContext;

/**
 * Validates that the distance between stop points in interchange is within the expected range.
 */
public class UnexpectedInterchangeDistanceValidator extends AntuNetexValidator {

  private static final double INTERCHANGE_EXPECTED_DISTANCE = 1000;

  public UnexpectedInterchangeDistanceValidator(
    ValidationReportEntryFactory validationReportEntryFactory
  ) {
    super(validationReportEntryFactory);
  }

  @Override
  protected RuleCode[] getRuleCodes() {
    return UnexpectedInterchangeDistanceError.RuleCode.values();
  }

  @Override
  protected void validateLineFile(
    ValidationReport validationReport,
    ValidationContext validationContext,
    AntuNetexData antuNetexData
  ) {
    antuNetexData
      .serviceJourneyInterchanges()
      .map(serviceJourneyInterchange ->
        UnexpectedInterchangeDistanceContext.of(
          antuNetexData,
          serviceJourneyInterchange
        )
      )
      .filter(UnexpectedInterchangeDistanceContext::isValid)
      .forEach(context ->
        validateDistance(
          antuNetexData,
          context,
          validationError ->
            addValidationReportEntry(
              validationReport,
              validationContext,
              validationError
            )
        )
      );
  }

  private void validateDistance(
    AntuNetexData antuNetexData,
    UnexpectedInterchangeDistanceContext distanceContext,
    Consumer<ValidationError> reportError
  ) {
    double distance = SphericalDistanceLibrary.distance(
      distanceContext.fromStopPointCoordinates().quayCoordinates(),
      distanceContext.toStopPointCoordinates().quayCoordinates()
    );

    if (distance > INTERCHANGE_EXPECTED_DISTANCE) {
      if (distance > (3 * INTERCHANGE_EXPECTED_DISTANCE)) {
        reportError.accept(
          new UnexpectedInterchangeDistanceError(
            UnexpectedInterchangeDistanceError.RuleCode.DISTANCE_BETWEEN_STOP_POINTS_IN_INTERCHANGE_IS_MORE_THAN_MAX_LIMIT,
            distanceContext.interchangeId(),
            antuNetexData.stopPointName(
              distanceContext.fromStopPointCoordinates().scheduledStopPointId()
            ),
            antuNetexData.stopPointName(
              distanceContext.toStopPointCoordinates().scheduledStopPointId()
            ),
            Comparison.of(3 * INTERCHANGE_EXPECTED_DISTANCE, distance)
          )
        );
      } else {
        reportError.accept(
          new UnexpectedInterchangeDistanceError(
            UnexpectedInterchangeDistanceError.RuleCode.DISTANCE_BETWEEN_STOP_POINTS_IN_INTERCHANGE_IS_MORE_THAN_WARNING_LIMIT,
            distanceContext.interchangeId(),
            antuNetexData.stopPointName(
              distanceContext.fromStopPointCoordinates().scheduledStopPointId()
            ),
            antuNetexData.stopPointName(
              distanceContext.toStopPointCoordinates().scheduledStopPointId()
            ),
            Comparison.of(INTERCHANGE_EXPECTED_DISTANCE, distance)
          )
        );
      }
    }
  }
}
