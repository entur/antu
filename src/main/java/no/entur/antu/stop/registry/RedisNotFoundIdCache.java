package no.entur.antu.stop.registry;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.redisson.api.RSetCache;

public class RedisNotFoundIdCache implements NotFoundIdCache {

  private final RSetCache<String> notFoundIds;
  private final Duration timeToLive;

  public RedisNotFoundIdCache(
    RSetCache<String> notFoundIds,
    Duration timeToLive
  ) {
    this.notFoundIds = notFoundIds;
    this.timeToLive = timeToLive;
  }

  @Override
  public boolean contains(String id) {
    return notFoundIds.contains(id);
  }

  @Override
  public void remember(String id) {
    notFoundIds.add(id, timeToLive.toSeconds(), TimeUnit.SECONDS);
  }
}
