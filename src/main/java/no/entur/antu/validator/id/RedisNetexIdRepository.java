package no.entur.antu.validator.id;

import no.entur.antu.exception.AntuException;
import org.redisson.api.RLock;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;

import java.util.Set;
import java.util.concurrent.TimeUnit;


/**
 * Redis-based implementation of the NeTEX ids repository.
 * Duplicate check is performed remotely on the Redis server and protected by a lock to prevent concurrent access on the accumulated NeTEX ids set.
 */
public class RedisNetexIdRepository implements NetexIdRepository {

    private static final String NETEX_LOCAL_ID_SET_PREFIX = "NETEX_LOCAL_ID_SET_";

    private static final String ACCUMULATED_NETEX_ID_LOCK_PREFIX = "ACCUMULATED_NETEX_ID_LOCK_";
    private static final String ACCUMULATED_NETEX_ID_SET_PREFIX = "ACCUMULATED_NETEX_ID_SET_";

    private final RedissonClient redissonClient;

    public RedisNetexIdRepository(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public Set<String> getDuplicates(String reportId, String filename, Set<String> localIds) {
        String netexLocalIdsKey = getNetexLocalIdsKey(reportId, filename);
        String accumulatedNetexIdsKey = getAccumulatedNetexIdsKey(reportId);
        RSet<String> localNetexIds = redissonClient.getSet(netexLocalIdsKey);
        if (!localNetexIds.isEmpty()) {
            // protect against multiple run due to retry logic
            throw new AntuException("Duplicate check already run for file " + filename);
        }
        localNetexIds.expire(1, TimeUnit.HOURS);
        localNetexIds.addAll(localIds);
        RSet<String> accumulatedNetexIds = redissonClient.getSet(accumulatedNetexIdsKey);
        accumulatedNetexIds.expire(1, TimeUnit.HOURS);
        RLock lock = redissonClient.getLock(ACCUMULATED_NETEX_ID_LOCK_PREFIX);
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

    private String getNetexLocalIdsKey(String reportId, String filename) {
        return NETEX_LOCAL_ID_SET_PREFIX + reportId + '_' + filename;
    }

    private String getAccumulatedNetexIdsKey(String reportId) {
        return ACCUMULATED_NETEX_ID_SET_PREFIX + reportId;
    }
}