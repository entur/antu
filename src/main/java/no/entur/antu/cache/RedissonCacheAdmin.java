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
  public long deleteKeysByPattern(String pattern) {
    return redissonClient.getKeys().deleteByPattern(pattern);
  }

  @Override
  public String dumpKeys() {
    return redissonClient
      .getKeys()
      .getKeysStream()
      .map(key ->
        key +
        " (" +
        redissonClient.getKeys().getType(key).name() +
        ", TTL: " +
        formatTtl(redissonClient.getKeys().remainTimeToLive(key)) +
        ")"
      )
      .sorted()
      .collect(Collectors.joining("\n"));
  }

  private String formatTtl(long ttl) {
    if (ttl == -2) {
      return "DELETED";
    }
    if (ttl == -1) {
      return "NONE";
    }
    return ttl / 1000 + "s";
  }
}
