package no.entur.antu.netexdata.collectors;

import static no.entur.antu.config.cache.CacheConfig.SERVICE_JOURNEY_STOPS_CACHE;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import no.entur.antu.validation.validator.support.NetexUtils;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
import org.entur.netex.validation.validator.jaxb.NetexDataCollector;
import org.entur.netex.validation.validator.model.ScheduledStopPointId;
import org.entur.netex.validation.validator.model.ServiceJourneyStop;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.rutebanken.netex.model.*;
import org.springframework.stereotype.Component;

/**
 * Collects and caches service journey stop information from NeTEx data files.
 *
 * <p>This collector processes service journeys and their associated timetabled passing times
 * to create {@link ServiceJourneyStop} objects that contain information about each stop,
 * including whether passengers can board or alight at each stop.</p>
 *
 * <p>The collected data is stored in a distributed Redis cache using Redisson for
 * thread-safe access across multiple processing nodes.</p>
 */
@Component
public class ServiceJourneyStopsCollector extends NetexDataCollector {

  private final RedissonClient redissonClient;
  private final Map<String, Map<String, List<ServiceJourneyStop>>> serviceJourneyStopsCache;

  /**
   * Constructs a new ServiceJourneyStopsCollector.
   *
   * @param redissonClient the Redis client used for distributed locking and data storage
   * @param serviceJourneyStopsCache the local cache that mirrors the Redis cache data
   */
  public ServiceJourneyStopsCollector(
    RedissonClient redissonClient,
    Map<String, Map<String, List<ServiceJourneyStop>>> serviceJourneyStopsCache
  ) {
    this.redissonClient = redissonClient;
    this.serviceJourneyStopsCache = serviceJourneyStopsCache;
  }

  /**
   * Collects service journey stop data from line files.
   *
   * <p>This method is called during the validation process for each line file.
   * It extracts all service journeys and their stops, then caches the results
   * for later validation use.</p>
   *
   * @param validationContext the validation context containing the NeTEx data
   */
  @Override
  protected void collectDataFromLineFile(
    JAXBValidationContext validationContext
  ) {
    Map<String, List<ServiceJourneyStop>> serviceJourneyStops =
      collectServiceJourneyStops(validationContext);

    addServiceJourneyStops(
      validationContext.getValidationReportId(),
      validationContext.getFileName(),
      serviceJourneyStops
    );
  }

  /**
   * Collects data from common files (no-op for this collector).
   *
   * <p>Common files do not contain service journeys or journey patterns,
   * so this method does nothing.</p>
   *
   * @param validationContext the validation context (unused)
   */
  @Override
  protected void collectDataFromCommonFile(
    JAXBValidationContext validationContext
  ) {
    // No service journeys and journey patterns in common files
  }

  /**
   * Collects all service journey stops from the validation context.
   *
   * @param validationContext the context containing service journey data
   * @return a map where keys are service journey IDs and values are lists of stops for each journey
   */
  private Map<String, List<ServiceJourneyStop>> collectServiceJourneyStops(
    JAXBValidationContext validationContext
  ) {
    return validationContext
      .serviceJourneys()
      .stream()
      .collect(
        Collectors.toMap(
          ServiceJourney::getId,
          serviceJourney ->
            createServiceJourneyStops(serviceJourney, validationContext)
        )
      );
  }

  /**
   * Creates a list of service journey stops for a single service journey.
   *
   * <p>This method processes all timetabled passing times for the journey and
   * creates corresponding {@link ServiceJourneyStop} objects with boarding/alighting
   * information derived from the journey pattern.</p>
   *
   * @param serviceJourney the service journey to process
   * @param validationContext the validation context containing related data
   * @return a list of valid service journey stops
   */
  private List<ServiceJourneyStop> createServiceJourneyStops(
    ServiceJourney serviceJourney,
    JAXBValidationContext validationContext
  ) {
    JourneyPattern journeyPattern = validationContext.journeyPattern(
      serviceJourney
    );
    Map<String, ScheduledStopPointId> scheduledStopPointIdMap =
      NetexUtils.scheduledStopPointIdByStopPointId(journeyPattern);

    return validationContext
      .timetabledPassingTimes(serviceJourney)
      .stream()
      .map(passingTime ->
        createServiceJourneyStop(
          passingTime,
          journeyPattern,
          scheduledStopPointIdMap
        )
      )
      .filter(ServiceJourneyStop::isValid)
      .toList();
  }

  /**
   * Creates a single service journey stop from a timetabled passing time.
   *
   * <p>This method determines whether passengers can board or alight at the stop
   * by examining the stop points in the journey pattern. A stop allows boarding/alighting
   * if any of its stop point definitions in the pattern allow it (or if the property is null,
   * which defaults to allowing the action).</p>
   *
   * @param passingTime the timetabled passing time for this stop
   * @param journeyPattern the journey pattern containing stop definitions
   * @param scheduledStopPointIdMap mapping from stop point references to scheduled stop point IDs
   * @return a service journey stop with boarding/alighting information
   */
  private ServiceJourneyStop createServiceJourneyStop(
    TimetabledPassingTime passingTime,
    JourneyPattern journeyPattern,
    Map<String, ScheduledStopPointId> scheduledStopPointIdMap
  ) {
    String stopPointInJourneyPatternRef = extractStopPointRef(passingTime);

    List<StopPointInJourneyPattern_VersionedChildStructure> stopPoints =
      findStopPointsInPattern(journeyPattern, stopPointInJourneyPatternRef);

    boolean isForAlighting = isAnyStopPointForAlighting(stopPoints);
    boolean isForBoarding = isAnyStopPointForBoarding(stopPoints);

    ScheduledStopPointId scheduledStopPointId = scheduledStopPointIdMap.get(
      NetexUtils.stopPointRef(passingTime)
    );

    return ServiceJourneyStop.of(
      scheduledStopPointId,
      passingTime,
      isForAlighting,
      isForBoarding
    );
  }

  private String extractStopPointRef(TimetabledPassingTime passingTime) {
    return passingTime.getPointInJourneyPatternRef().getValue().getRef();
  }

  /**
   * Finds all stop points in a journey pattern that match the given reference.
   *
   * <p>Note: There can be multiple occurrences of the same stop point reference
   * in a journey pattern (e.g., for circular routes).</p>
   *
   * @param journeyPattern the journey pattern to search
   * @param stopPointInJourneyPatternRef the reference ID to find
   * @return a list of matching stop points
   */
  private List<StopPointInJourneyPattern_VersionedChildStructure> findStopPointsInPattern(
    JourneyPattern journeyPattern,
    String stopPointInJourneyPatternRef
  ) {
    return journeyPattern
      .getPointsInSequence()
      .getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern()
      .stream()
      .filter(this::isStopPointInJourneyPattern)
      .filter(point -> point.getId().equals(stopPointInJourneyPatternRef))
      .map(point -> (StopPointInJourneyPattern_VersionedChildStructure) point)
      .toList();
  }

  private boolean isStopPointInJourneyPattern(
    PointInLinkSequence_VersionedChildStructure point
  ) {
    return point instanceof StopPointInJourneyPattern_VersionedChildStructure;
  }

  /**
   * Determines if any of the stop points allow passenger alighting.
   *
   * <p>A stop allows alighting if at least one stop point definition either
   * explicitly allows it (isForAlighting = true) or doesn't specify
   * (isForAlighting = null, which defaults to true).</p>
   *
   * @param stopPoints the list of stop point definitions to check
   * @return true if alighting is allowed at any stop point
   */
  private boolean isAnyStopPointForAlighting(
    List<StopPointInJourneyPattern_VersionedChildStructure> stopPoints
  ) {
    return stopPoints
      .stream()
      .anyMatch(stopPoint ->
        stopPoint.isForAlighting() == null || stopPoint.isForAlighting()
      );
  }

  /**
   * Determines if any of the stop points allow passenger boarding.
   *
   * <p>A stop allows boarding if at least one stop point definition either
   * explicitly allows it (isForBoarding = true) or doesn't specify
   * (isForBoarding = null, which defaults to true).</p>
   *
   * @param stopPoints the list of stop point definitions to check
   * @return true if boarding is allowed at any stop point
   */
  private boolean isAnyStopPointForBoarding(
    List<StopPointInJourneyPattern_VersionedChildStructure> stopPoints
  ) {
    return stopPoints
      .stream()
      .anyMatch(stopPoint ->
        stopPoint.isForBoarding() == null || stopPoint.isForBoarding()
      );
  }

  /**
   * Adds the collected service journey stops to the distributed cache.
   *
   * <p>This method uses Redis distributed locking to ensure thread-safe updates
   * when multiple processes are collecting data concurrently. The data is stored
   * both in Redis and in a local cache for performance.</p>
   *
   * @param validationReportId the ID of the validation report
   * @param filename the name of the file being processed
   * @param serviceJourneyStops the stops to cache
   */
  private void addServiceJourneyStops(
    String validationReportId,
    String filename,
    Map<String, List<ServiceJourneyStop>> serviceJourneyStops
  ) {
    RLock lock = redissonClient.getLock(validationReportId);
    try {
      lock.lock();

      String cacheKey = buildCacheKey(validationReportId, filename);
      RMap<String, List<ServiceJourneyStop>> serviceJourneyStopsMap =
        redissonClient.getMap(cacheKey);

      serviceJourneyStopsMap.putAll(serviceJourneyStops);
      serviceJourneyStopsCache.put(cacheKey, serviceJourneyStopsMap);
    } finally {
      if (lock.isHeldByCurrentThread()) {
        lock.unlock();
      }
    }
  }

  private String buildCacheKey(String validationReportId, String filename) {
    return (
      validationReportId + "_" + SERVICE_JOURNEY_STOPS_CACHE + "_" + filename
    );
  }
}
