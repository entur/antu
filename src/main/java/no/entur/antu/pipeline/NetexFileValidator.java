package no.entur.antu.pipeline;

import java.util.List;
import java.util.Optional;
import no.entur.antu.exception.AntuException;
import no.entur.antu.exception.RetryableAntuException;
import no.entur.antu.job.AntuJob;
import no.entur.antu.job.JobQueue;
import no.entur.antu.job.ValidationContext;
import no.entur.antu.job.ValidationMdc;
import no.entur.antu.memorystore.AntuMemoryStoreFileNotFoundException;
import no.entur.antu.validation.AntuNetexValidationProgressCallback;
import no.entur.antu.validation.NetexValidationProfile;
import no.entur.antu.validation.ValidationReportTransformer;
import no.entur.antu.validation.state.ValidationStateRepository;
import org.entur.netex.validation.validator.DataLocation;
import org.entur.netex.validation.validator.Severity;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Validates one NeTEx file and records its report.
 *
 * <p>Once the report is written, the file reports the barriers it belongs to: every file counts
 * towards the report aggregation, and a common file additionally counts towards the common files
 * barrier that releases the line files.
 */
@Component
public class NetexFileValidator {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    NetexFileValidator.class
  );

  /**
   * Beyond this, additional findings for the same rule say nothing new and only inflate the report.
   */
  private static final int MAX_REPORT_ENTRIES_PER_RULE = 50;

  private final NetexValidationProfile netexValidationProfile;
  private final ValidationStateRepository validationStateRepository;
  private final NetexFileStore netexFileStore;
  private final ValidationReportStore validationReportStore;
  private final ValidationBarrier validationBarrier;
  private final JobQueue jobQueue;
  private final ValidationReportTransformer reportTransformer =
    new ValidationReportTransformer(MAX_REPORT_ENTRIES_PER_RULE);

  public NetexFileValidator(
    NetexValidationProfile netexValidationProfile,
    ValidationStateRepository validationStateRepository,
    NetexFileStore netexFileStore,
    ValidationReportStore validationReportStore,
    ValidationBarrier validationBarrier,
    JobQueue jobQueue
  ) {
    this.netexValidationProfile = netexValidationProfile;
    this.validationStateRepository = validationStateRepository;
    this.netexFileStore = netexFileStore;
    this.validationReportStore = validationReportStore;
    this.validationBarrier = validationBarrier;
    this.jobQueue = jobQueue;
  }

  public void validate(AntuJob.ValidateFile job) {
    ValidationContext context = job.context();
    String netexFileName = job.netexFileName();
    ValidationMdc.setFileName(netexFileName);
    LOGGER.info("Validating NeTEx file {}", netexFileName);
    long startedAt = System.currentTimeMillis();

    Optional<ValidationReport> report = runValidators(context, netexFileName);
    if (report.isEmpty()) {
      return;
    }

    validationReportStore.saveFileReport(
      context,
      netexFileName,
      reportTransformer.truncate(report.get())
    );
    LOGGER.info(
      "Validated NeTEx file {} in {} ms",
      netexFileName,
      System.currentTimeMillis() - startedAt
    );

    openBarriers(job);
  }

  /**
   * @return empty when the file is gone from the memory store, which means this file has already
   *         been validated and the message is a duplicated delivery.
   */
  private Optional<ValidationReport> runValidators(
    ValidationContext context,
    String netexFileName
  ) {
    byte[] fileContent;
    try {
      fileContent =
        netexFileStore.read(context.validationReportId(), netexFileName);
    } catch (AntuMemoryStoreFileNotFoundException e) {
      LOGGER.info(
        "NeTEx file {} has already been validated and removed from the memory store. Ignoring.",
        netexFileName
      );
      return Optional.empty();
    }

    try {
      return Optional.of(
        netexValidationProfile.validate(
          context.validationProfile(),
          context.codespace(),
          context.validationReportId(),
          netexFileName,
          fileContent,
          new AntuNetexValidationProgressCallback(
            validationStateRepository,
            context.validationReportId()
          )
        )
      );
    } catch (Exception e) {
      if (RetryableFailures.isRetryable(e)) {
        LOGGER.info(
          "Retryable exception while validating {}, the file will be retried later: {}",
          netexFileName,
          e.getMessage(),
          e
        );
        throw new RetryableAntuException(
          "Retryable failure while validating " + netexFileName,
          e
        );
      }
      LOGGER.error(
        "System error while validating the NeTEx file {}: {}",
        netexFileName,
        e.getMessage(),
        e
      );
      return Optional.of(systemErrorReport(context, netexFileName));
    }
  }

  /**
   * A file that cannot be validated still has to produce a report, otherwise the dataset would wait
   * for it forever.
   */
  private static ValidationReport systemErrorReport(
    ValidationContext context,
    String netexFileName
  ) {
    ValidationReport report = new ValidationReport(
      context.codespace(),
      context.validationReportId()
    );
    report.addValidationReportEntry(
      new ValidationReportEntry(
        "System error while validating the file " + netexFileName,
        "SYSTEM_ERROR",
        Severity.ERROR,
        new DataLocation(null, netexFileName, null, null)
      )
    );
    return report;
  }

  private void openBarriers(AntuJob.ValidateFile job) {
    ValidationContext context = job.context();

    validationBarrier.arrive(
      ValidationBarrier.Stage.REPORTS_WRITTEN,
      context.validationReportId(),
      job.netexFileName(),
      job.nbNetexFiles(),
      fileNames ->
        jobQueue.submit(new AntuJob.AggregateReports(context, fileNames))
    );

    if (job.isCommonFile()) {
      validationBarrier.arrive(
        ValidationBarrier.Stage.COMMON_FILES_VALIDATED,
        context.validationReportId(),
        job.netexFileName(),
        job.nbCommonFiles(),
        ignored ->
          jobQueue.submit(
            new AntuJob.CreateLineFileJobs(context, allNetexFileNames(job))
          )
      );
    }
  }

  private static List<String> allNetexFileNames(AntuJob.ValidateFile job) {
    if (job.allNetexFileNames().isEmpty()) {
      throw new AntuException(
        "Common file " +
        job.netexFileName() +
        " carries no NeTEx file name list, cannot create the line file jobs"
      );
    }
    return job.allNetexFileNames();
  }
}
