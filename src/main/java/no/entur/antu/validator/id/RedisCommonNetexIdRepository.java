package no.entur.antu.validator.id;

import javax.cache.Cache;
import java.util.Collections;
import java.util.Set;


/**
 * Redis-based implementation of the common NeTEX ids repository.
 */
public class RedisCommonNetexIdRepository implements CommonNetexIdRepository {

    private static final String COMMON_NETEX_ID_SET_PREFIX = "ACCUMULATED_NETEX_ID_SET_";

    private final Cache<String, Set<String>> commonIdsCache;

    public RedisCommonNetexIdRepository(Cache<String, Set<String>> commonIdsCache) {
        this.commonIdsCache = commonIdsCache;
    }

    @Override
    public Set<String> getCommonNetexIds(String reportId) {
        // TODO temporary, should be initialized upstream in the route
        Set<String> commonIds = commonIdsCache.get(getCommonNetexIdsKey(reportId));
        if (commonIds != null) {
            return commonIds;
        } else {
            return Collections.emptySet();
        }
    }

    @Override
    public void addCommonNetexIds(String reportId, Set<String> commonIds) {
        commonIdsCache.get(getCommonNetexIdsKey(reportId)).addAll(commonIds);
    }


    private String getCommonNetexIdsKey(String reportId) {
        return COMMON_NETEX_ID_SET_PREFIX + reportId;
    }
}
