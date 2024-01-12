package no.entur.antu.cache;

import java.util.stream.Collectors;
import org.redisson.api.RedissonClient;

/**
 * Redisson-based implementation of the CacheAdmin interface.
 */
public class RedissonCacheAdmin implements CacheAdmin {

  private final RedissonClient redissonClient;

  public RedissonCacheAdmin(RedissonClient redissonClient) {
    this.redissonClient = redissonClient;
  }

  @Override
  public void clear() {
    redissonClient.getKeys().flushdbParallelAsync();
  }

  @Override
  public String dumpKeys() {
    return redissonClient
      .getKeys()
      .getKeysStream()
      .collect(Collectors.joining("\n"));
  }
}
