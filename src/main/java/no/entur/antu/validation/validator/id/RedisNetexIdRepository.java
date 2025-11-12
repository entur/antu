package no.entur.antu.validation.validator.id;

import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.entur.netex.validation.validator.id.IdVersion;
import org.entur.netex.validation.validator.id.NetexIdRepository;
import org.redisson.api.RLocalCachedMap;
import org.redisson.api.RLock;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Redis-based implementation of the NeTEX ids repository.
 * Duplicate check is performed remotely on the Redis server and protected by a lock to prevent concurrent access on the accumulated NeTEX ids set.
 */
public class RedisNetexIdRepository implements NetexIdRepository {

  private static final String NETEX_LOCAL_ID_SET_PREFIX = "NETEX_LOCAL_ID_SET_";
  private static final String DUPLICATED_ID_SET_PREFIX = "DUPLICATED_ID_SET_";

  private static final String COMMON_NETEX_ID_SET_PREFIX =
    "COMMON_NETEX_ID_SET_";
  private static final String COMMON_NETEX_ID_LOCK_PREFIX =
    "COMMON_NETEX_LOCK_SET_";

  private static final String ACCUMULATED_NETEX_ID_LOCK_PREFIX =
    "ACCUMULATED_NETEX_ID_LOCK_";
  private static final String ACCUMULATED_NETEX_ID_SET_PREFIX =
    "ACCUMULATED_NETEX_ID_SET_";

  private static final Logger LOGGER = LoggerFactory.getLogger(
    RedisNetexIdRepository.class
  );

  private final RedissonClient redissonClient;

  private final RLocalCachedMap<String, Set<String>> commonIdsCache;

  public RedisNetexIdRepository(
    RedissonClient redissonClient,
    RLocalCachedMap<String, Set<String>> commonIdsCache
  ) {
    this.redissonClient = redissonClient;
    this.commonIdsCache = commonIdsCache;
  }

  @Override
  public Set<String> getDuplicateNetexIds(
    String reportId,
    String filename,
    Set<String> localIds
  ) {
    String netexLocalIdsKey = getNetexLocalIdsKey(reportId, filename);
    String accumulatedNetexIdsKey = getAccumulatedNetexIdsKey(reportId);
    String duplicatedNetexIdsKey = getDuplicatedNetexIdsKey(reportId, filename);

    RLock lock = redissonClient.getLock(
      getAccumulatedNetexIdsLockKey(reportId)
    );
    try {
      lock.lock();

      // in order to make the operation idempotent in case of multiple deliveries of the same PubSub message,
      // the duplicated ids that were calculated in a previous invocation are retrieved and reused.
      RSet<String> localNetexIds = redissonClient.getSet(netexLocalIdsKey);
      RSet<String> duplicatedIds = redissonClient.getSet(duplicatedNetexIdsKey);
      if (localNetexIds.isExists()) {
        LOGGER.info(
          "Validation already run for file {} in report {}. Ignoring",
          filename,
          reportId
        );
        return new HashSet<>(duplicatedIds);
      }
      duplicatedIds.expire(Duration.ofHours(1));
      localNetexIds.expire(Duration.ofHours(1));
      localNetexIds.addAll(localIds);

      RSet<String> accumulatedNetexIds = redissonClient.getSet(
        accumulatedNetexIdsKey
      );
      accumulatedNetexIds.expire(Duration.ofHours(1));

      Set<String> intersection = localNetexIds.readIntersection(
        accumulatedNetexIdsKey
      );
      duplicatedIds.addAll(intersection);
      accumulatedNetexIds.addAll(localNetexIds);
      return intersection;
    } finally {
      if (lock.isHeldByCurrentThread()) {
        lock.unlock();
      }
    }
  }

  @Override
  public Set<String> getSharedNetexIds(String reportId) {
    Set<String> commonIds = commonIdsCache.get(getCommonNetexIdsKey(reportId));
    if (commonIds != null) {
      LOGGER.debug(
        "Found {} shared ids for reportId {}",
        commonIds.size(),
        reportId
      );
      return commonIds;
    } else {
      LOGGER.debug("No shared ids found for reportId {}", reportId);
      return Collections.emptySet();
    }
  }

  @Override
  public void addSharedNetexIds(
    String reportId,
    Set<IdVersion> commonIdVersions
  ) {
    Set<String> commonIds = commonIdVersions
      .stream()
      .map(IdVersion::getId)
      .collect(Collectors.toSet());
    String cacheKey = getCommonNetexIdsKey(reportId);
    RLock lock = redissonClient.getLock(getCommonNetexIdsLockKey(reportId));
    try {
      lock.lock();
      Set<String> existingCommonIds = commonIdsCache.get(cacheKey);
      if (existingCommonIds == null) {
        existingCommonIds = new HashSet<>();
      }
      existingCommonIds.addAll(commonIds);
      commonIdsCache.put(cacheKey, existingCommonIds);
    } finally {
      if (lock.isHeldByCurrentThread()) {
        lock.unlock();
      }
    }
  }

  @Override
  public void cleanUp(String reportId) {
    commonIdsCache.fastRemove(getCommonNetexIdsKey(reportId));
    redissonClient.getKeys().delete(getCommonNetexIdsLockKey(reportId));
    redissonClient.getKeys().delete(getAccumulatedNetexIdsKey(reportId));
    redissonClient.getKeys().delete(getAccumulatedNetexIdsLockKey(reportId));
    redissonClient
      .getKeys()
      .deleteByPattern(NETEX_LOCAL_ID_SET_PREFIX + reportId + '*');
    redissonClient
      .getKeys()
      .deleteByPattern(DUPLICATED_ID_SET_PREFIX + reportId + '*');
  }

  private String getNetexLocalIdsKey(String reportId, String filename) {
    return NETEX_LOCAL_ID_SET_PREFIX + reportId + '_' + filename;
  }

  private String getDuplicatedNetexIdsKey(String reportId, String filename) {
    return DUPLICATED_ID_SET_PREFIX + reportId + '_' + filename;
  }

  private String getAccumulatedNetexIdsKey(String reportId) {
    return ACCUMULATED_NETEX_ID_SET_PREFIX + reportId;
  }

  private String getAccumulatedNetexIdsLockKey(String reportId) {
    return ACCUMULATED_NETEX_ID_LOCK_PREFIX + reportId;
  }

  private String getCommonNetexIdsKey(String reportId) {
    return COMMON_NETEX_ID_SET_PREFIX + reportId;
  }

  private String getCommonNetexIdsLockKey(String reportId) {
    return COMMON_NETEX_ID_LOCK_PREFIX + reportId;
  }
}
