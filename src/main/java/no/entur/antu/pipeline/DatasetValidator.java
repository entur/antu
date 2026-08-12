package no.entur.antu.pipeline;

import java.util.Optional;
import no.entur.antu.exception.RetryableAntuException;
import no.entur.antu.job.AntuJob;
import no.entur.antu.job.ValidationContext;
import no.entur.antu.validation.AntuNetexValidationProgressCallback;
import no.entur.antu.validation.NetexValidationProfile;
import no.entur.antu.validation.state.ValidationStateRepository;
import org.entur.netex.validation.validator.DataLocation;
import org.entur.netex.validation.validator.Severity;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Runs the validators that need the whole dataset at once, on top of the merged report.
 */
@Component
public class DatasetValidator {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    DatasetValidator.class
  );

  private final NetexValidationProfile netexValidationProfile;
  private final ValidationStateRepository validationStateRepository;
  private final ValidationReportStore validationReportStore;
  private final ValidationCompleter validationCompleter;

  public DatasetValidator(
    NetexValidationProfile netexValidationProfile,
    ValidationStateRepository validationStateRepository,
    ValidationReportStore validationReportStore,
    ValidationCompleter validationCompleter
  ) {
    this.netexValidationProfile = netexValidationProfile;
    this.validationStateRepository = validationStateRepository;
    this.validationReportStore = validationReportStore;
    this.validationCompleter = validationCompleter;
  }

  public void validate(AntuJob.ValidateDataset job) {
    ValidationContext context = job.context();
    LOGGER.info(
      "Downloading the aggregated validation report for dataset validation"
    );
    Optional<ValidationReport> merged =
      validationReportStore.readAggregatedReport(context);
    if (merged.isEmpty()) {
      return;
    }

    validationCompleter.complete(
      context,
      runDatasetValidators(context, merged.get())
    );
  }

  private ValidationReport runDatasetValidators(
    ValidationContext context,
    ValidationReport mergedReport
  ) {
    try {
      ValidationReport report = netexValidationProfile.validateDataset(
        mergedReport,
        context.validationProfile(),
        new AntuNetexValidationProgressCallback(
          validationStateRepository,
          context.validationReportId()
        )
      );
      LOGGER.info("Completed all NeTEx dataset validators");
      return report;
    } catch (Exception e) {
      if (RetryableFailures.isRetryable(e)) {
        LOGGER.info(
          "Retryable exception while running dataset validation. Validation will be retried later: {}",
          e.getMessage(),
          e
        );
        throw new RetryableAntuException(
          "Retryable failure while running the dataset validators",
          e
        );
      }
      LOGGER.error(
        "System error while running dataset validation: {}",
        e.getMessage(),
        e
      );
      mergedReport.addValidationReportEntry(
        new ValidationReportEntry(
          "System error while running cross-validation of files in the dataset",
          "SYSTEM_ERROR",
          Severity.ERROR,
          new DataLocation(null, "N/A", null, null)
        )
      );
      return mergedReport;
    }
  }
}
