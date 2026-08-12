package no.entur.antu.pipeline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import no.entur.antu.job.AntuJob;
import no.entur.antu.job.JobQueue;
import no.entur.antu.job.ValidationContext;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Merges the per file reports into one report for the dataset.
 *
 * <p>When the merged report already contains errors the dataset validators are skipped: they exist
 * to find problems that span files, and a dataset that is already failing does not need them.
 */
@Component
public class ReportAggregator {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    ReportAggregator.class
  );

  private final ValidationReportStore validationReportStore;
  private final ValidationCompleter validationCompleter;
  private final JobQueue jobQueue;

  public ReportAggregator(
    ValidationReportStore validationReportStore,
    ValidationCompleter validationCompleter,
    JobQueue jobQueue
  ) {
    this.validationReportStore = validationReportStore;
    this.validationCompleter = validationCompleter;
    this.jobQueue = jobQueue;
  }

  public void aggregate(AntuJob.AggregateReports job) {
    ValidationContext context = job.context();
    LOGGER.info("Merging {} individual reports", job.netexFileNames().size());
    long startedAt = System.currentTimeMillis();

    Merged result = merge(context, job.netexFileNames());
    LOGGER.info(
      "Completed reports merging in {} ms",
      System.currentTimeMillis() - startedAt
    );

    if (!result.lostFileNames().isEmpty()) {
      giveUp(context, result.lostFileNames());
      return;
    }

    ValidationReport merged = result.report();
    if (merged.hasError()) {
      LOGGER.info("Validation errors found, skipping dataset validation");
      validationCompleter.complete(context, merged);
      return;
    }
    validationReportStore.saveAggregatedReport(context, merged);
    jobQueue.submit(new AntuJob.ValidateDataset(context));
  }

  /**
   * The merged report, plus the files whose own report could not be found.
   */
  private record Merged(ValidationReport report, List<String> lostFileNames) {}

  /**
   * Merged in reverse name order, which is the entry order the published reports have always had.
   */
  private Merged merge(ValidationContext context, List<String> netexFileNames) {
    List<ValidationReportEntry> entries = new ArrayList<>();
    Map<String, Long> entriesPerRule = new HashMap<>();
    List<String> lostFileNames = new ArrayList<>();

    List<String> orderedFileNames = new ArrayList<>(netexFileNames);
    orderedFileNames.sort(Collections.reverseOrder());
    for (String netexFileName : orderedFileNames) {
      LOGGER.info("Merging file {}", netexFileName);
      Optional<ValidationReport> fileReport =
        validationReportStore.readFileReport(context, netexFileName);
      if (fileReport.isEmpty()) {
        lostFileNames.add(netexFileName);
        continue;
      }
      ValidationReport report = fileReport.get();
      entries.addAll(report.getValidationReportEntries());
      report
        .getNumberOfValidationEntriesPerRule()
        .forEach((rule, count) -> entriesPerRule.merge(rule, count, Long::sum));
    }

    return new Merged(
      new ValidationReport(
        context.codespace(),
        context.validationReportId(),
        entries,
        entriesPerRule
      ),
      lostFileNames
    );
  }

  /**
   * Every file writes its report before it arrives at the barrier, so a report missing here was lost after
   * that: expired on its TTL, or evicted while Redis was under memory pressure. Merging only what is left
   * would publish a report that looks complete, and can even look clean, for a dataset whose files were
   * never all accounted for.
   *
   * <p>Reported as {@code timeout} rather than {@code failed}, and with no report published at all, because
   * this says the dataset was not validated and not that the data is invalid. It is the same conclusion
   * {@code StalledValidationSweeper} reaches, and marduk maps it to {@code JobEvent.State.TIMEOUT}. The
   * wording matches the log-based metric so an operator finds out the cache is losing data.
   *
   * <p>Goes through {@link ValidationCompleter#abandon} rather than notifying directly: a second delivery
   * of this job reads the reports the first one cleaned up as lost, and would otherwise time out a
   * validation that has already reported {@code ok}.
   */
  private void giveUp(ValidationContext context, List<String> lostFileNames) {
    LOGGER.error(
      "System error: the validation reports for {} are gone, so the dataset cannot be reported as validated",
      lostFileNames
    );
    validationCompleter.abandon(context);
  }
}
