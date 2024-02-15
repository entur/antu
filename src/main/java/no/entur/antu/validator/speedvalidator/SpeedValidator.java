package no.entur.antu.validator.speedvalidator;

import static no.entur.antu.validator.speedvalidator.ServiceJourneyContextBuilder.*;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import no.entur.antu.commondata.CommonDataRepository;
import no.entur.antu.exception.AntuException;
import no.entur.antu.stop.StopPlaceRepository;
import no.entur.antu.stoptime.PassingTimes;
import no.entur.antu.stoptime.SortedStopTimes;
import no.entur.antu.stoptime.StopTime;
import no.entur.antu.validator.AntuNetexValidator;
import no.entur.antu.validator.RuleCode;
import no.entur.antu.validator.ValidationContextWithNetexEntitiesIndex;
import no.entur.antu.validator.ValidationError;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.*;
import org.entur.netex.validation.validator.xpath.ValidationContext;
import org.rutebanken.netex.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates that the speed of a service journey is within expected limits.
 * The expected speed is based on the transport mode of the service journey.
 * The speed is calculated based on the distance between two stops and the time it takes to travel between them.
 */
public class SpeedValidator extends AntuNetexValidator {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    SpeedValidator.class
  );
  private final CommonDataRepository commonDataRepository;
  private final StopPlaceRepository stopPlaceRepository;

  public SpeedValidator(
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
    return Stream
      .concat(
        Arrays.stream(SpeedError.RuleCode.values()),
        Arrays.stream(SameDepartureArrivalTimeError.RuleCode.values())
      )
      .map(RuleCode.class::cast)
      .toArray(RuleCode[]::new);
  }

  @Override
  public void validate(
    ValidationReport validationReport,
    ValidationContext validationContext
  ) {
    LOGGER.debug("Validating Speed");

    if (validationContext.isCommonFile()) {
      return;
    }

    if (
      validationContext instanceof ValidationContextWithNetexEntitiesIndex validationContextWithNetexEntitiesIndex
    ) {
      NetexEntitiesIndex netexEntitiesIndex =
        validationContextWithNetexEntitiesIndex.getNetexEntitiesIndex();
      ServiceJourneyContextBuilder contextBuilder =
        new ServiceJourneyContextBuilder(
          validationReport.getValidationReportId(),
          netexEntitiesIndex,
          commonDataRepository,
          stopPlaceRepository
        );
      List<ServiceJourney> serviceJourneys = netexEntitiesIndex
        .getTimetableFrames()
        .stream()
        .flatMap(timetableFrame ->
          timetableFrame
            .getVehicleJourneys()
            .getVehicleJourneyOrDatedVehicleJourneyOrNormalDatedVehicleJourney()
            .stream()
        )
        .filter(ServiceJourney.class::isInstance)
        .map(ServiceJourney.class::cast)
        .toList();

      serviceJourneys
        .stream()
        .map(contextBuilder::build)
        .forEach(context ->
          validateServiceJourney(
            context,
            netexEntitiesIndex,
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
        "Received invalid validation context in Speed validator"
      );
    }
  }

  private void validateServiceJourney(
    ServiceJourneyContext context,
    NetexEntitiesIndex netexEntitiesIndex,
    Consumer<ValidationError> reportError
  ) {
    List<StopTime> sortedTimetabledPassingTime = SortedStopTimes.from(
      context.serviceJourney(),
      netexEntitiesIndex
    );

    IntStream
      .range(1, sortedTimetabledPassingTime.size())
      .mapToObj(i ->
        new PassingTimes(
          sortedTimetabledPassingTime.get(i - 1),
          sortedTimetabledPassingTime.get(i)
        )
      )
      .takeWhile(PassingTimes::isValid)
      .filter(passingTimes ->
        filterAndReportInValidTimeDifference(context, passingTimes, reportError)
      )
      .filter(context::hasValidCoordinates)
      .forEach(passingTimes -> validateSpeed(context, passingTimes, reportError)
      );
  }

  private boolean filterAndReportInValidTimeDifference(
    ServiceJourneyContext context,
    PassingTimes passingTimes,
    Consumer<ValidationError> reportError
  ) {
    if (passingTimes.getTimeDifference() == 0) {
      reportError.accept(
        new SameDepartureArrivalTimeError(
          context.serviceJourney().getId(),
          passingTimes,
          SameDepartureArrivalTimeError.RuleCode.SAME_DEPARTURE_ARRIVAL_TIME
        )
      );
      return false;
    }
    return true;
  }

  /**
   * Why multiply by 3.6?
   * <p>
   * To convert m/s to km/h, we have to convert two units i.e. distance and time
   * First km to meters, 1 km = 1000 meters
   * And hour to seconds, 1 hour = 1×60×60 = 3600,
   * <p>
   * So 1 m/s = 1000/3600 = 10/36 = 0.277
   * So to convert km/h to m/s, we have to multiply by 0.277 approx.
   * <p>
   * Similarly, to convert m/s to km/h,
   * To convert meters to kilometers divide by 1000
   * To Convert seconds to hours divide by 3600 i.e. for 1 second 1/3600,
   * <p>
   * So 1 km/h = (1/1000) ÷ (1÷3600) = (1×3600)/1000 = 3.6
   * So to convert m/s to km/h, we have to multiply by 3.6
   */
  private static double calculateSpeedInKilometerPerHour(
    double distanceInMeters,
    DoubleSupplier timeInSeconds
  ) {
    return distanceInMeters / timeInSeconds.getAsDouble() * 3.6;
  }

  private static void validateSpeed(
    ServiceJourneyContext context,
    PassingTimes passingTimes,
    Consumer<ValidationError> reportError
  ) {
    double distance = context.calculateDistance(passingTimes);
    if (distance < 1) {
      LOGGER.debug(
        "Distance between stops is less than 1 meter, skipping speed validation"
      );
      return;
    }

    if (context.transportMode() == null) {
      // TransportMode on Line is mandatory. At this point, the validation entry for the Missing transport mode,
      // will already be created. So we will simply ignore it, if there is no transportModeForServiceJourney exists.
      // stopPlaceTransportModes should never be null at this point, as it is mandatory in stop places file in tiamat.
      // In worst case we will return true to ignore the validation.
      LOGGER.debug("Transport mode is missing, skipping speed validation");
      return;
    }

    ExpectedSpeed expectedSpeed = ExpectedSpeed.of(context.transportMode());
    if (expectedSpeed == null) {
      LOGGER.debug(
        "No expected speed for transport mode {}, skipping speed validation",
        context.transportMode()
      );
      return;
    }

    // Assume max error (120 sec) when comparing with min and max expected speed.
    double optimisticSpeed = calculateSpeedInKilometerPerHour(
      distance,
      () -> passingTimes.minimumPossibleTimeDifference(120)
    );

    double pessimisticSpeed = calculateSpeedInKilometerPerHour(
      distance,
      () -> passingTimes.maximumPossibleTimeDifference(120)
    );

    if (optimisticSpeed < expectedSpeed.minSpeed()) {
      // too slow
      reportError.accept(
        new SpeedError(
          context.serviceJourney().getId(),
          passingTimes,
          SpeedError.RuleCode.LOW_SPEED,
          Long.toString(expectedSpeed.minSpeed()),
          Double.toString(optimisticSpeed)
        )
      );
    } else if (pessimisticSpeed > expectedSpeed.warningSpeed()) {
      // too fast
      if (pessimisticSpeed > expectedSpeed.maxSpeed()) {
        reportError.accept(
          new SpeedError(
            context.serviceJourney().getId(),
            passingTimes,
            SpeedError.RuleCode.HIGH_SPEED,
            Long.toString(expectedSpeed.maxSpeed()),
            Double.toString(pessimisticSpeed)
          )
        );
      } else {
        reportError.accept(
          new SpeedError(
            context.serviceJourney().getId(),
            passingTimes,
            SpeedError.RuleCode.WARNING_SPEED,
            Long.toString(expectedSpeed.warningSpeed()),
            Double.toString(pessimisticSpeed)
          )
        );
      }
    }
  }
}
