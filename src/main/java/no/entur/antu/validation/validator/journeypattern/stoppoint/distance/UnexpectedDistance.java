package no.entur.antu.validation.validator.journeypattern.stoppoint.distance;

import java.util.function.Consumer;
import no.entur.antu.commondata.CommonDataRepository;
import no.entur.antu.stop.StopPlaceRepository;
import no.entur.antu.validation.AntuNetexData;
import no.entur.antu.validation.AntuNetexValidator;
import no.entur.antu.validation.RuleCode;
import no.entur.antu.validation.ValidationError;
import no.entur.antu.validation.utilities.Comparison;
import no.entur.antu.validation.utilities.SphericalDistanceLibrary;
import no.entur.antu.validation.validator.journeypattern.stoppoint.distance.UnexpectedDistanceContextBuilder.ScheduledStopPointCoordinates;
import no.entur.antu.validation.validator.journeypattern.stoppoint.distance.UnexpectedDistanceContextBuilder.UnexpectedDistanceContext;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntryFactory;
import org.entur.netex.validation.validator.xpath.ValidationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validate that the distance between stops in journey patterns is as expected.
 */
public class UnexpectedDistance extends AntuNetexValidator {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    UnexpectedDistance.class
  );
  private final CommonDataRepository commonDataRepository;
  private final StopPlaceRepository stopPlaceRepository;

  protected UnexpectedDistance(
    ValidationReportEntryFactory validationReportEntryFactory,
    CommonDataRepository commonDataRepository,
    StopPlaceRepository stopPlaceRepository
  ) {
    super(validationReportEntryFactory);
    this.commonDataRepository = commonDataRepository;
    this.stopPlaceRepository = stopPlaceRepository;
  }

  @Override
  protected RuleCode[] getRuleCodes() {
    return new RuleCode[0];
  }

  @Override
  public void validateLineFile(
    ValidationReport validationReport,
    ValidationContext validationContext
  ) {
    LOGGER.debug("Validating distance between stops in journey patterns");

    AntuNetexData antuNetexData = createAntuNetexData(
      validationReport,
      validationContext
    );

    UnexpectedDistanceContextBuilder builder =
      new UnexpectedDistanceContextBuilder(
        antuNetexData.withStopPlacesAndCommonData(
          commonDataRepository,
          stopPlaceRepository
        )
      );

    antuNetexData
      .entitiesIndex()
      .getJourneyPatternIndex()
      .getAll()
      .stream()
      .map(builder::build)
      .filter(UnexpectedDistanceContext::isValid)
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
    ValidationContext validationContext
  ) {
    // JourneyPatterns only appear in the Line file.
  }

  private void validateDistance(
    AntuNetexData antuNetexData,
    UnexpectedDistanceContext distanceContext,
    Consumer<ValidationError> reportError
  ) {
    ExpectedDistance expectedDistance = ExpectedDistance.of(
      distanceContext.transportMode()
    );

    ScheduledStopPointCoordinates previous = distanceContext
      .scheduledStopPointCoordinates()
      .get(0);

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
          new UnexpectedDistanceError(
            UnexpectedDistanceError.RuleCode.DISTANCE_BETWEEN_STOP_POINTS_LESS_THAN_EXPECTED,
            distanceContext.journeyPatternRef(),
            antuNetexData.getStopPointName(previous.scheduledStopPointId()),
            antuNetexData.getStopPointName(current.scheduledStopPointId()),
            Comparison.of(expectedDistance.minDistance(), distance)
          )
        );
      } else if (distance > expectedDistance.maxDistance()) {
        reportError.accept(
          new UnexpectedDistanceError(
            UnexpectedDistanceError.RuleCode.DISTANCE_BETWEEN_STOP_POINTS_MORE_THAN_EXPECTED,
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