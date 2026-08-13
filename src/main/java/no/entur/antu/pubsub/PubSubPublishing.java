package no.entur.antu.pubsub;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import no.entur.antu.exception.RetryableAntuException;

/**
 * Publishes and waits for the broker to confirm.
 *
 * <p>Every publish in the pipeline hands work over to a later step, so a silent failure would
 * abandon the validation. Blocking on the future turns that into an exception, which nacks the
 * message being processed and lets PubSub redeliver it.
 */
final class PubSubPublishing {

  private PubSubPublishing() {}

  static void publishAndWait(
    PubSubTemplate pubSubTemplate,
    String topic,
    String body,
    Map<String, String> attributes
  ) {
    try {
      pubSubTemplate.publish(topic, body, attributes).get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RetryableAntuException(
        "Interrupted while publishing to " + topic,
        e
      );
    } catch (ExecutionException e) {
      throw new RetryableAntuException("Failed to publish to " + topic, e);
    }
  }
}
