package no.entur.antu.pipeline;

import static no.entur.antu.validation.ValidationProfile.TIMETABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import no.entur.antu.job.AntuJob;
import org.junit.jupiter.api.Test;

/**
 * The order the pipeline creates jobs in is what makes the cross-file validation rules work, so it is
 * asserted directly rather than inferred from the reports.
 */
class CommonFilesBarrierTest extends AntuPipelineTestBase {

  private static final String CODESPACE_AVI = "avi";

  @Test
  void lineFileJobsAreCreatedOnlyAfterTheCommonFilesAreValidated() {
    validate(
      CODESPACE_AVI,
      uploadTestDataset(CODESPACE_AVI, "rb_avi-aggregated-netex.zip"),
      TIMETABLE.id()
    );

    List<AntuJob> jobs = jobQueue.submittedJobs();

    int barrierIndex = indexOfFirst(jobs, AntuJob.CreateLineFileJobs.class);
    assertTrue(
      barrierIndex >= 0,
      "the dataset has common files, so the barrier must have been crossed"
    );
    assertTrue(
      jobs
        .stream()
        .anyMatch(job ->
          job instanceof AntuJob.ValidateFile validateFile &&
          validateFile.isCommonFile()
        ),
      "the dataset has common files"
    );

    for (int i = 0; i < jobs.size(); i++) {
      if (
        jobs.get(i) instanceof AntuJob.ValidateFile validateFile &&
        !validateFile.isCommonFile()
      ) {
        assertTrue(
          i > barrierIndex,
          "line file " +
          validateFile.netexFileName() +
          " was queued before the common files barrier opened"
        );
      }
    }
  }

  @Test
  void everyFileIsCountedTowardsTheReportAggregation() {
    validate(
      CODESPACE_AVI,
      uploadTestDataset(CODESPACE_AVI, "rb_avi-aggregated-netex.zip"),
      TIMETABLE.id()
    );

    List<AntuJob.ValidateFile> validateJobs = jobQueue
      .submittedJobs()
      .stream()
      .filter(AntuJob.ValidateFile.class::isInstance)
      .map(AntuJob.ValidateFile.class::cast)
      .toList();

    // Every file has to agree on the target, otherwise the barrier opens early or never at all.
    assertEquals(
      1,
      validateJobs
        .stream()
        .map(AntuJob.ValidateFile::nbNetexFiles)
        .distinct()
        .count()
    );
    assertEquals(
      validateJobs.size(),
      validateJobs.getFirst().nbNetexFiles(),
      "the aggregation target must match the number of files actually validated"
    );

    List<AntuJob.AggregateReports> aggregations = jobQueue
      .submittedJobs()
      .stream()
      .filter(AntuJob.AggregateReports.class::isInstance)
      .map(AntuJob.AggregateReports.class::cast)
      .toList();
    assertEquals(
      1,
      aggregations.size(),
      "the report aggregation must be triggered exactly once"
    );
    assertEquals(
      validateJobs.size(),
      aggregations.getFirst().netexFileNames().size()
    );
  }

  private static int indexOfFirst(List<AntuJob> jobs, Class<?> type) {
    for (int i = 0; i < jobs.size(); i++) {
      if (type.isInstance(jobs.get(i))) {
        return i;
      }
    }
    return -1;
  }
}
