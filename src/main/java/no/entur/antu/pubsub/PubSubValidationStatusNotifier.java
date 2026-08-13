package no.entur.antu.pubsub;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import no.entur.antu.job.ValidationContext;
import no.entur.antu.job.ValidationStatus;
import no.entur.antu.job.ValidationStatusNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PubSubValidationStatusNotifier
  implements ValidationStatusNotifier {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    PubSubValidationStatusNotifier.class
  );

  private final PubSubTemplate pubSubTemplate;

  public PubSubValidationStatusNotifier(PubSubTemplate pubSubTemplate) {
    this.pubSubTemplate = pubSubTemplate;
  }

  @Override
  public void notifyStatus(ValidationContext context, ValidationStatus status) {
    LOGGER.info("Notifying status {}", status.wireValue());
    PubSubPublishing.publishAndWait(
      pubSubTemplate,
      AntuQueues.NETEX_VALIDATION_STATUS_QUEUE,
      status.wireValue(),
      JobMessageCodec.statusAttributes(context)
    );
  }
}
