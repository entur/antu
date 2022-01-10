package no.entur.antu.validator.id;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import javax.cache.Cache;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Redis-based implementation of the common NeTEX ids repository.
 */
public class RedisCommonNetexIdRepository implements CommonNetexIdRepository {

    private static final String COMMON_NETEX_ID_LOCK_PREFIX = "COMMON_NETEX_ID_LOCK_";
    private static final String COMMON_NETEX_ID_SET_PREFIX = "COMMON_NETEX_ID_SET_";

    private final RedissonClient redissonClient;
    private final Cache<String, Set<String>> commonIdsCache;

    public RedisCommonNetexIdRepository(RedissonClient redissonClient, Cache<String, Set<String>> commonIdsCache) {
        this.redissonClient = redissonClient;
        this.commonIdsCache = commonIdsCache;
    }

    @Override
    public Set<String> getCommonNetexIds(String reportId) {
        Set<String> commonIds = commonIdsCache.get(getCommonNetexIdsKey(reportId));
        if (commonIds != null) {
            return commonIds;
        } else {
            return Collections.emptySet();
        }
    }

    @Override
    public void addCommonNetexIds(String reportId, Set<IdVersion> commonIdVersions) {
        Set<String> commonIds = commonIdVersions.stream().map(IdVersion::getId).collect(Collectors.toSet());
        RLock lock = redissonClient.getLock(COMMON_NETEX_ID_LOCK_PREFIX + reportId);
        String cacheKey = getCommonNetexIdsKey(reportId);
        try {
            lock.lock();
            Set<String> commonsIdsInCache = commonIdsCache.get(cacheKey);
            if (commonsIdsInCache == null) {
                commonIdsCache.put(cacheKey, commonIds);
            } else {
                commonsIdsInCache.addAll(commonIds);
                commonIdsCache.put(cacheKey, commonsIdsInCache);
            }
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }


    }


    private String getCommonNetexIdsKey(String reportId) {
        return COMMON_NETEX_ID_SET_PREFIX + reportId;
    }
}
