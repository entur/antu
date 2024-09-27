package no.entur.antu.validation.validator.servicejourney.passingtime;

import java.util.List;
import java.util.function.Consumer;
import no.entur.antu.stoptime.SortStopTimesUtil;
import no.entur.antu.stoptime.StopTime;
import no.entur.antu.validation.AntuNetexData;
import no.entur.antu.validation.AntuNetexValidator;
import no.entur.antu.validation.RuleCode;
import no.entur.antu.validation.ValidationError;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntryFactory;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
import org.entur.netex.validation.validator.xpath.XPathValidationContext;
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
    JAXBValidationContext validationContext,
    AntuNetexData antuNetexData
  ) {
    LOGGER.debug("Validating ServiceJourney non-increasing passing time");

    antuNetexData
      .validServiceJourneys()
      .forEach(serviceJourney ->
        validateServiceJourney(
          serviceJourney,
          antuNetexData,
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
    JAXBValidationContext validationContext,
    AntuNetexData antuNetexData
  ) {
    // ServiceJourneys and Line only appear in the Line file.
  }

  public void validateServiceJourney(
    ServiceJourney serviceJourney,
    AntuNetexData antuNetexData,
    Consumer<ValidationError> reportError
  ) {
    List<StopTime> sortedTimetabledPassingTime =
      SortStopTimesUtil.getSortedStopTimes(serviceJourney, antuNetexData);

    var previousPassingTime = sortedTimetabledPassingTime.get(0);
    if (
      validateStopTime(
        serviceJourney,
        antuNetexData,
        previousPassingTime,
        reportError
      )
    ) return;

    for (int i = 1; i < sortedTimetabledPassingTime.size(); i++) {
      var currentPassingTime = sortedTimetabledPassingTime.get(i);

      if (
        validateStopTime(
          serviceJourney,
          antuNetexData,
          currentPassingTime,
          reportError
        )
      ) return;

      if (!previousPassingTime.isStopTimesIncreasing(currentPassingTime)) {
        reportError.accept(
          new NonIncreasingPassingTimeError(
            NonIncreasingPassingTimeError.RuleCode.TIMETABLED_PASSING_TIME_NON_INCREASING_TIME,
            antuNetexData.getStopPointName(
              previousPassingTime.scheduledStopPointId()
            ),
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
    AntuNetexData antuNetexData,
    StopTime stopTime,
    Consumer<ValidationError> reportError
  ) {
    if (!stopTime.isComplete()) {
      reportError.accept(
        new NonIncreasingPassingTimeError(
          NonIncreasingPassingTimeError.RuleCode.TIMETABLED_PASSING_TIME_INCOMPLETE_TIME,
          antuNetexData.getStopPointName(stopTime.scheduledStopPointId()),
          serviceJourney.getId()
        )
      );
      return true;
    }
    if (!stopTime.isConsistent()) {
      reportError.accept(
        new NonIncreasingPassingTimeError(
          NonIncreasingPassingTimeError.RuleCode.TIMETABLED_PASSING_TIME_INCONSISTENT_TIME,
          antuNetexData.getStopPointName(stopTime.scheduledStopPointId()),
          serviceJourney.getId()
        )
      );
      return true;
    }
    return false;
  }
}
