package no.entur.antu.validation.validator.journeypattern.stoppoint.distance;

import java.util.function.Consumer;
import no.entur.antu.validation.AntuNetexData;
import no.entur.antu.validation.AntuNetexValidator;
import no.entur.antu.validation.RuleCode;
import no.entur.antu.validation.ValidationError;
import no.entur.antu.validation.utilities.Comparison;
import no.entur.antu.validation.utilities.SphericalDistanceLibrary;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntryFactory;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
import org.entur.netex.validation.validator.xpath.XPathValidationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validate that the distance between stops in journey patterns is as expected.
 * The distance between stops in a journey pattern should be within a configured 'MIN' and 'MAX' limits.
 * If the distance is less than the 'MIN' limit, a warning is added to the validation report.
 * If the distance is more than the 'MAX' limit, an error is added to the validation report.
 * Chouette Reference: 3-JourneyPattern-rutebanken-1
 */
public class UnexpectedDistanceBetweenStopPointsValidator
  extends AntuNetexValidator {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    UnexpectedDistanceBetweenStopPointsValidator.class
  );

  public UnexpectedDistanceBetweenStopPointsValidator(
    ValidationReportEntryFactory validationReportEntryFactory
  ) {
    super(validationReportEntryFactory);
  }

  @Override
  protected RuleCode[] getRuleCodes() {
    return UnexpectedDistanceBetweenStopPointsError.RuleCode.values();
  }

  @Override
  public void validateLineFile(
    ValidationReport validationReport,
    JAXBValidationContext validationContext,
    AntuNetexData antuNetexData
  ) {
    LOGGER.debug("Validating distance between stops in journey patterns");

    UnexpectedDistanceBetweenStopPointsContext.Builder builder =
      new UnexpectedDistanceBetweenStopPointsContext.Builder(antuNetexData);

    antuNetexData
      .journeyPatterns()
      .map(builder::build)
      .filter(UnexpectedDistanceBetweenStopPointsContext::isValid)
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

  @Override
  protected void validateCommonFile(
    ValidationReport validationReport,
    JAXBValidationContext validationContext,
    AntuNetexData antuNetexData
  ) {
    // JourneyPatterns only appear in the Line file.
  }

  private void validateDistance(
    AntuNetexData antuNetexData,
    UnexpectedDistanceBetweenStopPointsContext distanceContext,
    Consumer<ValidationError> reportError
  ) {
    ExpectedDistance expectedDistance = ExpectedDistance.of(
      distanceContext.transportMode()
    );

    UnexpectedDistanceBetweenStopPointsContext.ScheduledStopPointCoordinates previous =
      distanceContext.scheduledStopPointCoordinates().get(0);

    for (
      int i = 1;
      i < distanceContext.scheduledStopPointCoordinates().size();
      i++
    ) {
      var current = distanceContext.scheduledStopPointCoordinates().get(i);

      double distance = SphericalDistanceLibrary.distance(
        previous.quayCoordinates(),
        current.quayCoordinates()
      );

      if (distance < expectedDistance.minDistance()) {
        reportError.accept(
          new UnexpectedDistanceBetweenStopPointsError(
            UnexpectedDistanceBetweenStopPointsError.RuleCode.DISTANCE_BETWEEN_STOP_POINTS_LESS_THAN_EXPECTED,
            distanceContext.journeyPatternRef(),
            antuNetexData.getStopPointName(previous.scheduledStopPointId()),
            antuNetexData.getStopPointName(current.scheduledStopPointId()),
            Comparison.of(expectedDistance.minDistance(), distance)
          )
        );
      } else if (distance > expectedDistance.maxDistance()) {
        reportError.accept(
          new UnexpectedDistanceBetweenStopPointsError(
            UnexpectedDistanceBetweenStopPointsError.RuleCode.DISTANCE_BETWEEN_STOP_POINTS_MORE_THAN_EXPECTED,
            distanceContext.journeyPatternRef(),
            antuNetexData.getStopPointName(previous.scheduledStopPointId()),
            antuNetexData.getStopPointName(current.scheduledStopPointId()),
            Comparison.of(expectedDistance.maxDistance(), distance)
          )
        );
      }
      previous = current;
    }
  }
}
