package no.entur.antu.actuator;

import java.util.HashMap;
import java.util.Map;
import no.entur.antu.stop.changelog.ChangelogUpdateTimestampRepository;
import org.rutebanken.helper.stopplace.changelog.kafka.ChangelogConsumerController;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Provide information related to the stop place change log in the /info actuator endpoint.
 */
@Profile("stop-place-changelog")
@Component
public class StopPlaceChangelogInfoContributor implements InfoContributor {

  private final ChangelogConsumerController changelogConsumerController;
  private final ChangelogUpdateTimestampRepository changelogUpdateTimestampRepository;

  public StopPlaceChangelogInfoContributor(
    ChangelogConsumerController changelogConsumerController,
    ChangelogUpdateTimestampRepository changelogUpdateTimestampRepository
  ) {
    this.changelogConsumerController = changelogConsumerController;
    this.changelogUpdateTimestampRepository =
      changelogUpdateTimestampRepository;
  }

  @Override
  public void contribute(Info.Builder builder) {
    Map<String, Object> details = new HashMap<>();
    details.put(
      "kafkaListenerRunning",
      changelogConsumerController.isRunning()
    );
    details.put(
      "lastPublicationTimestamp",
      changelogUpdateTimestampRepository.getTimestamp()
    );
    builder.withDetail("stopPlaceChangeLog", details);
  }
}
