package no.entur.antu.pipeline;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import no.entur.antu.job.AntuJob;
import no.entur.antu.job.JobQueue;
import no.entur.antu.job.ValidationContext;
import org.entur.netex.validation.validator.ValidationReport;
import org.junit.jupiter.api.Test;

class ReportAggregatorTest {

  private static final ValidationContext CONTEXT = ValidationContext
    .builder()
    .referential("rb_tst")
    .codespace("tst")
    .validationReportId("rb_tst_20260811103000000000")
    .build();

  private final ValidationReportStore validationReportStore = mock(
    ValidationReportStore.class
  );
  private final ValidationCompleter validationCompleter = mock(
    ValidationCompleter.class
  );
  private final JobQueue jobQueue = mock(JobQueue.class);
  private final ReportAggregator aggregator = new ReportAggregator(
    validationReportStore,
    validationCompleter,
    jobQueue
  );

  /**
   * Every file writes its report before arriving at the barrier, so a report missing at merge time was lost
   * afterwards: expired on its TTL, or evicted while Redis was under memory pressure. Merging what remains
   * would publish a report that looks complete, and can look clean, for a dataset whose files were never all
   * accounted for. That is a wrong answer sent to marduk, not a degraded one.
   *
   * <p>The status is timeout, not failed: the dataset was not validated, which is a different thing from the
   * data being invalid. Same conclusion the sweeper reaches, and marduk maps it to JobEvent.State.TIMEOUT.
   *
   * <p>Routed through the completer rather than notified here, so that a second delivery of this job, which
   * reads the reports the first one cleaned up as lost, cannot contradict an already published OK. The
   * guard itself is pinned by ValidationCompleterTest.
   */
  @Test
  void aLostFileReportEndsTheValidationAsTimeout() {
    when(validationReportStore.readFileReport(CONTEXT, "line1.xml"))
      .thenReturn(Optional.of(cleanReport()));
    when(validationReportStore.readFileReport(CONTEXT, "line2.xml"))
      .thenReturn(Optional.empty());

    aggregate("line1.xml", "line2.xml");

    verify(validationCompleter).abandon(CONTEXT);
  }

  /**
   * Nothing may be published from an incomplete merge, and the dataset validators must not run on it.
   */
  @Test
  void aLostFileReportPublishesNothing() {
    when(validationReportStore.readFileReport(CONTEXT, "line1.xml"))
      .thenReturn(Optional.empty());

    aggregate("line1.xml");

    verifyNoInteractions(jobQueue);
    verify(validationReportStore, never())
      .saveAggregatedReport(eq(CONTEXT), any());
    verify(validationCompleter, never()).complete(eq(CONTEXT), any());
  }

  /**
   * The ordinary path has to stay intact: all reports present and clean means the dataset validators still
   * get their turn.
   */
  @Test
  void aCompleteCleanMergeGoesOnToDatasetValidation() {
    when(validationReportStore.readFileReport(CONTEXT, "line1.xml"))
      .thenReturn(Optional.of(cleanReport()));
    when(validationReportStore.readFileReport(CONTEXT, "line2.xml"))
      .thenReturn(Optional.of(cleanReport()));

    aggregate("line1.xml", "line2.xml");

    verify(jobQueue).submit(new AntuJob.ValidateDataset(CONTEXT));
    verifyNoInteractions(validationCompleter);
  }

  private void aggregate(String... netexFileNames) {
    aggregator.aggregate(
      new AntuJob.AggregateReports(CONTEXT, List.of(netexFileNames))
    );
  }

  private static ValidationReport cleanReport() {
    return new ValidationReport(
      CONTEXT.codespace(),
      CONTEXT.validationReportId()
    );
  }
}
