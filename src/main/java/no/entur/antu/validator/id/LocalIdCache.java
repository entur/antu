package no.entur.antu.validator.id;

import no.entur.antu.exception.AntuException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.cache.Cache;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Cache for NeTEx local IDs to check uniqueness across lines.
 */
public class LocalIdCache {

    /**
     * Set of NeTEx elements for which id-uniqueness across lines is not verified.
     * These IDs need not be cached.
     */
    private static final HashSet<String> IGNORABLE_ELEMENTS = new HashSet<>(Arrays.asList("ResourceFrame", "SiteFrame", "CompositeFrame", "TimetableFrame", "ServiceFrame", "ServiceCalendarFrame", "VehicleScheduleFrame", "Block", "RoutePoint", "PointProjection", "ScheduledStopPoint", "PassengerStopAssignment", "NoticeAssignment"));

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalIdCache.class);


    private final Cache<String, Map<String, IdVersion>> cache;

    public LocalIdCache(Cache<String, Map<String, IdVersion>> cache) {
        this.cache = cache;
    }

    public void addAll(String reportId, String filename, Set<IdVersion> idVersions) {
        String cacheKey = getKey(reportId, filename);
        final Map<String, IdVersion> cacheableIds;
        if (idVersions == null) {
            // no ids were stored if the XMLSchema validation failed
            LOGGER.debug("No ids found for cache entry {}", cacheKey);
            cacheableIds = Collections.emptyMap();
        } else {
            cacheableIds = idVersions.stream().filter(idVersion -> !IGNORABLE_ELEMENTS.contains(idVersion.getElementName())).collect(Collectors.toMap(IdVersion::getId, Function.identity()));
        }
        cache.put(cacheKey, cacheableIds);
        LOGGER.debug("Added local ids cache entry {}", cacheKey);
    }

    public Map<String, IdVersion> get(String reportId, String filename) {
        String cacheKey = getKey(reportId, filename);
        LOGGER.debug("Retrieving local ids cache entry {}", cacheKey);
        Map<String, IdVersion> localIds = cache.get(cacheKey);
        if (localIds == null) {
            throw new AntuException("Cache entry not found: " + cacheKey);
        }
        return localIds;
    }

    private String getKey(String reportId, String filename) {
        return reportId + '_' + filename;
    }
}
