package no.entur.antu.validator.id;

import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Repository for NeTEx IDs to check uniqueness across lines.
 */
public class NetexIdRepository {

    /**
     * Set of NeTEx elements for which id-uniqueness across lines is not verified.
     * These IDs need not be stored.
     */
    private static final HashSet<String> IGNORABLE_ELEMENTS = new HashSet<>(Arrays.asList("ResourceFrame", "SiteFrame", "CompositeFrame", "TimetableFrame", "ServiceFrame", "ServiceCalendarFrame", "VehicleScheduleFrame", "Block", "RoutePoint", "PointProjection", "ScheduledStopPoint", "PassengerStopAssignment", "NoticeAssignment"));

    private static final Logger LOGGER = LoggerFactory.getLogger(NetexIdRepository.class);
    private final RedissonClient redissonClient;


    public NetexIdRepository(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public void addAll(String reportId, String filename, Set<IdVersion> idVersions) {
        String entryKey = getKey(reportId, filename);
        final Map<String, IdVersion> netexIds;
        if (idVersions == null) {
            // no ids were stored if the XMLSchema validation failed
            LOGGER.debug("No ids added for entry {}", entryKey);
            netexIds = Collections.emptyMap();
        } else {
            netexIds = idVersions.stream().filter(idVersion -> !IGNORABLE_ELEMENTS.contains(idVersion.getElementName())).collect(Collectors.toMap(IdVersion::getId, Function.identity()));
        }
        RBucket<Map<String, IdVersion>> bucket = redissonClient.getBucket(entryKey);
        bucket.expire(1, TimeUnit.HOURS);
        bucket.set(netexIds);
        LOGGER.debug("Added ids for entry {}", entryKey);
    }

    public Map<String, IdVersion> get(String reportId, String filename) {
        String entryKey = getKey(reportId, filename);
        LOGGER.debug("Retrieving ids entry {}", entryKey);
        RBucket<Map<String, IdVersion>> bucket = redissonClient.getBucket(entryKey);
        Map<String, IdVersion> netexIds = bucket.get();
        if (netexIds == null) {
            LOGGER.warn("No ids found for entry {}", entryKey);
            netexIds = Collections.emptyMap();
        }
        return netexIds;
    }

    private String getKey(String reportId, String filename) {
        return reportId + '_' + filename;
    }
}
