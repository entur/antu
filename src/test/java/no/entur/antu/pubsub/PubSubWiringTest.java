package no.entur.antu.pubsub;

import static no.entur.antu.Constants.BLOBSTORE_PATH_ANTU_EXCHANGE_INBOUND_RECEIVED;
import static no.entur.antu.Constants.DATASET_REFERENTIAL;
import static no.entur.antu.Constants.STATUS_VALIDATION_STARTED;
import static no.entur.antu.Constants.VALIDATION_CLIENT_HEADER;
import static no.entur.antu.Constants.VALIDATION_CLIENT_MARDUK;
import static no.entur.antu.Constants.VALIDATION_CORRELATION_ID_HEADER;
import static no.entur.antu.Constants.VALIDATION_DATASET_FILE_HANDLE_HEADER;
import static no.entur.antu.Constants.VALIDATION_PROFILE_HEADER;
import static no.entur.antu.Constants.VALIDATION_REPORT_ID_HEADER;
import static no.entur.antu.Constants.VALIDATION_STAGE_HEADER;
import static no.entur.antu.validation.ValidationProfile.TIMETABLE;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.cloud.pubsub.v1.Subscriber;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.support.converter.ConvertedBasicAcknowledgeablePubsubMessage;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import no.entur.antu.TestApp;
import no.entur.antu.config.TestConfig;
import org.entur.pubsub.base.EnturGooglePubSubAdmin;
import org.entur.pubsub.base.EnturGooglePubSubUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.rutebanken.helper.storage.repository.InMemoryBlobStoreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.gcloud.PubSubEmulatorContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Checks the transport, once: a validation request published by a client is picked up, and the status
 * antu publishes carries the attributes the client matches its pending job on.
 *
 * <p>The validation logic itself is covered by the pipeline tests, which need no emulator. This is the
 * only test that pays for one.
 */
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.NONE,
  classes = TestApp.class,
  properties = { "antu.pubsub.consumers.enabled=true" }
)
@ActiveProfiles({ "test", "default", "in-memory-blobstore" })
@Import(TestConfig.class)
// Closed rather than left in the context cache: the PubSub admin clients hold non-daemon gax threads that
// otherwise live until the JVM exits, which intermittently costs the build 30s waiting for the fork to die.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PubSubWiringTest {

  private static PubSubEmulatorContainer pubsubEmulator;

  @Autowired
  private PubSubTemplate pubSubTemplate;

  @Autowired
  private EnturGooglePubSubAdmin enturGooglePubSubAdmin;

  @Autowired
  private InMemoryBlobStoreRepository antuExchangeInMemoryBlobStoreRepository;

  @Value("${blobstore.gcs.antu.exchange.container.name}")
  private String antuExchangeContainerName;

  private Subscriber statusSubscriber;

  @AfterEach
  void closeStatusSubscriber() {
    if (statusSubscriber != null) {
      EnturGooglePubSubUtils.closeSubscriber(statusSubscriber);
      statusSubscriber = null;
    }
  }

  @BeforeAll
  static void startEmulator() {
    pubsubEmulator =
      new PubSubEmulatorContainer(
        DockerImageName.parse(
          "gcr.io/google.com/cloudsdktool/cloud-sdk:emulators"
        )
      );
    pubsubEmulator.start();
  }

  @AfterAll
  static void stopEmulator() {
    pubsubEmulator.stop();
  }

  @DynamicPropertySource
  static void emulatorProperties(DynamicPropertyRegistry registry) {
    registry.add(
      "spring.cloud.gcp.pubsub.emulator-host",
      pubsubEmulator::getEmulatorEndpoint
    );
  }

  @Test
  void aValidationRequestIsPickedUpAndAnsweredWithAStatus() {
    antuExchangeInMemoryBlobStoreRepository.setContainerName(
      antuExchangeContainerName
    );
    String datasetBlobName =
      BLOBSTORE_PATH_ANTU_EXCHANGE_INBOUND_RECEIVED +
      "flb/rb_flb-aggregated-netex.zip";
    InputStream dataset = getClass()
      .getResourceAsStream("/rb_flb-aggregated-netex.zip");
    assertNotNull(dataset);
    antuExchangeInMemoryBlobStoreRepository.uploadBlob(
      datasetBlobName,
      dataset
    );

    List<ConvertedBasicAcknowledgeablePubsubMessage<String>> statuses =
      new CopyOnWriteArrayList<>();
    enturGooglePubSubAdmin.createSubscriptionIfMissing(
      AntuQueues.NETEX_VALIDATION_STATUS_QUEUE
    );
    // Closed at the end: an open Subscriber keeps non-daemon gax threads alive, which intermittently
    // delays the JVM's exit past surefire's patience and adds 30s to the build.
    statusSubscriber =
      pubSubTemplate.subscribeAndConvert(
        AntuQueues.NETEX_VALIDATION_STATUS_QUEUE,
        message -> {
          statuses.add(message);
          message.ack();
        },
        String.class
      );

    pubSubTemplate.publish(
      AntuQueues.NETEX_VALIDATION_QUEUE,
      "",
      Map.of(
        DATASET_REFERENTIAL,
        "flb",
        VALIDATION_DATASET_FILE_HANDLE_HEADER,
        datasetBlobName,
        VALIDATION_CORRELATION_ID_HEADER,
        "correlation-from-marduk",
        VALIDATION_CLIENT_HEADER,
        VALIDATION_CLIENT_MARDUK,
        VALIDATION_STAGE_HEADER,
        "EnturValidationStagePreValidation",
        VALIDATION_PROFILE_HEADER,
        TIMETABLE.id()
      )
    );

    await()
      .atMost(Duration.ofMinutes(2))
      .pollInterval(Duration.ofSeconds(1))
      .until(() -> !statuses.isEmpty());

    var started = statuses.getFirst();
    assertEquals(STATUS_VALIDATION_STARTED, started.getPayload());

    Map<String, String> attributes = started
      .getPubsubMessage()
      .getAttributesMap();
    // The client matches its pending job on these, so they have to survive the round trip.
    assertEquals("flb", attributes.get(DATASET_REFERENTIAL));
    assertEquals(
      "correlation-from-marduk",
      attributes.get(VALIDATION_CORRELATION_ID_HEADER)
    );
    assertEquals(
      datasetBlobName,
      attributes.get(VALIDATION_DATASET_FILE_HANDLE_HEADER)
    );
    assertEquals(
      "EnturValidationStagePreValidation",
      attributes.get(VALIDATION_STAGE_HEADER)
    );
    assertEquals(
      VALIDATION_CLIENT_MARDUK,
      attributes.get(VALIDATION_CLIENT_HEADER)
    );
    // Derived by antu, and what the client uses to fetch the report afterwards.
    assertTrue(
      attributes.get(VALIDATION_REPORT_ID_HEADER).startsWith("flb_"),
      "unexpected report id " + attributes.get(VALIDATION_REPORT_ID_HEADER)
    );
  }
}
