package no.entur.antu.pubsub;

import org.entur.pubsub.base.EnturGooglePubSubAdmin;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Creates the destinations antu only publishes to. The consumers create the ones they subscribe to,
 * which covers AntuJobQueue but not the status queue, so without this an emulator that was never
 * initialised answers the first status notification with NOT_FOUND.
 *
 * <p>A no-op wherever entur.pubsub.subscriber.autocreate is false, which is every deployed
 * environment. Gated on the consumer flag because that is the one saying this instance talks to
 * PubSub: the tests point at an unreachable emulator, where an admin call would stall the context.
 */
@Component
@ConditionalOnProperty(
  value = "antu.pubsub.consumers.enabled",
  matchIfMissing = true
)
class PubSubPublishTargets {

  private final EnturGooglePubSubAdmin enturGooglePubSubAdmin;

  PubSubPublishTargets(EnturGooglePubSubAdmin enturGooglePubSubAdmin) {
    this.enturGooglePubSubAdmin = enturGooglePubSubAdmin;
  }

  @EventListener
  void handleContextRefreshed(ContextRefreshedEvent contextRefreshedEvent) {
    enturGooglePubSubAdmin.createSubscriptionIfMissing(
      AntuQueues.NETEX_VALIDATION_STATUS_QUEUE
    );
  }
}
