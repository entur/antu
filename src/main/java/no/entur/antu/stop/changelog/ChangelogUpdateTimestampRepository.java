package no.entur.antu.stop.changelog;

import java.time.Instant;

/**
 * Repository storing the time of the last update applied to the stop repository.
 */
public interface ChangelogUpdateTimestampRepository {
  void setTimestamp(Instant publicationTime);

  Instant getTimestamp();
}
