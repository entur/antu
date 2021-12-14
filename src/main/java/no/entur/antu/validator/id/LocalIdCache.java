package no.entur.antu.validator.id;

import javax.cache.Cache;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class LocalIdCache {

    private final Cache<String, Map<String, IdVersion>> cache;

    public LocalIdCache(Cache<String, Map<String, IdVersion>> cache) {
        this.cache = cache;
    }

    public void addAll(String reportId, String filename, Set<IdVersion> idVersions) {
        Map<String, IdVersion> collect = idVersions.stream().collect(Collectors.toMap(IdVersion::getId, Function.identity()));
        cache.put(reportId + '_' + filename, collect);
    }

    public Map<String, IdVersion> get(String reportId, String filename) {
        return cache.get(reportId + '_' + filename);
    }
}
