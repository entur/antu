package no.entur.antu.validator.servicelinksvalidator;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import no.entur.antu.commondata.CommonDataRepository;
import no.entur.antu.exception.AntuException;
import no.entur.antu.stop.StopPlaceRepository;
import no.entur.antu.validator.AntuNetexValidator;
import no.entur.antu.validator.RuleCode;
import no.entur.antu.validator.ValidationContextWithNetexEntitiesIndex;
import no.entur.antu.validator.ValidationError;
import no.entur.antu.validator.servicelinksvalidator.ServiceLinkContextBuilder.ServiceLinkContext;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntryFactory;
import org.entur.netex.validation.validator.xpath.ValidationContext;
import org.locationtech.jts.geom.Coordinate;
import org.rutebanken.netex.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceLinksValidator extends AntuNetexValidator {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    ServiceLinksValidator.class
  );

  private static final int DISTANCE_WARNING = 20;
  private static final int DISTANCE_MAX = 100;

  private final CommonDataRepository commonDataRepository;
  private final StopPlaceRepository stopPlaceRepository;

  public ServiceLinksValidator(
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
    return ServiceLinksError.RuleCode.values();
  }

  @Override
  public void validate(
    ValidationReport validationReport,
    ValidationContext validationContext
  ) {
    // Validate only common files
    if (!validationContext.isCommonFile()) {
      return;
    }

    LOGGER.debug("Validating ServiceLinks");

    if (
      validationContext instanceof ValidationContextWithNetexEntitiesIndex validationContextWithNetexEntitiesIndex
    ) {
      NetexEntitiesIndex index =
        validationContextWithNetexEntitiesIndex.getNetexEntitiesIndex();

      ServiceLinkContextBuilder contextBuilder = new ServiceLinkContextBuilder(
        validationReport.getValidationReportId(),
        commonDataRepository,
        stopPlaceRepository
      );

      List<ServiceLink> serviceLinks = index
        .getServiceFrames()
        .stream()
        .flatMap(frame -> frame.getServiceLinks().getServiceLink().stream())
        .toList();

      serviceLinks
        .stream()
        .map(contextBuilder::build)
        .filter(Objects::nonNull)
        .forEach(serviceLinkContext ->
          validateServiceLink(
            serviceLinkContext,
            error ->
              addValidationReportEntry(
                validationReport,
                validationContext,
                error
              )
          )
        );
    } else {
      throw new AntuException(
        "Received invalid validation context in " + "Validating ServiceLinks"
      );
    }
  }

  private void validateServiceLink(
    ServiceLinkContext serviceLinkContext,
    Consumer<ValidationError> reportError
  ) {
    Coordinate startCoordinate = serviceLinkContext.from().asJtsCoordinate();
    Coordinate endCoordinate = serviceLinkContext.to().asJtsCoordinate();

    Coordinate geometryStartCoordinate = serviceLinkContext
      .lineString()
      .getStartPoint()
      .getCoordinate();
    Coordinate geometryEndCoordinate = serviceLinkContext
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
      serviceLinkContext,
      reportError
    );
    checkDistanceAndReportError(
      distanceFromEnd,
      false,
      serviceLinkContext,
      reportError
    );
  }

  private void checkDistanceAndReportError(
    double distance,
    boolean isStart,
    ServiceLinkContext context,
    Consumer<ValidationError> reportError
  ) {
    if (distance > DISTANCE_WARNING) {
      reportError.accept(
        createServiceLinksError(
          isStart
            ? ServiceLinksError.RuleCode.DISTANCE_BETWEEN_STOP_POINT_AND_START_OF_LINE_STRING_EXCEEDS_WARNING_LIMIT
            : ServiceLinksError.RuleCode.DISTANCE_BETWEEN_STOP_POINT_AND_END_OF_LINE_STRING_EXCEEDS_WARNING_LIMIT,
          distance,
          context
        )
      );
      if (distance > DISTANCE_MAX) {
        reportError.accept(
          createServiceLinksError(
            isStart
              ? ServiceLinksError.RuleCode.DISTANCE_BETWEEN_STOP_POINT_AND_START_OF_LINE_STRING_EXCEEDS_MAX_LIMIT
              : ServiceLinksError.RuleCode.DISTANCE_BETWEEN_STOP_POINT_AND_END_OF_LINE_STRING_EXCEEDS_MAX_LIMIT,
            distance,
            context
          )
        );
      }
    }
  }

  private ServiceLinksError createServiceLinksError(
    ServiceLinksError.RuleCode ruleCode,
    double distance,
    ServiceLinkContext context
  ) {
    return new ServiceLinksError(
      ruleCode,
      distance,
      context.serviceLink().getFromPointRef(),
      context.serviceLink().getId()
    );
  }
}
