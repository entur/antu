package no.entur.antu.validation.validator.servicelink;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import no.entur.antu.commondata.CommonDataRepository;
import no.entur.antu.stop.StopPlaceRepository;
import no.entur.antu.validation.AntuNetexValidator;
import no.entur.antu.validation.RuleCode;
import no.entur.antu.validation.ValidationError;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntryFactory;
import org.entur.netex.validation.validator.xpath.ValidationContext;
import org.locationtech.jts.geom.Coordinate;
import org.rutebanken.netex.model.ServiceLink;
import org.rutebanken.netex.model.ServiceLinksInFrame_RelStructure;
import org.rutebanken.netex.model.Service_VersionFrameStructure;
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

  private static final int DISTANCE_WARNING = 20;
  private static final int DISTANCE_MAX = 100;

  private final CommonDataRepository commonDataRepository;
  private final StopPlaceRepository stopPlaceRepository;

  public InvalidServiceLinks(
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
    return InvalidServiceLinkError.RuleCode.values();
  }

  @Override
  public void validateCommonFile(
    ValidationReport validationReport,
    ValidationContext validationContext
  ) {
    LOGGER.debug("Validating ServiceLinks");

    NetexEntitiesIndex index = getNetexEntitiesIndex(validationContext);

    InvalidServiceLinkContext.Builder contextBuilder =
      new InvalidServiceLinkContext.Builder(
        validationReport.getValidationReportId(),
        commonDataRepository,
        stopPlaceRepository
      );

    List<ServiceLink> serviceLinks = index
      .getServiceFrames()
      .stream()
      .map(Service_VersionFrameStructure::getServiceLinks)
      .filter(Objects::nonNull)
      .map(ServiceLinksInFrame_RelStructure::getServiceLink)
      .flatMap(Collection::stream)
      .toList();

    serviceLinks
      .stream()
      .map(contextBuilder::build)
      .filter(Objects::nonNull)
      .forEach(invalidServiceLinkContext ->
        validateServiceLink(
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
    InvalidServiceLinkContext invalidServiceLinkContext,
    Consumer<ValidationError> reportError
  ) {
    Coordinate startCoordinate = invalidServiceLinkContext
      .from()
      .asJtsCoordinate();
    Coordinate endCoordinate = invalidServiceLinkContext.to().asJtsCoordinate();

    Coordinate geometryStartCoordinate = invalidServiceLinkContext
      .lineString()
      .getStartPoint()
      .getCoordinate();
    Coordinate geometryEndCoordinate = invalidServiceLinkContext
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
      distanceFromStart,
      true,
      invalidServiceLinkContext,
      reportError
    );
    checkDistanceAndReportError(
      distanceFromEnd,
      false,
      invalidServiceLinkContext,
      reportError
    );
  }

  private void checkDistanceAndReportError(
    double distance,
    boolean isStart,
    InvalidServiceLinkContext context,
    Consumer<ValidationError> reportError
  ) {
    if (distance > DISTANCE_MAX) {
      reportError.accept(
        createServiceLinksError(
          isStart
            ? InvalidServiceLinkError.RuleCode.DISTANCE_BETWEEN_STOP_POINT_AND_START_OF_LINE_STRING_EXCEEDS_MAX_LIMIT
            : InvalidServiceLinkError.RuleCode.DISTANCE_BETWEEN_STOP_POINT_AND_END_OF_LINE_STRING_EXCEEDS_MAX_LIMIT,
          distance,
          context
        )
      );
      return;
    }
    if (distance > DISTANCE_WARNING) {
      reportError.accept(
        createServiceLinksError(
          isStart
            ? InvalidServiceLinkError.RuleCode.DISTANCE_BETWEEN_STOP_POINT_AND_START_OF_LINE_STRING_EXCEEDS_WARNING_LIMIT
            : InvalidServiceLinkError.RuleCode.DISTANCE_BETWEEN_STOP_POINT_AND_END_OF_LINE_STRING_EXCEEDS_WARNING_LIMIT,
          distance,
          context
        )
      );
    }
  }

  private InvalidServiceLinkError createServiceLinksError(
    InvalidServiceLinkError.RuleCode ruleCode,
    double distance,
    InvalidServiceLinkContext context
  ) {
    return new InvalidServiceLinkError(
      ruleCode,
      distance,
      context.serviceLink().getFromPointRef(),
      context.serviceLink().getId()
    );
  }
}
