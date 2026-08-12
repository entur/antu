package no.entur.antu.pipeline;

import static no.entur.antu.Constants.BLOBSTORE_PATH_ANTU_EXCHANGE_INBOUND_RECEIVED;
import static no.entur.antu.Constants.BLOBSTORE_PATH_ANTU_REPORTS;
import static no.entur.antu.Constants.VALIDATION_REPORT_PREFIX;
import static no.entur.antu.Constants.VALIDATION_REPORT_SUFFIX;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import no.entur.antu.TestApp;
import no.entur.antu.config.TestConfig;
import no.entur.antu.job.ValidationContext;
import no.entur.antu.util.TestValidationReportUtil;
import org.entur.netex.validation.validator.ValidationReport;
import org.junit.jupiter.api.BeforeEach;
import org.rutebanken.helper.storage.repository.InMemoryBlobStoreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Runs the validation pipeline end to end in one JVM, against embedded Redis and an in-memory blob
 * store, with the job queue drained synchronously.
 *
 * <p>The Spring context is shared by every test in the suite. Validation report ids embed a
 * microsecond timestamp, so runs never collide in Redis or in the blob store and no state has to be
 * torn down between tests.
 */
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.NONE,
  classes = TestApp.class
)
@Import({ TestConfig.class, PipelineTestConfig.class })
@ActiveProfiles({ "test", "default", "in-memory-blobstore" })
public abstract class AntuPipelineTestBase {

  protected static final String VALIDATION_STAGE_PREVALIDATION =
    "EnturValidationStagePreValidation";

  @Autowired
  protected ValidationInitializer validationInitializer;

  @Autowired
  protected RecordingJobQueue jobQueue;

  @Autowired
  protected RecordingValidationStatusNotifier statusNotifier;

  @Autowired
  protected InMemoryBlobStoreRepository antuExchangeInMemoryBlobStoreRepository;

  @Autowired
  protected InMemoryBlobStoreRepository antuInMemoryBlobStoreRepository;

  @Value("${blobstore.gcs.antu.exchange.container.name}")
  private String antuExchangeContainerName;

  @Value("${blobstore.gcs.antu.container.name}")
  private String antuContainerName;

  @PostConstruct
  void initInMemoryBlobStoreRepositories() {
    antuExchangeInMemoryBlobStoreRepository.setContainerName(
      antuExchangeContainerName
    );
    antuInMemoryBlobStoreRepository.setContainerName(antuContainerName);
  }

  @BeforeEach
  void resetRecorders() {
    jobQueue.reset();
    statusNotifier.reset();
  }

  /**
   * Put a test dataset from the classpath where a validation client would have left it.
   */
  protected String uploadTestDataset(String codespace, String fileName) {
    InputStream dataset = getClass().getResourceAsStream('/' + fileName);
    assertNotNull(dataset, "Test dataset file not found: " + fileName);
    String datasetBlobName =
      BLOBSTORE_PATH_ANTU_EXCHANGE_INBOUND_RECEIVED +
      codespace +
      '/' +
      fileName;
    antuExchangeInMemoryBlobStoreRepository.uploadBlob(
      datasetBlobName,
      dataset
    );
    return datasetBlobName;
  }

  /**
   * Validate a dataset from start to finish. Returns when the validation client has been notified of
   * the outcome.
   */
  protected ValidationContext validate(
    String referential,
    String datasetBlobName,
    String validationProfile
  ) {
    return validationInitializer.initValidation(
      ValidationContext
        .builder()
        .referential(referential)
        .datasetFileHandle(datasetBlobName)
        .validationStage(VALIDATION_STAGE_PREVALIDATION)
        .validationClient(no.entur.antu.Constants.VALIDATION_CLIENT_MARDUK)
        .validationProfile(validationProfile)
        .build()
    );
  }

  protected ValidationReport publishedReport(ValidationContext context)
    throws IOException {
    String reportBlobName =
      BLOBSTORE_PATH_ANTU_REPORTS +
      context.referential() +
      VALIDATION_REPORT_PREFIX +
      context.validationReportId() +
      VALIDATION_REPORT_SUFFIX;
    InputStream report = antuInMemoryBlobStoreRepository.getBlob(
      reportBlobName
    );
    assertNotNull(
      report,
      "No validation report published at " + reportBlobName
    );
    return TestValidationReportUtil.getValidationReport(report);
  }
}
