package no.entur.antu.stop.changelog;

import java.time.Duration;
import java.time.Instant;
import org.rutebanken.helper.stopplace.changelog.kafka.BasePublicationTimeRecordFilterStrategy;

/**
 * Filter changelog events by publication time.
 * Since the stop registry is periodically rebuilt from a NeTEx dataset, this implementation gives the possibility
 * to update the cut-off date when a newer dataset has been imported.
 */
public class AntuPublicationTimeRecordFilterStrategy
  extends BasePublicationTimeRecordFilterStrategy {

  private volatile Instant publicationTime = Instant
    .now()
    .minus(Duration.ofDays(1));

  @Override
  protected Instant publicationTime() {
    return publicationTime;
  }

  public void setPublicationTime(Instant publicationTime) {
    this.publicationTime = publicationTime;
  }
}
