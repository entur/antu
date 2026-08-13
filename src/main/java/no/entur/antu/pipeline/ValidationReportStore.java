package no.entur.antu.pipeline;

import static no.entur.antu.Constants.BLOBSTORE_PATH_ANTU_REPORTS;
import static no.entur.antu.Constants.BLOBSTORE_PATH_ANTU_WORK;
import static no.entur.antu.Constants.VALIDATION_REPORT_PREFIX;
import static no.entur.antu.Constants.VALIDATION_REPORT_STATUS_SUFFIX;
import static no.entur.antu.Constants.VALIDATION_REPORT_SUFFIX;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;
import no.entur.antu.exception.AntuException;
import no.entur.antu.job.ValidationContext;
import no.entur.antu.memorystore.AntuMemoryStoreFileNotFoundException;
import no.entur.antu.memorystore.TemporaryFileRepository;
import no.entur.antu.services.AntuBlobStoreService;
import org.entur.netex.validation.validator.ValidationReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Reads and writes validation reports.
 *
 * <p>Per file reports and the merged dataset report are intermediate results and live in the memory
 * store. Only the final report is published to the antu bucket, where the REST API and the
 * validation clients read it from.
 */
@Component
public class ValidationReportStore {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    ValidationReportStore.class
  );

  /**
   * The name the merged report is filed under, alongside the per file reports. Cannot collide with
   * a NeTEx file name, which always ends in {@code .xml}.
   */
  private static final String AGGREGATED_REPORT_NAME = "aggregated";

  private final TemporaryFileRepository temporaryFileRepository;
  private final AntuBlobStoreService antuBlobStoreService;
  private final ObjectMapper objectMapper;

  public ValidationReportStore(
    TemporaryFileRepository temporaryFileRepository,
    AntuBlobStoreService antuBlobStoreService,
    @Qualifier("validationReportObjectMapper") ObjectMapper objectMapper
  ) {
    this.temporaryFileRepository = temporaryFileRepository;
    this.antuBlobStoreService = antuBlobStoreService;
    this.objectMapper = objectMapper;
  }

  public void saveFileReport(
    ValidationContext context,
    String netexFileName,
    ValidationReport report
  ) {
    temporaryFileRepository.upload(
      context.validationReportId(),
      workPath(context, netexFileName),
      serialize(report)
    );
  }

  /**
   * @return empty when the report is no longer in the memory store, which means it has already been
   *         merged into the dataset report and this is a duplicated delivery.
   */
  public Optional<ValidationReport> readFileReport(
    ValidationContext context,
    String netexFileName
  ) {
    return read(context, workPath(context, netexFileName));
  }

  public void saveAggregatedReport(
    ValidationContext context,
    ValidationReport report
  ) {
    temporaryFileRepository.upload(
      context.validationReportId(),
      workPath(context, AGGREGATED_REPORT_NAME),
      serialize(report)
    );
  }

  public Optional<ValidationReport> readAggregatedReport(
    ValidationContext context
  ) {
    return read(context, workPath(context, AGGREGATED_REPORT_NAME));
  }

  /**
   * Publish the final report to the antu bucket.
   */
  public void publishReport(
    ValidationContext context,
    ValidationReport report
  ) {
    String fileHandle = reportPath(context, VALIDATION_REPORT_SUFFIX);
    LOGGER.info("Uploading the aggregated validation report to {}", fileHandle);
    antuBlobStoreService.uploadBlob(
      fileHandle,
      new ByteArrayInputStream(serialize(report))
    );
  }

  /**
   * A marker written after the validation client has been notified.
   *
   * <p>It covers a crash between publishing the report and sending the notification: on redelivery
   * the report is published and the notification sent again only when the marker is missing.
   */
  public boolean reportAlreadyPublished(ValidationContext context) {
    return antuBlobStoreService.existBlob(
      reportPath(context, VALIDATION_REPORT_STATUS_SUFFIX)
    );
  }

  public void markReportPublished(ValidationContext context) {
    antuBlobStoreService.uploadBlob(
      reportPath(context, VALIDATION_REPORT_STATUS_SUFFIX),
      new ByteArrayInputStream("OK".getBytes())
    );
  }

  private Optional<ValidationReport> read(
    ValidationContext context,
    String path
  ) {
    byte[] content;
    try {
      content =
        temporaryFileRepository.download(context.validationReportId(), path);
    } catch (AntuMemoryStoreFileNotFoundException e) {
      LOGGER.warn(
        "Validation report {} is no longer in the memory store. Ignoring.",
        path
      );
      return Optional.empty();
    }
    return Optional.of(deserialize(content));
  }

  private byte[] serialize(ValidationReport report) {
    try {
      return objectMapper.writeValueAsBytes(report);
    } catch (IOException e) {
      throw new AntuException("Failed to serialize a validation report", e);
    }
  }

  private ValidationReport deserialize(byte[] content) {
    try {
      return objectMapper.readValue(content, ValidationReport.class);
    } catch (IOException e) {
      throw new AntuException("Failed to parse a validation report", e);
    }
  }

  private static String workPath(ValidationContext context, String fileName) {
    return (
      BLOBSTORE_PATH_ANTU_WORK +
      context.referential() +
      '/' +
      context.validationReportId() +
      '/' +
      fileName +
      VALIDATION_REPORT_SUFFIX
    );
  }

  private static String reportPath(ValidationContext context, String suffix) {
    return (
      BLOBSTORE_PATH_ANTU_REPORTS +
      context.referential() +
      VALIDATION_REPORT_PREFIX +
      context.validationReportId() +
      suffix
    );
  }
}
