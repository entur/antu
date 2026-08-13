package no.entur.antu.pipeline;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import no.entur.antu.config.cache.ValidationState;
import no.entur.antu.job.AntuJob;
import no.entur.antu.job.JobQueue;
import no.entur.antu.job.ValidationContext;
import no.entur.antu.job.ValidationMdc;
import no.entur.antu.job.ValidationStatus;
import no.entur.antu.job.ValidationStatusNotifier;
import no.entur.antu.validation.state.ValidationStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Turns a validation request from marduk or kakka into a running validation.
 */
@Component
public class ValidationInitializer {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    ValidationInitializer.class
  );

  private static final DateTimeFormatter REPORT_ID_TIMESTAMP_FORMAT =
    DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSSSSS");

  private final ValidationStateRepository validationStateRepository;
  private final ValidationStatusNotifier validationStatusNotifier;
  private final JobQueue jobQueue;

  public ValidationInitializer(
    ValidationStateRepository validationStateRepository,
    ValidationStatusNotifier validationStatusNotifier,
    JobQueue jobQueue
  ) {
    this.validationStateRepository = validationStateRepository;
    this.validationStatusNotifier = validationStatusNotifier;
    this.jobQueue = jobQueue;
  }

  public ValidationContext initValidation(ValidationContext request) {
    ValidationContext context = withDerivedIdentity(request);
    ValidationMdc.set(context);

    validationStateRepository.createValidationStateIfMissing(
      context.validationReportId(),
      new ValidationState(context, Instant.now())
    );
    try {
      validationStatusNotifier.notifyStatus(context, ValidationStatus.STARTED);
      LOGGER.info("Starting validation of {}", context.datasetFileHandle());
      jobQueue.submit(new AntuJob.SplitDataset(context));
    } catch (RuntimeException e) {
      // Nothing will ever complete this report: the redelivery of the request derives a new id from the
      // clock, so this state would sit untouched until the sweep reported a timeout for it, half an hour
      // after the retry had already reported ok or failed. Dropping it leaves the retry as the only
      // validation. The client sees a second STARTED, which marduk treats as the same state it is in.
      LOGGER.warn(
        "Could not start the validation of {}, abandoning report {} so the redelivery is the only validation",
        context.datasetFileHandle(),
        context.validationReportId()
      );
      // Only the state exists this early, so this is the whole of the clean-up.
      validationStateRepository.cleanUp(context.validationReportId());
      throw e;
    }
    return context;
  }

  /**
   * The codespace is the referential without the {@code rb_} prefix, and the report id identifies
   * this run of the validation for the lifetime of the report in the bucket.
   */
  private static ValidationContext withDerivedIdentity(
    ValidationContext request
  ) {
    String referential = request.referential();
    String correlationId = request.correlationId();
    return request.withIdentity(
      referential == null ? null : referential.replace("rb_", ""),
      referential +
      '_' +
      // The pod's own zone, which is TZ=Europe/Oslo in the chart. Spelled out because the report id is
      // wire-visible: switching to UTC would shift every id by an hour or two.
      REPORT_ID_TIMESTAMP_FORMAT.format(
        LocalDateTime.now(ZoneId.systemDefault())
      ),
      // Clients normally send one; mint one rather than lose traceability if a client does not.
      correlationId == null || correlationId.isEmpty()
        ? UUID.randomUUID().toString()
        : correlationId
    );
  }
}
