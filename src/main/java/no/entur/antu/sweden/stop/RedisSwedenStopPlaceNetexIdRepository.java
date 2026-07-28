package no.entur.antu.sweden.stop;

import static no.entur.antu.config.cache.CacheConfig.VALIDATION_DATA_TTL;

import java.time.Duration;
import java.util.Set;
import no.entur.antu.exception.AntuException;
import no.entur.antu.exception.RetryableAntuException;
import org.redisson.api.RSemaphore;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;

/**
 * Redis-based implementation of the StopPlace and Quay ID repository for Swedish datasets.
 * Since multiple common files can reference the stop and quay ids, the file containing the SiteFrame
 * must be parsed before other common files can be validated.
 * This implementation uses semaphores to ensure that validation is blocked until the SiteFrame data is
 * loaded into the repository.
 */
public class RedisSwedenStopPlaceNetexIdRepository
  implements SwedenStopPlaceNetexIdRepository {

  private static final String SHARED_STOP_PLACE_ID_SET_PREFIX =
    "SHARED_STOP_PLACE_ID_SET_";
  public static final String STOP_PLACE_ID_SET_SEMAPHORE_PREFIX =
    "SWEDEN_STOP_PLACE_SET_SEMAPHORE_";

  private final RedissonClient redissonClient;

  public RedisSwedenStopPlaceNetexIdRepository(RedissonClient redissonClient) {
    this.redissonClient = redissonClient;
  }

  @Override
  public Set<String> getSharedStopPlaceAndQuayIds(String reportId) {
    RSemaphore semaphore = redissonClient.getSemaphore(
      getStopPlaceIdCacheSemaphoreKey(reportId)
    );
    semaphore.trySetPermits(1, VALIDATION_DATA_TTL);
    boolean cacheEntryAvailable;
    try {
      cacheEntryAvailable = semaphore.tryAcquire(Duration.ofMinutes(1));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RetryableAntuException(e);
    }
    if (cacheEntryAvailable) {
      return redissonClient.getSet(getStopPlaceIdCacheKey(reportId));
    } else {
      throw new AntuException("Unable to retrieve shared stop place ids");
    }
  }

  @Override
  public void addSharedStopPlaceAndQuayIds(
    String reportId,
    Set<String> stopPlaceAndQuayIds
  ) {
    RSet<String> stopPlaceIds = redissonClient.getSet(
      getStopPlaceIdCacheKey(reportId)
    );
    stopPlaceIds.addAll(stopPlaceAndQuayIds);
    stopPlaceIds.expireIfNotSet(VALIDATION_DATA_TTL);
    RSemaphore semaphore = redissonClient.getSemaphore(
      getStopPlaceIdCacheSemaphoreKey(reportId)
    );
    semaphore.addPermits(1);
    semaphore.expireIfNotSet(VALIDATION_DATA_TTL);
  }

  @Override
  public void cleanUp(String reportId) {
    redissonClient.getKeys().delete(getStopPlaceIdCacheKey(reportId));
    redissonClient.getKeys().delete(getStopPlaceIdCacheSemaphoreKey(reportId));
  }

  private String getStopPlaceIdCacheSemaphoreKey(String reportId) {
    return STOP_PLACE_ID_SET_SEMAPHORE_PREFIX + reportId;
  }

  private String getStopPlaceIdCacheKey(String reportId) {
    return SHARED_STOP_PLACE_ID_SET_PREFIX + reportId;
  }
}
