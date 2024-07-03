package no.entur.antu.validation.validator.servicejourney.speed;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import no.entur.antu.model.ServiceJourneyId;
import no.entur.antu.stoptime.PassingTimes;
import no.entur.antu.stoptime.SortStopTimesUtil;
import no.entur.antu.stoptime.StopTime;
import no.entur.antu.validation.AntuNetexData;
import no.entur.antu.validation.AntuNetexValidator;
import no.entur.antu.validation.RuleCode;
import no.entur.antu.validation.ValidationError;
import no.entur.antu.validation.utilities.Comparison;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntryFactory;
import org.entur.netex.validation.validator.xpath.ValidationContext;
import org.rutebanken.netex.model.ServiceJourney;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates that the speed of a service journey is within expected limits.
 * The expected speed is based on the transport mode of the service journey.
 * The speed is calculated based on the distance between two stops
 * and the time it takes to travel between them.
 * Chouette references:
 *  3-VehicleJourney-2-1 (Chronologically reverse),
 *  3-VehicleJourney-2-2 (Min speed),
 *  3-VehicleJourney-2-3 (Warning speed)
 * 	3-VehicleJourney-2-4 (Same departure/arrival time)
 * 	3-VehicleJourney-2-5 (Max speed)
 */
public class UnexpectedSpeedValidator extends AntuNetexValidator {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    UnexpectedSpeedValidator.class
  );

  public UnexpectedSpeedValidator(
    ValidationReportEntryFactory validationReportEntryFactory
  ) {
    super(validationReportEntryFactory);
  }

  @Override
  protected RuleCode[] getRuleCodes() {
    return Stream
      .concat(
        Arrays.stream(UnexpectedSpeedError.RuleCode.values()),
        Arrays.stream(SameDepartureArrivalTimeError.RuleCode.values())
      )
      .map(RuleCode.class::cast)
      .toArray(RuleCode[]::new);
  }

  @Override
  public void validateLineFile(
    ValidationReport validationReport,
    ValidationContext validationContext,
    AntuNetexData antuNetexData
  ) {
    LOGGER.debug("Validating Speed");

    UnexpectedSpeedContext.Builder contextBuilder =
      new UnexpectedSpeedContext.Builder(antuNetexData);

    antuNetexData
      .validServiceJourneys()
      .map(contextBuilder::build)
      .filter(UnexpectedSpeedContext::isValid)
      .forEach(context ->
        validateServiceJourney(
          context,
          antuNetexData,
          error ->
            addValidationReportEntry(validationReport, validationContext, error)
        )
      );
  }

  @Override
  protected void validateCommonFile(
    ValidationReport validationReport,
    ValidationContext validationContext,
    AntuNetexData antuNetexData
  ) {
    // ServiceJourneys and Line only appear in the Line file.
  }

  private void validateServiceJourney(
    UnexpectedSpeedContext context,
    AntuNetexData antuNetexData,
    Consumer<ValidationError> reportError
  ) {
    List<StopTime> sortedTimetabledPassingTime =
      SortStopTimesUtil.getSortedStopTimes(
        context.serviceJourney(),
        antuNetexData
      );

    IntStream
      .range(1, sortedTimetabledPassingTime.size())
      .mapToObj(i ->
        new PassingTimes(
          sortedTimetabledPassingTime.get(i - 1),
          sortedTimetabledPassingTime.get(i)
        )
      )
      // Chouette reference: 3-VehicleJourney-2-1
      // Ignoring the validation if there exists the case where the time is chronologically reverse
      // Is this already validated and reported previously?? TODO: This is reported as Error in Chouette
      .takeWhile(PassingTimes::isValid)
      .filter(passingTimes ->
        // TODO: This will never reached due to the takeWhile above. Reported as WARNING in Chouette
        filterAndReportInValidTimeDifference(
          context.serviceJourney(),
          antuNetexData,
          passingTimes,
          reportError
        )
      )
      .filter(context::hasValidCoordinates)
      .forEach(passingTimes ->
        validateSpeed(context, antuNetexData, passingTimes, reportError)
      );
  }

  private boolean filterAndReportInValidTimeDifference(
    ServiceJourney serviceJourney,
    AntuNetexData antuNetexData,
    PassingTimes passingTimes,
    Consumer<ValidationError> reportError
  ) {
    if (passingTimes.getTimeDifference() == 0) {
      reportError.accept(
        new SameDepartureArrivalTimeError(
          ServiceJourneyId.ofValidId(serviceJourney),
          antuNetexData.stopPointName(
            passingTimes.from().scheduledStopPointId()
          ),
          antuNetexData.stopPointName(passingTimes.to().scheduledStopPointId()),
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
    UnexpectedSpeedContext context,
    AntuNetexData antuNetexData,
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
        new UnexpectedSpeedError(
          ServiceJourneyId.ofValidId(context.serviceJourney()),
          antuNetexData.stopPointName(
            passingTimes.from().scheduledStopPointId()
          ),
          antuNetexData.stopPointName(passingTimes.to().scheduledStopPointId()),
          UnexpectedSpeedError.RuleCode.LOW_SPEED,
          Comparison.of(
            Long.toString(expectedSpeed.minSpeed()),
            Double.toString(optimisticSpeed)
          )
        )
      );
    } else if (pessimisticSpeed > expectedSpeed.warningSpeed()) {
      // too fast
      if (pessimisticSpeed > expectedSpeed.maxSpeed()) {
        reportError.accept(
          new UnexpectedSpeedError(
            ServiceJourneyId.ofValidId(context.serviceJourney()),
            antuNetexData.stopPointName(
              passingTimes.from().scheduledStopPointId()
            ),
            antuNetexData.stopPointName(
              passingTimes.to().scheduledStopPointId()
            ),
            UnexpectedSpeedError.RuleCode.HIGH_SPEED,
            Comparison.of(
              Long.toString(expectedSpeed.maxSpeed()),
              Double.toString(pessimisticSpeed)
            )
          )
        );
      } else {
        reportError.accept(
          new UnexpectedSpeedError(
            ServiceJourneyId.ofValidId(context.serviceJourney()),
            antuNetexData.stopPointName(
              passingTimes.from().scheduledStopPointId()
            ),
            antuNetexData.stopPointName(
              passingTimes.to().scheduledStopPointId()
            ),
            UnexpectedSpeedError.RuleCode.WARNING_SPEED,
            Comparison.of(
              Long.toString(expectedSpeed.warningSpeed()),
              Double.toString(pessimisticSpeed)
              // TODO: 2 decimal points
            )
          )
        );
      }
    }
  }
}
