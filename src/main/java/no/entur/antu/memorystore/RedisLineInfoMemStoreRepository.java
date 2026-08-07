package no.entur.antu.memorystore;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.redisson.api.RLocalCachedMap;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.api.options.LocalCachedMapOptions;
import org.redisson.client.codec.StringCodec;
import org.redisson.codec.CompositeCodec;
import org.redisson.codec.JsonJacksonCodec;

/**
 * Redis-based implementation of the line information repository.
 * Uses distributed locks to ensure thread-safe updates across multiple pods.
 * The cache is created and managed internally by this repository.
 */
public class RedisLineInfoMemStoreRepository
  implements LineInfoMemStoreRepository {

  private static final String LINE_INFO_CACHE = "linesInfoCache";

  private final RedissonClient redissonClient;
  private final Map<String, List<String>> lineInfoCache;

  public RedisLineInfoMemStoreRepository(RedissonClient redissonClient) {
    this.redissonClient = redissonClient;
    this.lineInfoCache = createLineInfoCache(redissonClient);
  }

  private static RLocalCachedMap<String, List<String>> createLineInfoCache(
    RedissonClient redissonClient
  ) {
    LocalCachedMapOptions<String, List<String>> options = LocalCachedMapOptions
      .<String, List<String>>name(LINE_INFO_CACHE)
      .codec(new CompositeCodec(new StringCodec(), new JsonJacksonCodec()))
      .timeToLive(Duration.ofHours(1));
    return redissonClient.getLocalCachedMap(options);
  }

  @Override
  public void addLineInfo(String validationReportId, String lineInfoString) {
    RLock lock = redissonClient.getLock(validationReportId);
    try {
      lock.lock();

      lineInfoCache.merge(
        validationReportId,
        new ArrayList<>(List.of(lineInfoString)),
        (existingList, newList) -> {
          existingList.addAll(newList);
          return existingList;
        }
      );
    } finally {
      if (lock.isHeldByCurrentThread()) {
        lock.unlock();
      }
    }
  }

  @Override
  public List<String> getLineInfo(String validationReportId) {
    return lineInfoCache.get(validationReportId);
  }

  @Override
  public void removeLineInfo(String validationReportId) {
    lineInfoCache.remove(validationReportId);
  }
}
