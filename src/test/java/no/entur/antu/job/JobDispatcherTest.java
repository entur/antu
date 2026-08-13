package no.entur.antu.job;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import no.entur.antu.pipeline.DatasetSplitter;
import no.entur.antu.pipeline.DatasetValidator;
import no.entur.antu.pipeline.NetexFileValidator;
import no.entur.antu.pipeline.OrganisationAliasCacheRefresher;
import no.entur.antu.pipeline.ReportAggregator;
import no.entur.antu.pipeline.StopPlaceCacheRefresher;
import no.entur.antu.pipeline.ValidationCompleter;
import no.entur.antu.validation.state.ValidationStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * PubSub delivers at least once, so the dispatcher has to recognise a job whose validation is already
 * finished. Doing the work again would republish a report and re-notify the client.
 */
class JobDispatcherTest {

  private static final String LIVE_REPORT_ID = "live-report";
  private static final String FINISHED_REPORT_ID = "finished-report";

  private static final ValidationContext LIVE_CONTEXT = ValidationContext
    .builder()
    .referential("rb_tst")
    .validationReportId(LIVE_REPORT_ID)
    .build();

  private static final ValidationContext FINISHED_CONTEXT = ValidationContext
    .builder()
    .referential("rb_tst")
    .validationReportId(FINISHED_REPORT_ID)
    .build();

  private DatasetSplitter datasetSplitter;
  private NetexFileValidator netexFileValidator;
  private ReportAggregator reportAggregator;
  private DatasetValidator datasetValidator;
  private ValidationCompleter validationCompleter;
  private StopPlaceCacheRefresher stopPlaceCacheRefresher;
  private OrganisationAliasCacheRefresher organisationAliasCacheRefresher;
  private JobDispatcher dispatcher;

  @BeforeEach
  void setUp() {
    ValidationStateRepository validationStateRepository = mock(
      ValidationStateRepository.class
    );
    // recordProgress both answers "is this validation still running" and keeps the stalled-validation
    // sweep from giving up on it, so it is the one call the dispatcher makes.
    when(validationStateRepository.recordProgress(LIVE_REPORT_ID))
      .thenReturn(true);
    when(validationStateRepository.recordProgress(FINISHED_REPORT_ID))
      .thenReturn(false);

    datasetSplitter = mock(DatasetSplitter.class);
    netexFileValidator = mock(NetexFileValidator.class);
    reportAggregator = mock(ReportAggregator.class);
    datasetValidator = mock(DatasetValidator.class);
    validationCompleter = mock(ValidationCompleter.class);
    stopPlaceCacheRefresher = mock(StopPlaceCacheRefresher.class);
    organisationAliasCacheRefresher =
      mock(OrganisationAliasCacheRefresher.class);

    dispatcher =
      new JobDispatcher(
        validationStateRepository,
        datasetSplitter,
        netexFileValidator,
        reportAggregator,
        datasetValidator,
        validationCompleter,
        stopPlaceCacheRefresher,
        organisationAliasCacheRefresher
      );
  }

  @Test
  void aJobForAFinishedValidationIsIgnored() {
    dispatcher.dispatch(
      new AntuJob.ValidateFile(FINISHED_CONTEXT, "a.xml", 1, 0, List.of())
    );

    verifyNoInteractions(netexFileValidator);
  }

  @Test
  void aJobForARunningValidationIsExecuted() {
    AntuJob.ValidateFile job = new AntuJob.ValidateFile(
      LIVE_CONTEXT,
      "a.xml",
      1,
      0,
      List.of()
    );

    dispatcher.dispatch(job);

    verify(netexFileValidator).validate(job);
  }

  /**
   * Cache refreshes belong to no validation, so the completeness check must not apply to them.
   */
  @Test
  void cacheRefreshJobsAreAlwaysExecuted() {
    dispatcher.dispatch(new AntuJob.RefreshStopPlaceCache());
    dispatcher.dispatch(new AntuJob.RefreshOrganisationAliasCache());

    verify(stopPlaceCacheRefresher).refresh();
    verify(organisationAliasCacheRefresher).refresh();
  }

  @Test
  void everyValidationJobTypeReachesItsStep() {
    dispatcher.dispatch(new AntuJob.SplitDataset(LIVE_CONTEXT));
    dispatcher.dispatch(
      new AntuJob.CreateLineFileJobs(LIVE_CONTEXT, List.of("a.xml"))
    );
    dispatcher.dispatch(
      new AntuJob.AggregateReports(LIVE_CONTEXT, List.of("a.xml"))
    );
    dispatcher.dispatch(new AntuJob.ValidateDataset(LIVE_CONTEXT));
    dispatcher.dispatch(new AntuJob.CompleteValidation(LIVE_CONTEXT));

    verify(datasetSplitter).split(any(AntuJob.SplitDataset.class));
    verify(datasetSplitter)
      .createLineFileJobs(any(AntuJob.CreateLineFileJobs.class));
    verify(reportAggregator).aggregate(any(AntuJob.AggregateReports.class));
    verify(datasetValidator).validate(any(AntuJob.ValidateDataset.class));
    verify(validationCompleter).complete(LIVE_CONTEXT);
  }
}
