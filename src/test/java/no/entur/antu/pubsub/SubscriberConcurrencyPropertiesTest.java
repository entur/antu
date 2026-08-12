package no.entur.antu.pubsub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.google.cloud.spring.pubsub.core.PubSubConfiguration;
import com.google.pubsub.v1.ProjectSubscriptionName;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

/**
 * How many validations a pod runs at once is set through Spring Cloud GCP's per-subscription properties,
 * and a misspelled key there is silently ignored rather than rejected — the pod would fall back to the
 * library default of four concurrent callbacks against a heap sized for one.
 *
 * <p>This binds the keys exactly as {@code helm/antu/templates/configmap.yaml} writes them, so a library
 * upgrade that moves them fails here instead of in production. Binding directly rather than booting a
 * context keeps it to the one thing worth checking: that the key path reaches the field.
 */
class SubscriberConcurrencyPropertiesTest {

  private static final String PROJECT = "test";

  private static PubSubConfiguration bind(Map<String, Object> properties) {
    PubSubConfiguration configuration = new Binder(
      new MapConfigurationPropertySource(properties)
    )
      .bindOrCreate("spring.cloud.gcp.pubsub", PubSubConfiguration.class);
    configuration.initialize(PROJECT);
    return configuration;
  }

  private static PubSubConfiguration.Subscriber subscription(
    PubSubConfiguration configuration,
    String subscription
  ) {
    return configuration.getSubscriptionProperties(
      ProjectSubscriptionName.of(PROJECT, subscription)
    );
  }

  @Test
  void theJobQueueSubscriptionGetsItsOwnExecutorAndFlowControl() {
    PubSubConfiguration configuration = bind(
      Map.of(
        "spring.cloud.gcp.pubsub.subscription.AntuJobQueue.executor-threads",
        "2",
        "spring.cloud.gcp.pubsub.subscription.AntuJobQueue.flow-control.max-outstanding-element-count",
        "2"
      )
    );

    PubSubConfiguration.Subscriber jobQueue = subscription(
      configuration,
      AntuQueues.JOB_QUEUE
    );
    assertEquals(2, jobQueue.getExecutorThreads());
    assertEquals(2L, jobQueue.getFlowControl().getMaxOutstandingElementCount());
  }

  /**
   * The two subscriptions are configured separately so that a validation running for minutes cannot stop
   * new validation requests from being accepted.
   */
  @Test
  void theRequestSubscriptionIsConfiguredSeparately() {
    PubSubConfiguration configuration = bind(
      Map.of(
        "spring.cloud.gcp.pubsub.subscription.AntuJobQueue.executor-threads",
        "4",
        "spring.cloud.gcp.pubsub.subscription.AntuNetexValidationQueue.executor-threads",
        "1"
      )
    );

    assertEquals(
      4,
      subscription(configuration, AntuQueues.JOB_QUEUE).getExecutorThreads()
    );
    assertEquals(
      1,
      subscription(configuration, AntuQueues.NETEX_VALIDATION_QUEUE)
        .getExecutorThreads()
    );
  }

  /**
   * The subscription name is a map key, and environment-variable relaxed binding lower-cases those. So the
   * keys below cannot be set with an env var such as
   * {@code SPRING_CLOUD_GCP_PUBSUB_SUBSCRIPTION_ANTUJOBQUEUE_EXECUTORTHREADS}: it binds
   * {@code ...subscription.antujobqueue...}, which never matches the real subscription and is silently
   * ignored. helm writes a properties file so it works there; compose and local-k8s have to use
   * SPRING_APPLICATION_JSON, which preserves case.
   */
  @Test
  void aLowerCasedSubscriptionNameDoesNotMatch() {
    PubSubConfiguration configuration = bind(
      Map.of(
        "spring.cloud.gcp.pubsub.subscription.antujobqueue.executor-threads",
        "2"
      )
    );

    assertNull(
      subscription(configuration, AntuQueues.JOB_QUEUE).getExecutorThreads(),
      "a lower-cased subscription key must not be treated as a match"
    );
  }

  /**
   * Left unset the properties carry no value at all, and the subscriber factory substitutes
   * {@link PubSubConfiguration#DEFAULT_EXECUTOR_THREADS}. Four callback threads per subscription is
   * therefore what a pod gets by default, which is the reason both keys above have to be set rather than
   * the Subscriber count alone.
   */
  @Test
  void unsetMeansTheLibraryDefaultOfFourThreads() {
    assertEquals(4, PubSubConfiguration.DEFAULT_EXECUTOR_THREADS);
    assertNull(
      subscription(bind(Map.of()), AntuQueues.JOB_QUEUE).getExecutorThreads()
    );
  }
}
