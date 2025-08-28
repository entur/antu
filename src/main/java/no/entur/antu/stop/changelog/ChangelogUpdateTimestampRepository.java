package no.entur.antu.stop.changelog;

import java.time.Instant;
import org.redisson.api.RedissonClient;

/**
 * Repository storing the time of the last update applied to the stop repository.
 * Since the instance that applies updates to the stop repository may change over time,
 * this information has to be stored in a distributed cache.
 */
public class ChangelogUpdateTimestampRepository {

  private static final String CHANGELOG_UPDATE_TIMESTAMP =
    "changelogUpdateTimestamp";
  private final RedissonClient redissonClient;

  public ChangelogUpdateTimestampRepository(RedissonClient redissonClient) {
    this.redissonClient = redissonClient;
  }

  public void setTimestamp(Instant publicationTime) {
    redissonClient
      .<Instant>getBucket(CHANGELOG_UPDATE_TIMESTAMP)
      .set(publicationTime);
  }

  public Instant getTimestamp() {
    return redissonClient.<Instant>getBucket(CHANGELOG_UPDATE_TIMESTAMP).get();
  }
}
