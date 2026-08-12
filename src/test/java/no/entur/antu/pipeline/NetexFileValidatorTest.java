package no.entur.antu.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.function.Consumer;
import no.entur.antu.exception.AntuException;
import no.entur.antu.exception.RetryableAntuException;
import no.entur.antu.job.AntuJob;
import no.entur.antu.job.JobQueue;
import no.entur.antu.job.ValidationContext;
import no.entur.antu.memorystore.AntuMemoryStoreFileNotFoundException;
import no.entur.antu.validation.NetexValidationProfile;
import no.entur.antu.validation.state.ValidationStateRepository;
import org.entur.netex.validation.validator.ValidationReport;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * How a single file validation ends decides whether the message is acked, and whether the dataset can
 * ever finish. These are the four outcomes.
 */
class NetexFileValidatorTest {

  private static final ValidationContext CONTEXT = ValidationContext
    .builder()
    .referential("rb_tst")
    .codespace("tst")
    .validationReportId("reportId")
    .validationProfile("timetable")
    .build();

  private static final AntuJob.ValidateFile LINE_FILE_JOB =
    new AntuJob.ValidateFile(CONTEXT, "netex.xml", 1, 0, List.of());

  private NetexValidationProfile netexValidationProfile;
  private NetexFileStore netexFileStore;
  private ValidationReportStore validationReportStore;
  private ValidationBarrier validationBarrier;
  private JobQueue jobQueue;
  private NetexFileValidator validator;

  @BeforeEach
  void setUp() {
    netexValidationProfile = mock(NetexValidationProfile.class);
    netexFileStore = mock(NetexFileStore.class);
    validationReportStore = mock(ValidationReportStore.class);
    validationBarrier = mock(ValidationBarrier.class);
    jobQueue = mock(JobQueue.class);
    validator =
      new NetexFileValidator(
        netexValidationProfile,
        mock(ValidationStateRepository.class),
        netexFileStore,
        validationReportStore,
        validationBarrier,
        jobQueue
      );
    when(netexFileStore.read(anyString(), anyString()))
      .thenReturn("<netex/>".getBytes());
  }

  /**
   * Make the given stage's barrier open, handing {@code fileNames} to whatever callback the validator
   * passed in. By default no barrier opens, so the callback is never invoked.
   */
  @SuppressWarnings("unchecked")
  private void openBarrier(
    ValidationBarrier.Stage stage,
    List<String> fileNames
  ) {
    doAnswer(invocation -> {
        Consumer<List<String>> onOpen = invocation.getArgument(4);
        onOpen.accept(fileNames);
        return null;
      })
      .when(validationBarrier)
      .arrive(eq(stage), anyString(), anyString(), anyInt(), any());
  }

  /**
   * The file is gone from the memory store, so this file was already validated and the message is a
   * duplicated delivery. Writing another report would double-count it at the barrier.
   */
  @Test
  void anAlreadyValidatedFileIsIgnored() {
    when(netexFileStore.read(anyString(), anyString()))
      .thenThrow(new AntuMemoryStoreFileNotFoundException("gone"));

    validator.validate(LINE_FILE_JOB);

    verifyNoInteractions(validationReportStore, validationBarrier, jobQueue);
  }

  /**
   * A retryable failure has to propagate so the consumer nacks and PubSub redelivers.
   */
  @Test
  void aRetryableFailureIsRethrownAndNothingIsRecorded() {
    when(
      netexValidationProfile.validate(
        anyString(),
        anyString(),
        anyString(),
        anyString(),
        any(),
        any()
      )
    )
      .thenThrow(
        new RetryableAntuException(new IllegalStateException("redis"))
      );

    assertThrows(AntuException.class, () -> validator.validate(LINE_FILE_JOB));

    verifyNoInteractions(validationReportStore, validationBarrier, jobQueue);
  }

  /**
   * A permanent failure becomes a finding in the report. Failing the message instead would leave the
   * dataset waiting for a report that is never going to come.
   */
  @Test
  void aPermanentFailureIsReportedAsASystemError() {
    when(
      netexValidationProfile.validate(
        anyString(),
        anyString(),
        anyString(),
        anyString(),
        any(),
        any()
      )
    )
      .thenThrow(new IllegalArgumentException("malformed"));

    validator.validate(LINE_FILE_JOB);

    ArgumentCaptor<ValidationReport> report = ArgumentCaptor.forClass(
      ValidationReport.class
    );
    verify(validationReportStore)
      .saveFileReport(eq(CONTEXT), eq("netex.xml"), report.capture());
    assertTrue(report.getValue().hasError());
    assertEquals(
      List.of("SYSTEM_ERROR"),
      report
        .getValue()
        .getValidationReportEntries()
        .stream()
        .map(ValidationReportEntry::getName)
        .toList()
    );
    verify(validationBarrier)
      .arrive(
        eq(ValidationBarrier.Stage.REPORTS_WRITTEN),
        eq("reportId"),
        eq("netex.xml"),
        eq(1),
        any()
      );
  }

  @Test
  void aSuccessfulValidationRecordsTheReportAndReportsToTheBarrier() {
    when(
      netexValidationProfile.validate(
        anyString(),
        anyString(),
        anyString(),
        anyString(),
        any(),
        any()
      )
    )
      .thenReturn(new ValidationReport("tst", "reportId"));

    validator.validate(LINE_FILE_JOB);

    verify(validationReportStore)
      .saveFileReport(eq(CONTEXT), eq("netex.xml"), any());
    verify(validationBarrier)
      .arrive(
        eq(ValidationBarrier.Stage.REPORTS_WRITTEN),
        eq("reportId"),
        eq("netex.xml"),
        eq(1),
        any()
      );
    verifyNoInteractions(jobQueue);
  }

  @Test
  void aLineFileDoesNotTouchTheCommonFilesBarrier() {
    when(
      netexValidationProfile.validate(
        anyString(),
        anyString(),
        anyString(),
        anyString(),
        any(),
        any()
      )
    )
      .thenReturn(new ValidationReport("tst", "reportId"));

    validator.validate(LINE_FILE_JOB);

    verify(validationBarrier, never())
      .arrive(
        eq(ValidationBarrier.Stage.COMMON_FILES_VALIDATED),
        anyString(),
        anyString(),
        anyInt(),
        any()
      );
  }

  @Test
  void passingTheCommonFilesBarrierCreatesTheLineFileJobs() {
    when(
      netexValidationProfile.validate(
        anyString(),
        anyString(),
        anyString(),
        anyString(),
        any(),
        any()
      )
    )
      .thenReturn(new ValidationReport("tst", "reportId"));
    openBarrier(
      ValidationBarrier.Stage.COMMON_FILES_VALIDATED,
      List.of("_common.xml")
    );

    validator.validate(
      new AntuJob.ValidateFile(
        CONTEXT,
        "_common.xml",
        2,
        1,
        List.of("_common.xml", "line.xml")
      )
    );

    verify(jobQueue)
      .submit(
        new AntuJob.CreateLineFileJobs(
          CONTEXT,
          List.of("_common.xml", "line.xml")
        )
      );
  }

  @Test
  void passingTheReportBarrierStartsTheAggregation() {
    when(
      netexValidationProfile.validate(
        anyString(),
        anyString(),
        anyString(),
        anyString(),
        any(),
        any()
      )
    )
      .thenReturn(new ValidationReport("tst", "reportId"));
    openBarrier(ValidationBarrier.Stage.REPORTS_WRITTEN, List.of("netex.xml"));

    validator.validate(LINE_FILE_JOB);

    verify(jobQueue)
      .submit(new AntuJob.AggregateReports(CONTEXT, List.of("netex.xml")));
  }
}
