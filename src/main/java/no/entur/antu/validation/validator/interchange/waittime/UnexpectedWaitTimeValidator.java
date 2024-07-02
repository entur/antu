package no.entur.antu.validation.validator.interchange.waittime;

import no.entur.antu.validation.AntuNetexData;
import no.entur.antu.validation.AntuNetexValidator;
import no.entur.antu.validation.RuleCode;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntryFactory;
import org.entur.netex.validation.validator.xpath.ValidationContext;

/**
 * Verify that wait time is not above configured threshold.
 * Must check that the two vehicle journeys share at least one active date,
 * or in the case of interchanges around midnight; consecutive dates.
 * Chouette reference:
 * 3-Interchange-8-1,
 * 3-Interchange-8-2,
 * 3-Interchange-10
 */
public class UnexpectedWaitTimeValidator extends AntuNetexValidator {

  // interchange_max_wait_seconds
  private final int INTERCHANGE_MAX_WAIT_TIME_SECONDS = 3600;

  protected UnexpectedWaitTimeValidator(
    ValidationReportEntryFactory validationReportEntryFactory
  ) {
    super(validationReportEntryFactory);
  }

  @Override
  protected RuleCode[] getRuleCodes() {
    return new RuleCode[0];
  }

  @Override
  protected void validateLineFile(
    ValidationReport validationReport,
    ValidationContext validationContext,
    AntuNetexData antuNetexData
  ) {
    antuNetexData
      .serviceJourneyInterchanges()
      .map(interchange ->
        UnexpectedWaitTimeContext.of(antuNetexData, interchange)
      )
      .filter(UnexpectedWaitTimeContext::isValid)
      .forEach(context -> validateWaitTime(context, validationReport));
  }

  private void validateWaitTime(
    UnexpectedWaitTimeContext context,
    ValidationReport validationReport
  ) {
    int dayOffsetDiff =
      context.toServiceJourneyStop().departureDayOffset() -
      context.fromServiceJourneyStop().arrivalDayOffset();

    long msWait =
      (
        context.toServiceJourneyStop().departureTime().toSecondOfDay() -
        context.fromServiceJourneyStop().arrivalTime().toSecondOfDay()
      ) *
      1000L;

    if (msWait < 0) {
      msWait = 86400000L + msWait;
      dayOffsetDiff--;
    }
  }
}
