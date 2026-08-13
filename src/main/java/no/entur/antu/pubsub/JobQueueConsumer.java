package no.entur.antu.pubsub;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import no.entur.antu.job.JobDispatcher;
import no.entur.antu.job.ValidationMdc;
import org.entur.pubsub.base.AbstractEnturGooglePubSubConsumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Consumes antu's internal work queue.
 *
 * <p>The subscription is served by the PubSub streaming pull client, which extends the ack deadline
 * on its own for as long as a message is being processed, up to an hour. Validating a large file can
 * take minutes, and this is what keeps it from being redelivered while it is still running.
 */
@Component
@ConditionalOnProperty(
  value = "antu.pubsub.consumers.enabled",
  matchIfMissing = true
)
public class JobQueueConsumer extends AbstractEnturGooglePubSubConsumer {

  private final JobDispatcher jobDispatcher;
  private final InFlightMessages inFlightMessages;
  private final int concurrentConsumers;

  public JobQueueConsumer(
    JobDispatcher jobDispatcher,
    InFlightMessages inFlightMessages,
    @Value("${antu.netex.job.consumers:1}") int concurrentConsumers
  ) {
    this.jobDispatcher = jobDispatcher;
    this.inFlightMessages = inFlightMessages;
    this.concurrentConsumers = concurrentConsumers;
  }

  @Override
  protected String getDestinationName() {
    return AntuQueues.JOB_QUEUE;
  }

  @Override
  protected int getConcurrentConsumers() {
    return concurrentConsumers;
  }

  @Override
  public void onMessage(byte[] content, Map<String, String> attributes) {
    // Cleared on the way in, not on the way out: the consumer base class logs a failed message after
    // onMessage returns, and that ERROR line is the one worth having the validation's identity on.
    ValidationMdc.clear();
    inFlightMessages.track(() ->
      jobDispatcher.dispatch(
        JobMessageCodec.decode(
          attributes,
          new String(content, StandardCharsets.UTF_8)
        )
      )
    );
  }
}
