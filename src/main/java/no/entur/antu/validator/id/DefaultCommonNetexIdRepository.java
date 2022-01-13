package no.entur.antu.validator.id;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


/**
 * Hashmap-based implementation of the common NeTEX ids repository.
 */
public class DefaultCommonNetexIdRepository implements CommonNetexIdRepository {

    private final Map<String, Set<String>> commonIdsCache;

    public DefaultCommonNetexIdRepository() {
        this.commonIdsCache = new ConcurrentHashMap<>();
    }

    @Override
    public Set<String> getCommonNetexIds(String reportId) {
        Set<String> commonIds = commonIdsCache.get(reportId);
        return Objects.requireNonNullElse(commonIds, Collections.emptySet());
    }

    @Override
    public synchronized void addCommonNetexIds(String reportId, Set<IdVersion> commonIdVersions) {
        Set<String> commonIds = commonIdVersions.stream().map(IdVersion::getId).collect(Collectors.toSet());
        commonIdsCache.computeIfPresent(reportId, (key, value) -> {
            value.addAll(commonIds);
            return value;
        });
        commonIdsCache.computeIfAbsent(reportId, key -> commonIds);
    }

    @Override
    public void cleanUp(String reportId) {
        commonIdsCache.remove(reportId);
    }
}
