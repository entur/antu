package no.entur.antu.validator.id;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Hashmap-based implementation of the NeTEX ids repository.
 */
public class DefaultNetexIdRepository implements NetexIdRepository {

    private final Map<String, Set<String>> accumulatedNetexIdsMap;
    
    public DefaultNetexIdRepository() {
        this.accumulatedNetexIdsMap = new ConcurrentHashMap<>();
    }

    @Override
    public synchronized Set<String> getDuplicates(String reportId, String filename, Set<String> localIds) {
        Set<String> accumulatedNetexIds = accumulatedNetexIdsMap.computeIfAbsent(reportId, s -> new HashSet<>());
        Set<String> duplicates = new HashSet<>(localIds);
        duplicates.retainAll(accumulatedNetexIds);
        accumulatedNetexIds.addAll(localIds);
        return duplicates;
    }

    @Override
    public void cleanUp(String reportId) {
        accumulatedNetexIdsMap.remove(reportId);
    }
}
