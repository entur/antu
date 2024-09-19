package no.entur.antu.cache;

import java.util.stream.Collectors;
import org.redisson.api.RType;
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
        formatKeyDetails(key) +
        ", TTL: " +
        formatTtl(redissonClient.getKeys().remainTimeToLive(key)) +
        ")"
      )
      .sorted()
      .collect(Collectors.joining("\n"));
  }

  private String formatKeyDetails(String key) {
    RType type = redissonClient.getKeys().getType(key);
    StringBuilder details = new StringBuilder();
    details.append(type.name());
    if (type == RType.MAP) {
      details.append("[");
      details.append(redissonClient.getMap(key).size());
      details.append("]");
    }
    if (type == RType.SET) {
      details.append("[");
      details.append(redissonClient.getSet(key).size());
      details.append("]");
    }
    return details.toString();
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
