package no.entur.antu.validator.id;

import no.entur.antu.exception.FileAlreadyValidatedException;
import org.entur.netex.validation.validator.id.IdVersion;
import org.entur.netex.validation.validator.id.NetexIdRepository;
import org.redisson.api.RLocalCachedMap;
import org.redisson.api.RLock;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


/**
 * Redis-based implementation of the NeTEX ids repository.
 * Duplicate check is performed remotely on the Redis server and protected by a lock to prevent concurrent access on the accumulated NeTEX ids set.
 */
public class RedisNetexIdRepository implements NetexIdRepository {

    private static final String NETEX_LOCAL_ID_SET_PREFIX = "NETEX_LOCAL_ID_SET_";
    private static final String COMMON_NETEX_ID_SET_PREFIX = "COMMON_NETEX_ID_SET_";


    private static final String ACCUMULATED_NETEX_ID_LOCK_PREFIX = "ACCUMULATED_NETEX_ID_LOCK_";
    private static final String ACCUMULATED_NETEX_ID_SET_PREFIX = "ACCUMULATED_NETEX_ID_SET_";

    private final RedissonClient redissonClient;

    private final RLocalCachedMap<String, Set<String>> commonIdsCache;

    public RedisNetexIdRepository(RedissonClient redissonClient, RLocalCachedMap<String, Set<String>> commonIdsCache) {
        this.redissonClient = redissonClient;
        this.commonIdsCache = commonIdsCache;
    }

    @Override
    public Set<String> getDuplicateNetexIds(String reportId, String filename, Set<String> localIds) {
        String netexLocalIdsKey = getNetexLocalIdsKey(reportId, filename);
        String accumulatedNetexIdsKey = getAccumulatedNetexIdsKey(reportId);
        RSet<String> localNetexIds = redissonClient.getSet(netexLocalIdsKey);
        if (!localNetexIds.isEmpty()) {
            // protect against multiple run due to retry logic
            throw new FileAlreadyValidatedException("Validation already run for file " + filename + " in report " + reportId);
        }
        localNetexIds.expire(1, TimeUnit.HOURS);
        localNetexIds.addAll(localIds);
        RSet<String> accumulatedNetexIds = redissonClient.getSet(accumulatedNetexIdsKey);
        accumulatedNetexIds.expire(1, TimeUnit.HOURS);
        RLock lock = redissonClient.getLock(getAccumulatedNetexIdsLockKey(reportId));
        try {
            lock.lock();
            Set<String> duplicates = localNetexIds.readIntersection(accumulatedNetexIdsKey);
            accumulatedNetexIds.union(netexLocalIdsKey);
            return duplicates;
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
            return commonIds;
        } else {
            return Collections.emptySet();
        }
    }

    @Override
    public void addSharedNetexIds(String reportId, Set<IdVersion> commonIdVersions) {
        Set<String> commonIds = commonIdVersions.stream().map(IdVersion::getId).collect(Collectors.toSet());
        String cacheKey = getCommonNetexIdsKey(reportId);
        commonIdsCache.put(cacheKey, commonIds);
    }

    @Override
    public void cleanUp(String reportId) {
        commonIdsCache.fastRemove(getCommonNetexIdsKey(reportId));
        redissonClient.getKeys().delete(getAccumulatedNetexIdsKey(reportId));
        redissonClient.getKeys().delete(getAccumulatedNetexIdsLockKey(reportId));
        redissonClient.getKeys().deleteByPattern(NETEX_LOCAL_ID_SET_PREFIX + reportId + '*');
    }


    private String getNetexLocalIdsKey(String reportId, String filename) {
        return NETEX_LOCAL_ID_SET_PREFIX + reportId + '_' + filename;
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
}
