package no.entur.antu.validation.validator.interchange.waittime;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import no.entur.antu.validation.utilities.Comparison;
import org.entur.netex.validation.validator.AbstractDatasetValidator;
import org.entur.netex.validation.validator.DataLocation;
import org.entur.netex.validation.validator.Severity;
import org.entur.netex.validation.validator.ValidationIssue;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.entur.netex.validation.validator.ValidationReportEntryFactory;
import org.entur.netex.validation.validator.ValidationRule;
import org.entur.netex.validation.validator.jaxb.NetexDataRepository;
import org.entur.netex.validation.validator.model.ServiceJourneyInterchangeInfo;
import org.entur.netex.validation.validator.model.ServiceJourneyStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Verify that wait time is not above configured threshold.
 * Must check that the two vehicle journeys share at least one active date,
 * or in the case of interchanges around midnight; consecutive dates.
 * Chouette reference:
 * 3-Interchange-8-1,
 * 3-Interchange-8-2,
 * 3-Interchange-10
 */
public class UnexpectedWaitTimeAndActiveDatesValidator
  extends AbstractDatasetValidator {

  static final ValidationRule RULE_NO_SHARED_ACTIVE_DATE_FOUND_IN_INTERCHANGE =
    new ValidationRule(
      "NO_SHARED_ACTIVE_DATE_FOUND_IN_INTERCHANGE",
      "No shared active date found in interchange",
      "No shared active date found in interchange between %s and %s",
      Severity.WARNING
    );

  static final ValidationRule RULE_WAIT_TIME_IN_INTERCHANGE_EXCEEDS_WARNING_LIMIT =
    new ValidationRule(
      "WAIT_TIME_IN_INTERCHANGE_EXCEEDS_WARNING_LIMIT",
      "Wait time in interchange exceeds warning limit",
      "Wait time between stops (%s) and (%s) is expected %s sec. but was %s sec.",
      Severity.WARNING
    );

  static final ValidationRule RULE_WAIT_TIME_IN_INTERCHANGE_EXCEEDS_MAX_LIMIT =
    new ValidationRule(
      "WAIT_TIME_IN_INTERCHANGE_EXCEEDS_MAX_LIMIT",
      "Wait time in interchange exceeds maximum limit",
      "Wait time between stops (%s) and (%s) is expected %s sec. but was %s sec.",
      Severity.WARNING
    );

  private static final Logger LOGGER = LoggerFactory.getLogger(
    UnexpectedWaitTimeAndActiveDatesValidator.class
  );

  // Marduk/Chouette config parameter: interchange_max_wait_seconds = 3600 Seconds

  // Warning wait time for interchange is 1 hour
  private static final int INTERCHANGE_WARNING_WAIT_TIME_MILLIS = 3600000; // 1 Hour

  // Maximum wait time for interchange is 3 hours
  private static final int INTERCHANGE_ERROR_WAIT_TIME_MILLIS =
    INTERCHANGE_WARNING_WAIT_TIME_MILLIS * 3; // 3 Hours

  private final NetexDataRepository netexDataRepository;

  public UnexpectedWaitTimeAndActiveDatesValidator(
    ValidationReportEntryFactory validationReportEntryFactory,
    NetexDataRepository netexDataRepository
  ) {
    super(validationReportEntryFactory);
    this.netexDataRepository = netexDataRepository;
  }

  @Override
  public ValidationReport validate(ValidationReport validationReport) {
    LOGGER.info("Validating interchange wait time.");

    List<ServiceJourneyInterchangeInfo> serviceJourneyInterchangeInfos =
      netexDataRepository.serviceJourneyInterchangeInfos(
        validationReport.getValidationReportId()
      );

    if (
      serviceJourneyInterchangeInfos == null ||
      serviceJourneyInterchangeInfos.isEmpty()
    ) {
      return validationReport;
    }

    UnexpectedWaitTimeAndActiveDatesContext.Builder builder =
      new UnexpectedWaitTimeAndActiveDatesContext.Builder(
        validationReport.getValidationReportId(),
        netexDataRepository
      );

    builder.primeCache();

    serviceJourneyInterchangeInfos
      .stream()
      .map(builder::build)
      .filter(Objects::nonNull)
      .filter(UnexpectedWaitTimeAndActiveDatesContext::isValid)
      .map(this::validateWaitTime)
      .filter(Objects::nonNull)
      .forEach(validationReport::addValidationReportEntry);

    return validationReport;
  }

  private ValidationReportEntry validateWaitTime(
    UnexpectedWaitTimeAndActiveDatesContext context
  ) {
    long MILLIS_PER_DAY = 86400000L;

    int dayOffsetDiff =
      context.toServiceJourneyStop().departureDayOffset() -
      context.fromServiceJourneyStop().arrivalDayOffset();

    long msWait =
      (
        Optional
          .ofNullable(context.toServiceJourneyStop().departureTime())
          .map(LocalTime::toSecondOfDay)
          .orElse(0) -
        Optional
          .ofNullable(context.fromServiceJourneyStop().arrivalTime())
          .map(LocalTime::toSecondOfDay)
          .orElse(0)
      ) *
      1000L;

    if (msWait < 0) {
      msWait = MILLIS_PER_DAY + msWait;
      dayOffsetDiff--;
    }

    if (!hasSharedActiveDate(context, dayOffsetDiff)) {
      return createValidationReportEntry(
        new ValidationIssue(
          RULE_NO_SHARED_ACTIVE_DATE_FOUND_IN_INTERCHANGE,
          new DataLocation(
            context.serviceJourneyInterchangeInfo().interchangeId(),
            context.serviceJourneyInterchangeInfo().filename(),
            0,
            0
          ),
          context.fromServiceJourneyStop(),
          context.toServiceJourneyStop()
        )
      );
    } else if (msWait > INTERCHANGE_WARNING_WAIT_TIME_MILLIS) {
      if (msWait > INTERCHANGE_ERROR_WAIT_TIME_MILLIS) {
        return createValidationReportEntry(
          RULE_WAIT_TIME_IN_INTERCHANGE_EXCEEDS_WARNING_LIMIT,
          context.serviceJourneyInterchangeInfo().interchangeId(),
          context.serviceJourneyInterchangeInfo().filename(),
          context.fromServiceJourneyStop(),
          context.toServiceJourneyStop(),
          Comparison.of(
            String.valueOf(msWait / 1000),
            String.valueOf(INTERCHANGE_ERROR_WAIT_TIME_MILLIS / 1000)
          )
        );
      } else {
        return createValidationReportEntry(
          RULE_WAIT_TIME_IN_INTERCHANGE_EXCEEDS_MAX_LIMIT,
          context.serviceJourneyInterchangeInfo().interchangeId(),
          context.serviceJourneyInterchangeInfo().filename(),
          context.fromServiceJourneyStop(),
          context.toServiceJourneyStop(),
          Comparison.of(
            String.valueOf(msWait / 1000),
            String.valueOf(INTERCHANGE_WARNING_WAIT_TIME_MILLIS / 1000)
          )
        );
      }
    }
    return null;
  }

  private boolean hasSharedActiveDate(
    UnexpectedWaitTimeAndActiveDatesContext context,
    int daysOffset
  ) {
    List<LocalDate> fromServiceJourneyActiveDates =
      context.fromServiceJourneyActiveDates();
    for (LocalDate toServiceJourneyActiveDate : context.toServiceJourneyActiveDates()) {
      LocalDate toServiceJourneyActiveDateWithOffset =
        toServiceJourneyActiveDate.plusDays(daysOffset);
      if (
        fromServiceJourneyActiveDates.contains(
          toServiceJourneyActiveDateWithOffset
        )
      ) {
        return true;
      }
    }
    return false;
  }

  private ValidationReportEntry createValidationReportEntry(
    ValidationRule rule,
    String interchangeId,
    String filename,
    ServiceJourneyStop fromJourneyStop,
    ServiceJourneyStop toJourneyStop,
    Comparison<String> comparison
  ) {
    return createValidationReportEntry(
      new ValidationIssue(
        rule,
        new DataLocation(interchangeId, filename, 0, 0),
        fromJourneyStop.scheduledStopPointId(),
        toJourneyStop.scheduledStopPointId(),
        comparison.expected(),
        comparison.actual()
      )
    );
  }
}
