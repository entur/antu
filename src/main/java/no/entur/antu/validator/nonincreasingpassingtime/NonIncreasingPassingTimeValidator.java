package no.entur.antu.validator.nonincreasingpassingtime;

import java.util.List;
import java.util.function.Consumer;
import no.entur.antu.exception.AntuException;
import no.entur.antu.stoptime.SortedStopTimes;
import no.entur.antu.stoptime.StopTime;
import no.entur.antu.validator.AntuNetexValidator;
import no.entur.antu.validator.RuleCode;
import no.entur.antu.validator.ValidationContextWithNetexEntitiesIndex;
import no.entur.antu.validator.ValidationError;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.*;
import org.entur.netex.validation.validator.xpath.ValidationContext;
import org.rutebanken.netex.model.ServiceJourney;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NonIncreasingPassingTimeValidator extends AntuNetexValidator {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    NonIncreasingPassingTimeValidator.class
  );

  public NonIncreasingPassingTimeValidator(
    ValidationReportEntryFactory validationReportEntryFactory
  ) {
    super(validationReportEntryFactory);
  }

  @Override
  protected RuleCode[] getRuleCodes() {
    return NonIncreasingPassingTimeError.RuleCode.values();
  }

  @Override
  public void validate(
    ValidationReport validationReport,
    ValidationContext validationContext
  ) {
    LOGGER.debug("Validating ServiceJourney non-increasing passing time");

    if (validationContext.isCommonFile()) {
      return;
    }

    if (
      validationContext instanceof ValidationContextWithNetexEntitiesIndex validationContextWithNetexEntitiesIndex
    ) {
      NetexEntitiesIndex index =
        validationContextWithNetexEntitiesIndex.getNetexEntitiesIndex();
      List<ServiceJourney> serviceJourneys = index
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

      serviceJourneys.forEach(serviceJourney ->
        validateServiceJourney(
          serviceJourney,
          index,
          validationError ->
            addValidationReportEntry(
              validationReport,
              validationContext,
              validationError
            )
        )
      );
    } else {
      throw new AntuException(
        "Received invalid validation context in " +
        "Validating ServiceJourney non-increasing passing time"
      );
    }
  }

  public void validateServiceJourney(
    ServiceJourney serviceJourney,
    NetexEntitiesIndex netexEntitiesIndex,
    Consumer<ValidationError> reportError
  ) {
    List<StopTime> sortedTimetabledPassingTime = SortedStopTimes.from(
      serviceJourney,
      netexEntitiesIndex
    );

    var previousPassingTime = sortedTimetabledPassingTime.get(0);
    if (
      validateStopTime(serviceJourney, previousPassingTime, reportError)
    ) return;

    for (int i = 1; i < sortedTimetabledPassingTime.size(); i++) {
      var currentPassingTime = sortedTimetabledPassingTime.get(i);

      if (
        validateStopTime(serviceJourney, currentPassingTime, reportError)
      ) return;

      if (!previousPassingTime.isStopTimesIncreasing(currentPassingTime)) {
        reportError.accept(
          new NonIncreasingPassingTimeError(
            NonIncreasingPassingTimeError.RuleCode.TIMETABLED_PASSING_TIME_NON_INCREASING_TIME,
            previousPassingTime,
            serviceJourney.getId()
          )
        );
        return;
      }

      previousPassingTime = currentPassingTime;
    }
  }

  private static boolean validateStopTime(
    ServiceJourney serviceJourney,
    StopTime stopTime,
    Consumer<ValidationError> reportError
  ) {
    if (!stopTime.isComplete()) {
      reportError.accept(
        new NonIncreasingPassingTimeError(
          NonIncreasingPassingTimeError.RuleCode.TIMETABLED_PASSING_TIME_INCOMPLETE_TIME,
          stopTime,
          serviceJourney.getId()
        )
      );
      return true;
    }
    if (!stopTime.isConsistent()) {
      reportError.accept(
        new NonIncreasingPassingTimeError(
          NonIncreasingPassingTimeError.RuleCode.TIMETABLED_PASSING_TIME_INCONSISTENT_TIME,
          stopTime,
          serviceJourney.getId()
        )
      );
      return true;
    }
    return false;
  }
}
