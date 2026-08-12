package no.entur.antu.pubsub;

import java.util.Map;
import no.entur.antu.job.ValidationMdc;
import no.entur.antu.pipeline.ValidationInitializer;
import org.entur.pubsub.base.AbstractEnturGooglePubSubConsumer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Consumes validation requests from marduk and kakka. The request carries no body, everything it says
 * is in the message attributes.
 */
@Component
@ConditionalOnProperty(
  value = "antu.pubsub.consumers.enabled",
  matchIfMissing = true
)
public class ValidationRequestConsumer
  extends AbstractEnturGooglePubSubConsumer {

  private final ValidationInitializer validationInitializer;
  private final InFlightMessages inFlightMessages;

  public ValidationRequestConsumer(
    ValidationInitializer validationInitializer,
    InFlightMessages inFlightMessages
  ) {
    this.validationInitializer = validationInitializer;
    this.inFlightMessages = inFlightMessages;
  }

  @Override
  protected String getDestinationName() {
    return AntuQueues.NETEX_VALIDATION_QUEUE;
  }

  @Override
  public void onMessage(byte[] content, Map<String, String> attributes) {
    ValidationMdc.clear();
    inFlightMessages.track(() ->
      validationInitializer.initValidation(
        JobMessageCodec.readContext(attributes)
      )
    );
  }
}
