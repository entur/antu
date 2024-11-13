package no.entur.antu.validation.validator.servicejourney.passingtime;

import java.util.List;
import java.util.function.Consumer;
import no.entur.antu.stoptime.SortStopTimesUtil;
import no.entur.antu.stoptime.StopTime;
import no.entur.antu.validation.AntuNetexValidator;
import no.entur.antu.validation.RuleCode;
import no.entur.antu.validation.ValidationError;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntryFactory;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
import org.entur.netex.validation.validator.model.ServiceJourneyId;
import org.rutebanken.netex.model.ServiceJourney;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates that the passing times of a service journey are non-decreasing.
 * This means that the time between each stop must be greater than or equal to zero.
 * Chouette reference: 3-VehicleJourney-5
 */
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
  public void validateLineFile(
    ValidationReport validationReport,
    JAXBValidationContext validationContext
  ) {
    LOGGER.debug("Validating ServiceJourney non-increasing passing time");

    validationContext
      .serviceJourneys()
      .forEach(serviceJourney ->
        validateServiceJourney(
          serviceJourney,
          validationContext,
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
    JAXBValidationContext validationContext
  ) {
    // ServiceJourneys and Line only appear in the Line file.
  }

  public void validateServiceJourney(
    ServiceJourney serviceJourney,
    JAXBValidationContext validationContext,
    Consumer<ValidationError> reportError
  ) {
    List<StopTime> sortedTimetabledPassingTime =
      SortStopTimesUtil.getSortedStopTimes(serviceJourney, validationContext);

    var previousPassingTime = sortedTimetabledPassingTime.get(0);
    if (
      validateStopTime(
        serviceJourney,
        validationContext,
        previousPassingTime,
        reportError
      )
    ) return;

    for (int i = 1; i < sortedTimetabledPassingTime.size(); i++) {
      var currentPassingTime = sortedTimetabledPassingTime.get(i);

      if (
        validateStopTime(
          serviceJourney,
          validationContext,
          currentPassingTime,
          reportError
        )
      ) return;

      if (!previousPassingTime.isStopTimesIncreasing(currentPassingTime)) {
        reportError.accept(
          new NonIncreasingPassingTimeError(
            NonIncreasingPassingTimeError.RuleCode.TIMETABLED_PASSING_TIME_NON_INCREASING_TIME,
            validationContext.stopPointName(
              previousPassingTime.scheduledStopPointId()
            ),
            ServiceJourneyId.ofValidId(serviceJourney)
          )
        );
        return;
      }

      previousPassingTime = currentPassingTime;
    }
  }

  private static boolean validateStopTime(
    ServiceJourney serviceJourney,
    JAXBValidationContext validationContext,
    StopTime stopTime,
    Consumer<ValidationError> reportError
  ) {
    ServiceJourneyId serviceJourneyId = ServiceJourneyId.ofValidId(
      serviceJourney
    );
    if (!stopTime.isComplete()) {
      reportError.accept(
        new NonIncreasingPassingTimeError(
          NonIncreasingPassingTimeError.RuleCode.TIMETABLED_PASSING_TIME_INCOMPLETE_TIME,
          validationContext.stopPointName(stopTime.scheduledStopPointId()),
          serviceJourneyId
        )
      );
      return true;
    }
    if (!stopTime.isConsistent()) {
      reportError.accept(
        new NonIncreasingPassingTimeError(
          NonIncreasingPassingTimeError.RuleCode.TIMETABLED_PASSING_TIME_INCONSISTENT_TIME,
          validationContext.stopPointName(stopTime.scheduledStopPointId()),
          serviceJourneyId
        )
      );
      return true;
    }
    return false;
  }
}
