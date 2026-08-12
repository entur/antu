package no.entur.antu.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import no.entur.antu.job.AntuJob;
import no.entur.antu.job.JobQueue;
import no.entur.antu.job.ValidationContext;
import no.entur.antu.job.ValidationStatus;
import no.entur.antu.job.ValidationStatusNotifier;
import no.entur.antu.validation.state.ValidationStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ValidationInitializerTest {

  private static final ValidationContext REQUEST = ValidationContext
    .builder()
    .referential("rb_tst")
    .correlationId("correlation-1")
    .datasetFileHandle("inbound/received/tst/dataset.zip")
    .build();

  private ValidationStateRepository validationStateRepository;
  private ValidationStatusNotifier validationStatusNotifier;
  private JobQueue jobQueue;
  private ValidationInitializer initializer;

  @BeforeEach
  void setUp() {
    validationStateRepository = mock(ValidationStateRepository.class);
    validationStatusNotifier = mock(ValidationStatusNotifier.class);
    jobQueue = mock(JobQueue.class);
    initializer =
      new ValidationInitializer(
        validationStateRepository,
        validationStatusNotifier,
        jobQueue
      );
  }

  @Test
  void aRequestGetsACodespaceAReportIdAndAStartedStatus() {
    ValidationContext context = initializer.initValidation(REQUEST);

    assertEquals("tst", context.codespace());
    assertTrue(
      context.validationReportId().startsWith("rb_tst_"),
      "got " + context.validationReportId()
    );
    verify(validationStatusNotifier)
      .notifyStatus(context, ValidationStatus.STARTED);
    verify(jobQueue).submit(new AntuJob.SplitDataset(context));
  }

  @Test
  void aRequestWithoutACorrelationIdGetsOne() {
    ValidationContext context = initializer.initValidation(
      ValidationContext.builder().referential("rb_tst").build()
    );

    assertNotEquals(null, context.correlationId());
  }

  /**
   * The report id comes from the clock, so a redelivered request starts a second validation under a new id.
   * Leaving the first one's state behind would have the sweep report a timeout for it half an hour after the
   * redelivery had already reported ok, giving one client request two contradictory terminal statuses.
   */
  @Test
  void aFailedHandOffAbandonsItsOwnReportRatherThanLeaveItToTimeOut() {
    doThrow(new IllegalStateException("the topic is unreachable"))
      .when(jobQueue)
      .submit(any());

    assertThrows(
      IllegalStateException.class,
      () -> initializer.initValidation(REQUEST)
    );

    verify(validationStateRepository).cleanUp(any());
  }

  /**
   * Two deliveries of one request do not share a report id. This is what makes the clean-up above necessary,
   * and it is unchanged from the Camel implementation.
   */
  @Test
  void twoDeliveriesOfTheSameRequestGetDifferentReportIds() {
    String first = initializer.initValidation(REQUEST).validationReportId();
    String second = initializer.initValidation(REQUEST).validationReportId();

    assertNotEquals(first, second);
  }
}
