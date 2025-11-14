package no.entur.antu.memorystore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

/**
 * Redis-based implementation of the line information repository.
 * Uses distributed locks to ensure thread-safe updates across multiple pods.
 */
public class RedisLineInfoMemStoreRepository
  implements LineInfoMemStoreRepository {

  private final RedissonClient redissonClient;
  private final Map<String, List<String>> lineInfoCache;

  public RedisLineInfoMemStoreRepository(
    RedissonClient redissonClient,
    Map<String, List<String>> lineInfoCache
  ) {
    this.redissonClient = redissonClient;
    this.lineInfoCache = lineInfoCache;
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
}
