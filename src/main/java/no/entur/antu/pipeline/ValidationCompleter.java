package no.entur.antu.pipeline;

import static no.entur.antu.config.cache.CacheConfig.VALIDATION_DATA_TTL;

import no.entur.antu.job.ValidationContext;
import no.entur.antu.job.ValidationStatus;
import no.entur.antu.job.ValidationStatusNotifier;
import no.entur.antu.metrics.AntuPrometheusMetricsService;
import org.entur.netex.validation.validator.ValidationReport;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Publishes the final report, tells the validation client how it went and releases the resources the
 * validation held.
 */
@Component
public class ValidationCompleter {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    ValidationCompleter.class
  );

  private static final String COMPLETION_CLAIM_PREFIX = "COMPLETING_";

  private final ValidationReportStore validationReportStore;
  private final ValidationStatusNotifier validationStatusNotifier;
  private final AntuPrometheusMetricsService antuPrometheusMetricsService;
  private final ValidationCacheCleaner validationCacheCleaner;
  private final RedissonClient redissonClient;

  public ValidationCompleter(
    ValidationReportStore validationReportStore,
    ValidationStatusNotifier validationStatusNotifier,
    AntuPrometheusMetricsService antuPrometheusMetricsService,
    ValidationCacheCleaner validationCacheCleaner,
    RedissonClient redissonClient
  ) {
    this.validationReportStore = validationReportStore;
    this.validationStatusNotifier = validationStatusNotifier;
    this.antuPrometheusMetricsService = antuPrometheusMetricsService;
    this.validationCacheCleaner = validationCacheCleaner;
    this.redissonClient = redissonClient;
  }

  /**
   * Complete a validation whose report is already in the memory store.
   */
  public void complete(ValidationContext context) {
    validationReportStore
      .readAggregatedReport(context)
      .ifPresent(report -> complete(context, report));
  }

  /**
   * Publish the report and tell the client it is done.
   */
  public void complete(ValidationContext context, ValidationReport report) {
    concludeOnce(context, () -> publishAndNotify(context, report));
  }

  /**
   * Conclude a validation antu could not finish, as {@code timeout} rather than {@code failed}: it says
   * antu could not validate the dataset, not that the dataset is invalid.
   *
   * <p>Guarded like {@link #complete}, and for a sharper reason. Without the guards a validation that has
   * already reported {@code ok} can be reported as timed out afterwards, and marduk maps {@code timeout}
   * to a failed import, so a dataset that validated cleanly fails. There are two ways in: a duplicated
   * {@code AggregateReports} delivery reads the per file reports the first delivery already cleaned up as
   * lost, and the sweep re-reads its state through an {@code RLocalCachedMap} whose invalidations are
   * pub/sub only, so after a reconnect it can still see a finished validation as stalled.
   *
   * <p>Takes the same claim as {@link #complete}, so the two cannot both conclude one report in either
   * order.
   */
  public void abandon(ValidationContext context) {
    concludeOnce(
      context,
      () -> {
        validationStatusNotifier.notifyStatus(
          context,
          ValidationStatus.TIMEOUT
        );
        // The marker records that a terminal status was sent, not that a report is readable. It is what
        // stops a redelivery arriving after the Redis claim's TTL has lapsed sending a second one.
        validationReportStore.markReportPublished(context);
        validationCacheCleaner.cleanUp(context.validationReportId());
      }
    );
  }

  /**
   * Two guards, because they cover different things. The marker is durable and catches a redelivery that
   * arrives after the client was told, however much later. The Redis claim is atomic and catches two
   * deliveries being worked on at the same instant, which the marker cannot: both would find it missing and
   * both would notify. Any pod concludes validations now, so that is a real race rather than a theoretical
   * one.
   *
   * <p>The marker is checked first, so a redelivery still gets to retry a clean-up that failed.
   */
  private void concludeOnce(ValidationContext context, Runnable conclusion) {
    String validationReportId = context.validationReportId();
    if (validationReportStore.reportAlreadyPublished(context)) {
      LOGGER.warn(
        "The report for {} already has a terminal status. Ignoring.",
        validationReportId
      );
      // Clean up again rather than just returning: a failed clean-up is the likeliest reason this job came
      // back at all, and leaving the state live lets the sweeper send a TIMEOUT contradicting the OK or
      // FAILED the client already has. Every step of it is a delete, so repeating it costs nothing.
      validationCacheCleaner.cleanUp(validationReportId);
      return;
    }

    RBucket<Boolean> claim = redissonClient.getBucket(
      COMPLETION_CLAIM_PREFIX + validationReportId
    );
    if (!claim.setIfAbsent(Boolean.TRUE, VALIDATION_DATA_TTL)) {
      LOGGER.warn(
        "Another pod is already concluding the report for {}. Ignoring.",
        validationReportId
      );
      return;
    }

    RedisClaim.runOrRelease(
      claim,
      "the completion claim for report " + validationReportId,
      conclusion
    );
  }

  private void publishAndNotify(
    ValidationContext context,
    ValidationReport report
  ) {
    ValidationStatus status;
    if (report.hasError()) {
      status = ValidationStatus.FAILED;
      LOGGER.info("Validation errors found");
    } else {
      status = ValidationStatus.OK;
      LOGGER.info("No validation error");
      antuPrometheusMetricsService.validationReportMetrics(report);
    }

    validationReportStore.publishReport(context, report);
    validationStatusNotifier.notifyStatus(context, status);
    validationReportStore.markReportPublished(context);
    validationCacheCleaner.cleanUp(context.validationReportId());
  }
}
