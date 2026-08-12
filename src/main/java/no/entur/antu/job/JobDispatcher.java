package no.entur.antu.job;

import no.entur.antu.pipeline.DatasetSplitter;
import no.entur.antu.pipeline.DatasetValidator;
import no.entur.antu.pipeline.NetexFileValidator;
import no.entur.antu.pipeline.OrganisationAliasCacheRefresher;
import no.entur.antu.pipeline.ReportAggregator;
import no.entur.antu.pipeline.StopPlaceCacheRefresher;
import no.entur.antu.pipeline.ValidationCompleter;
import no.entur.antu.validation.state.ValidationStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Runs one job.
 *
 * <p>This is the whole of antu's control flow: every step of a validation ends by putting the next
 * job on the queue, and lands back here on whichever pod picks it up.
 */
@Component
public class JobDispatcher {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    JobDispatcher.class
  );

  private final ValidationStateRepository validationStateRepository;
  private final DatasetSplitter datasetSplitter;
  private final NetexFileValidator netexFileValidator;
  private final ReportAggregator reportAggregator;
  private final DatasetValidator datasetValidator;
  private final ValidationCompleter validationCompleter;
  private final StopPlaceCacheRefresher stopPlaceCacheRefresher;
  private final OrganisationAliasCacheRefresher organisationAliasCacheRefresher;

  public JobDispatcher(
    ValidationStateRepository validationStateRepository,
    DatasetSplitter datasetSplitter,
    NetexFileValidator netexFileValidator,
    ReportAggregator reportAggregator,
    DatasetValidator datasetValidator,
    ValidationCompleter validationCompleter,
    StopPlaceCacheRefresher stopPlaceCacheRefresher,
    OrganisationAliasCacheRefresher organisationAliasCacheRefresher
  ) {
    this.validationStateRepository = validationStateRepository;
    this.datasetSplitter = datasetSplitter;
    this.netexFileValidator = netexFileValidator;
    this.reportAggregator = reportAggregator;
    this.datasetValidator = datasetValidator;
    this.validationCompleter = validationCompleter;
    this.stopPlaceCacheRefresher = stopPlaceCacheRefresher;
    this.organisationAliasCacheRefresher = organisationAliasCacheRefresher;
  }

  /**
   * The cache refresh arms bind a name they never use (java:S1481), for the reason given on
   * {@code JobMessageCodec.encode}: prettier-java rejects unnamed patterns, and a {@code default} arm would
   * cost the exhaustiveness check.
   */
  @SuppressWarnings("java:S1481")
  public void dispatch(AntuJob job) {
    if (job instanceof AntuJob.ValidationJob validationJob) {
      ValidationMdc.set(validationJob.context());
      if (isObsolete(validationJob)) {
        LOGGER.info(
          "Report {} is already complete. Ignoring job {}.",
          validationJob.context().validationReportId(),
          job.type()
        );
        return;
      }
    }

    switch (job) {
      case AntuJob.SplitDataset splitDataset -> datasetSplitter.split(
        splitDataset
      );
      case AntuJob.ValidateFile validateFile -> netexFileValidator.validate(
        validateFile
      );
      case AntuJob.CreateLineFileJobs createLineFileJobs -> datasetSplitter.createLineFileJobs(
        createLineFileJobs
      );
      case AntuJob.AggregateReports aggregateReports -> reportAggregator.aggregate(
        aggregateReports
      );
      case AntuJob.ValidateDataset validateDataset -> datasetValidator.validate(
        validateDataset
      );
      // Sonar suggests a record pattern here (java:S6878); prettier-java cannot parse one.
      case AntuJob.CompleteValidation completeValidation -> validationCompleter.complete(
        completeValidation.context()
      );
      case AntuJob.RefreshStopPlaceCache ignored -> stopPlaceCacheRefresher.refresh();
      case AntuJob.RefreshOrganisationAliasCache ignored -> organisationAliasCacheRefresher.refresh();
    }
  }

  /**
   * A validation whose state has been cleaned up is finished, so any job still referring to it is a
   * duplicated delivery and doing the work again would overwrite a published report.
   *
   * <p>Recording progress here rather than only checking is what keeps the stalled-validation sweep from
   * giving up on a large dataset that is still working through its files.
   */
  private boolean isObsolete(AntuJob.ValidationJob job) {
    String validationReportId = job.context().validationReportId();
    return (
      validationReportId != null &&
      !validationStateRepository.recordProgress(validationReportId)
    );
  }
}
