package no.entur.antu.validation.validator.servicejourney.speed;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.DoubleSupplier;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import no.entur.antu.stoptime.PassingTimes;
import no.entur.antu.stoptime.SortStopTimesUtil;
import no.entur.antu.stoptime.StopTime;
import org.entur.netex.validation.validator.Severity;
import org.entur.netex.validation.validator.ValidationIssue;
import org.entur.netex.validation.validator.ValidationRule;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
import org.entur.netex.validation.validator.jaxb.JAXBValidator;
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
public class UnexpectedSpeedValidator implements JAXBValidator {

  static final ValidationRule RULE_SAME_DEPARTURE_ARRIVAL_TIME =
    new ValidationRule(
      "SAME_DEPARTURE_ARRIVAL_TIME",
      "Same departure/arrival time for consecutive stops",
      "Same departure/arrival time for consecutive stops, from %s, to %s",
      Severity.INFO
    );

  static final ValidationRule RULE_LOW_SPEED = new ValidationRule(
    "LOW_SPEED",
    "Slow travel in ServiceJourney",
    "ServiceJourney has low speed, from %s, to %s, ExpectedSpeed = %s, ActualSpeed = %s",
    Severity.WARNING
  );

  static final ValidationRule RULE_HIGH_SPEED = new ValidationRule(
    "HIGH_SPEED",
    "Too fast travel in ServiceJourney",
    "ServiceJourney has too high speed, from %s, to %s, ExpectedSpeed = %s, ActualSpeed = %s",
    Severity.ERROR
  );

  static final ValidationRule RULE_WARNING_SPEED = new ValidationRule(
    "WARNING_SPEED",
    "Fast travel in ServiceJourney",
    "ServiceJourney has high speed, from %s, to %s, ExpectedSpeed = %s, ActualSpeed = %s",
    Severity.WARNING
  );

  private static final Logger LOGGER = LoggerFactory.getLogger(
    UnexpectedSpeedValidator.class
  );

  @Override
  public List<ValidationIssue> validate(
    JAXBValidationContext validationContext
  ) {
    UnexpectedSpeedContext.Builder contextBuilder =
      new UnexpectedSpeedContext.Builder(validationContext);

    return validationContext
      .serviceJourneys()
      .stream()
      .map(contextBuilder::build)
      .filter(UnexpectedSpeedContext::isValid)
      .map(context -> validateServiceJourney(context, validationContext))
      .flatMap(Collection::stream)
      .toList();
  }

  @Override
  public Set<ValidationRule> getRules() {
    return Set.of(
      RULE_HIGH_SPEED,
      RULE_LOW_SPEED,
      RULE_WARNING_SPEED,
      RULE_SAME_DEPARTURE_ARRIVAL_TIME
    );
  }

  private List<ValidationIssue> validateServiceJourney(
    UnexpectedSpeedContext context,
    JAXBValidationContext validationContext
  ) {
    List<StopTime> sortedTimetabledPassingTime =
      SortStopTimesUtil.getSortedStopTimes(
        context.serviceJourney(),
        validationContext
      );

    return IntStream
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
          validationContext,
          passingTimes
        )
      )
      .filter(context::hasValidCoordinates)
      .map(passingTimes ->
        validateSpeed(context, validationContext, passingTimes)
      )
      .filter(Objects::nonNull)
      .toList();
  }

  private boolean filterAndReportInValidTimeDifference(
    ServiceJourney serviceJourney,
    JAXBValidationContext validationContext,
    PassingTimes passingTimes
  ) {
    if (passingTimes.getTimeDifference() == 0) {
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

  @Nullable
  private static ValidationIssue validateSpeed(
    UnexpectedSpeedContext context,
    JAXBValidationContext validationContext,
    PassingTimes passingTimes
  ) {
    double distance = context.calculateDistance(passingTimes);
    if (distance < 1) {
      LOGGER.debug(
        "Distance between stops is less than 1 meter, skipping speed validation"
      );
      return null;
    }

    if (context.transportMode() == null) {
      // TransportMode on Line is mandatory. At this point, the validation entry for the Missing transport mode,
      // will already be created. So we will simply ignore it, if there is no transportModeForServiceJourney exists.
      // stopPlaceTransportModes should never be null at this point, as it is mandatory in stop places file in tiamat.
      // In worst case we will return true to ignore the validation.
      LOGGER.debug("Transport mode is missing, skipping speed validation");
      return null;
    }

    ExpectedSpeed expectedSpeed = ExpectedSpeed.of(context.transportMode());
    if (expectedSpeed == null) {
      LOGGER.debug(
        "No expected speed for transport mode {}, skipping speed validation",
        context.transportMode()
      );
      return null;
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

      return new ValidationIssue(
        RULE_LOW_SPEED,
        validationContext.dataLocation(context.serviceJourney().getId()),
        validationContext.stopPointName(
          passingTimes.from().scheduledStopPointId()
        ),
        validationContext.stopPointName(
          passingTimes.to().scheduledStopPointId()
        ),
        Long.toString(expectedSpeed.minSpeed()),
        Double.toString(optimisticSpeed)
      );
    } else if (pessimisticSpeed > expectedSpeed.warningSpeed()) {
      // too fast
      if (pessimisticSpeed > expectedSpeed.maxSpeed()) {
        return new ValidationIssue(
          RULE_HIGH_SPEED,
          validationContext.dataLocation(context.serviceJourney().getId()),
          validationContext.stopPointName(
            passingTimes.from().scheduledStopPointId()
          ),
          validationContext.stopPointName(
            passingTimes.to().scheduledStopPointId()
          ),
          Long.toString(expectedSpeed.maxSpeed()),
          Double.toString(pessimisticSpeed)
        );
      } else {
        return new ValidationIssue(
          RULE_WARNING_SPEED,
          validationContext.dataLocation(context.serviceJourney().getId()),
          validationContext.stopPointName(
            passingTimes.from().scheduledStopPointId()
          ),
          validationContext.stopPointName(
            passingTimes.to().scheduledStopPointId()
          ),
          Long.toString(expectedSpeed.warningSpeed()),
          Double.toString(pessimisticSpeed)
        );
      }
    }
    return null;
  }
}
