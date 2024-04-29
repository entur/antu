package no.entur.antu.validation.validator.servicelink.distance;

import java.util.Objects;
import java.util.function.Consumer;
import no.entur.antu.commondata.CommonDataRepository;
import no.entur.antu.stop.StopPlaceRepository;
import no.entur.antu.validation.AntuNetexData;
import no.entur.antu.validation.AntuNetexValidator;
import no.entur.antu.validation.RuleCode;
import no.entur.antu.validation.ValidationError;
import no.entur.antu.validation.utilities.Comparison;
import no.entur.antu.validation.utilities.SphericalDistanceLibrary;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntryFactory;
import org.entur.netex.validation.validator.xpath.ValidationContext;
import org.locationtech.jts.geom.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates ServiceLinks, by checking the distance between the stop points and the line string.
 * Stop points are the start and end points of the service link, and the line string is the geometry of the service link.
 * The distance is expected to be within a configured 'WARNING' and 'MAX' limits, and if it exceeds the limit,
 * a warning or an error is added to the validation report.
 */
public class UnexpectedDistanceValidator extends AntuNetexValidator {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    UnexpectedDistanceValidator.class
  );

  private static final double DISTANCE_WARNING = 20;
  private static final double DISTANCE_MAX = 100;

  public UnexpectedDistanceValidator(
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
    return UnexpectedDistanceError.RuleCode.values();
  }

  @Override
  public void validateCommonFile(
    ValidationReport validationReport,
    ValidationContext validationContext
  ) {
    LOGGER.debug("Validating ServiceLinks");

    AntuNetexData antuNetexData = createAntuNetexData(
      validationReport,
      validationContext
    );

    UnexpectedDistanceContext.Builder contextBuilder =
      new UnexpectedDistanceContext.Builder(antuNetexData);

    antuNetexData
      .serviceLinks()
      .map(contextBuilder::build)
      .filter(Objects::nonNull)
      .forEach(unexpectedDistanceContext ->
        validateServiceLink(
          antuNetexData,
          unexpectedDistanceContext,
          error ->
            addValidationReportEntry(validationReport, validationContext, error)
        )
      );
  }

  @Override
  protected void validateLineFile(
    ValidationReport validationReport,
    ValidationContext validationContext
  ) {
    /*
    No validation needed for line file

    We are validating service links only in the common file, but it's not the rule.
    They can also appear in line file, but they need to refer to the StopPlaceAssignments.

    But, what if the StopPlaceAssignments appears in the LineFile:
      The common file does not refer to the line file, so the StopPlaceAssignments
      will not appear in the line file, when the Service links are in the common file.

    We may need the implement the case where both the Service links and StopPlaceAssignments
    appear in the line file, OR the Service links in the line file and the StopPlaceAssignments
    in the common file.
    */
  }

  /**
   * Validates the distance between the stop points and the line string.
   * If the distance exceeds the warning limit, a warning is added to the validation report.
   * If the distance exceeds the max limit, an error is added to the validation report.
   */
  private void validateServiceLink(
    AntuNetexData antuNetexData,
    UnexpectedDistanceContext context,
    Consumer<ValidationError> reportError
  ) {
    Coordinate startCoordinate = context
      .fromQuayCoordinates()
      .asJtsCoordinate();
    Coordinate endCoordinate = context.toQuayCoordinates().asJtsCoordinate();

    Coordinate geometryStartCoordinate = context
      .lineString()
      .getStartPoint()
      .getCoordinate();
    Coordinate geometryEndCoordinate = context
      .lineString()
      .getEndPoint()
      .getCoordinate();

    double distanceFromStart = SphericalDistanceLibrary.fastDistance(
      startCoordinate,
      geometryStartCoordinate
    );
    double distanceFromEnd = SphericalDistanceLibrary.fastDistance(
      endCoordinate,
      geometryEndCoordinate
    );

    checkDistanceAndReportError(
      antuNetexData,
      distanceFromStart,
      true,
      context,
      reportError
    );
    checkDistanceAndReportError(
      antuNetexData,
      distanceFromEnd,
      false,
      context,
      reportError
    );
  }

  private void checkDistanceAndReportError(
    AntuNetexData antuNetexData,
    double distance,
    boolean isStart,
    UnexpectedDistanceContext context,
    Consumer<ValidationError> reportError
  ) {
    if (distance > DISTANCE_MAX) {
      reportError.accept(
        new UnexpectedDistanceError(
          isStart
            ? UnexpectedDistanceError.RuleCode.DISTANCE_BETWEEN_STOP_POINT_AND_START_OF_LINE_STRING_EXCEEDS_MAX_LIMIT
            : UnexpectedDistanceError.RuleCode.DISTANCE_BETWEEN_STOP_POINT_AND_END_OF_LINE_STRING_EXCEEDS_MAX_LIMIT,
          Comparison.of(DISTANCE_MAX, distance),
          isStart
            ? antuNetexData.getStopPointName(context.fromScheduledStopPointId())
            : antuNetexData.getStopPointName(context.toScheduledStopPointId()),
          context.serviceLinkId()
        )
      );
      return;
    }
    if (distance > DISTANCE_WARNING) {
      reportError.accept(
        new UnexpectedDistanceError(
          isStart
            ? UnexpectedDistanceError.RuleCode.DISTANCE_BETWEEN_STOP_POINT_AND_START_OF_LINE_STRING_EXCEEDS_WARNING_LIMIT
            : UnexpectedDistanceError.RuleCode.DISTANCE_BETWEEN_STOP_POINT_AND_END_OF_LINE_STRING_EXCEEDS_WARNING_LIMIT,
          Comparison.of(DISTANCE_WARNING, distance),
          isStart
            ? antuNetexData.getStopPointName(context.fromScheduledStopPointId())
            : antuNetexData.getStopPointName(context.toScheduledStopPointId()),
          context.serviceLinkId()
        )
      );
    }
  }
}
