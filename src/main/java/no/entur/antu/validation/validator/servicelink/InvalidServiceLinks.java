package no.entur.antu.validation.validator.servicelink;

import java.util.Objects;
import java.util.function.Consumer;
import no.entur.antu.commondata.CommonDataRepository;
import no.entur.antu.stop.StopPlaceRepository;
import no.entur.antu.validation.AntuNetexData;
import no.entur.antu.validation.AntuNetexValidator;
import no.entur.antu.validation.RuleCode;
import no.entur.antu.validation.ValidationError;
import no.entur.antu.validation.validator.Comparison;
import org.entur.netex.index.api.NetexEntitiesIndex;
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
public class InvalidServiceLinks extends AntuNetexValidator {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    InvalidServiceLinks.class
  );

  private static final double DISTANCE_WARNING = 20;
  private static final double DISTANCE_MAX = 100;

  public InvalidServiceLinks(
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
    return InvalidServiceLinkError.RuleCode.values();
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

    InvalidServiceLinkContext.Builder contextBuilder =
      new InvalidServiceLinkContext.Builder(antuNetexData);

    antuNetexData
      .serviceLinks()
      .map(contextBuilder::build)
      .filter(Objects::nonNull)
      .forEach(invalidServiceLinkContext ->
        validateServiceLink(
          antuNetexData,
          invalidServiceLinkContext,
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
    We are getting the only in the common file, but it's not the rule.
    But, what if the StopPlaceAssignments appears in the LineFile:
      The common file does not refer to the line file, so the StopPlaceAssignments
      will appear in the line file, when the Service links are in the common file.
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
    InvalidServiceLinkContext context,
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
    InvalidServiceLinkContext context,
    Consumer<ValidationError> reportError
  ) {
    if (distance > DISTANCE_MAX) {
      reportError.accept(
        new InvalidServiceLinkError(
          isStart
            ? InvalidServiceLinkError.RuleCode.DISTANCE_BETWEEN_STOP_POINT_AND_START_OF_LINE_STRING_EXCEEDS_MAX_LIMIT
            : InvalidServiceLinkError.RuleCode.DISTANCE_BETWEEN_STOP_POINT_AND_END_OF_LINE_STRING_EXCEEDS_MAX_LIMIT,
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
        new InvalidServiceLinkError(
          isStart
            ? InvalidServiceLinkError.RuleCode.DISTANCE_BETWEEN_STOP_POINT_AND_START_OF_LINE_STRING_EXCEEDS_WARNING_LIMIT
            : InvalidServiceLinkError.RuleCode.DISTANCE_BETWEEN_STOP_POINT_AND_END_OF_LINE_STRING_EXCEEDS_WARNING_LIMIT,
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
