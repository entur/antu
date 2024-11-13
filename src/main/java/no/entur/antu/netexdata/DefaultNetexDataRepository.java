package no.entur.antu.netexdata;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import no.entur.antu.exception.AntuException;
import org.entur.netex.validation.validator.jaxb.*;
import org.entur.netex.validation.validator.model.*;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of NetexDataRepository.
 * This repository is used to store and retrieve common data from Redis cache.
 */
public class DefaultNetexDataRepository implements NetexDataRepository {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    DefaultNetexDataRepository.class
  );

  private final RedissonClient redissonClient;
  private final Map<String, Map<String, String>> scheduledStopPointAndQuayIdCache;
  private final Map<String, Map<String, String>> serviceLinksAndFromToScheduledStopPointIdCache;
  private final Map<String, List<String>> lineInfoCache;
  private final Map<String, Map<String, List<String>>> serviceJourneyStopsCache;
  private final Map<String, List<String>> serviceJourneyInterchangeInfoCache;

  public DefaultNetexDataRepository(
    RedissonClient redissonClient,
    Map<String, Map<String, String>> scheduledStopPointAndQuayIdCache,
    Map<String, Map<String, String>> serviceLinksAndFromToScheduledStopPointIdCache,
    Map<String, List<String>> lineInfoCache,
    Map<String, Map<String, List<String>>> serviceJourneyStopsCache,
    Map<String, List<String>> serviceJourneyInterchangeInfoCache
  ) {
    this.redissonClient = redissonClient;
    this.scheduledStopPointAndQuayIdCache = scheduledStopPointAndQuayIdCache;
    this.serviceLinksAndFromToScheduledStopPointIdCache =
      serviceLinksAndFromToScheduledStopPointIdCache;
    this.lineInfoCache = lineInfoCache;
    this.serviceJourneyStopsCache = serviceJourneyStopsCache;
    this.serviceJourneyInterchangeInfoCache =
      serviceJourneyInterchangeInfoCache;
  }

  @Override
  public boolean hasQuayIds(String validationReportId) {
    Map<String, String> idsForReport = scheduledStopPointAndQuayIdCache.get(
      validationReportId
    );
    return idsForReport != null && !idsForReport.isEmpty();
  }

  @Override
  public QuayId quayIdForScheduledStopPoint(
    ScheduledStopPointId scheduledStopPointId,
    String validationReportId
  ) {
    Map<String, String> idsForReport = scheduledStopPointAndQuayIdCache.get(
      validationReportId
    );
    if (idsForReport == null) {
      throw new AntuException(
        "Quay ids cache not found for validation report with id: " +
        validationReportId
      );
    }
    return QuayId.ofValidId(idsForReport.get(scheduledStopPointId.id()));
  }

  @Override
  public FromToScheduledStopPointId fromToScheduledStopPointIdForServiceLink(
    ServiceLinkId serviceLinkId,
    String validationReportId
  ) {
    Map<String, String> idsForReport =
      serviceLinksAndFromToScheduledStopPointIdCache.get(validationReportId);
    if (idsForReport == null) {
      throw new AntuException(
        "Service links cache not found for validation report with id: " +
        validationReportId
      );
    }
    return FromToScheduledStopPointId.fromString(
      idsForReport.get(serviceLinkId.id())
    );
  }

  @Override
  public List<SimpleLine> lineNames(String validationReportId) {
    List<String> lineInfoForReportId = lineInfoCache.get(validationReportId);
    if (lineInfoForReportId == null) {
      throw new AntuException(
        "Line names not found for validation report with id: " +
        validationReportId
      );
    }
    return lineInfoForReportId.stream().map(SimpleLine::fromString).toList();
  }

  public Map<ServiceJourneyId, List<ServiceJourneyStop>> serviceJourneyStops(
    String validationReportId
  ) {
    return serviceJourneyStopsCache
      .keySet()
      .stream()
      .filter(k -> k.startsWith(validationReportId))
      .map(serviceJourneyStopsCache::get)
      .flatMap(m -> m.entrySet().stream())
      .collect(
        Collectors.toMap(
          k -> ServiceJourneyId.ofValidId(k.getKey()),
          v ->
            v.getValue().stream().map(ServiceJourneyStop::fromString).toList(),
          (p, n) -> n
        )
      );
  }

  @Override
  public List<ServiceJourneyInterchangeInfo> serviceJourneyInterchangeInfos(
    String validationReportId
  ) {
    return Optional
      .ofNullable(serviceJourneyInterchangeInfoCache)
      .map(Map::entrySet)
      .stream()
      .flatMap(Set::stream)
      .filter(entry -> entry.getKey().startsWith(validationReportId))
      .flatMap(entry -> entry.getValue().stream())
      .map(ServiceJourneyInterchangeInfo::fromString)
      .toList();
  }

  @Override
  public void fillNetexDataCache(
    byte[] fileContent,
    String validationReportId
  ) {
    NetexDataResource netexDataResource = new NetexDataResource();
    netexDataResource.loadNetexData(fileContent);
    // Merging with the existing map, for handing the case where there are
    // multiple common files in the dataset.
    scheduledStopPointAndQuayIdCache.merge(
      validationReportId,
      netexDataResource.getQuayIdsPerScheduledStopPoints(),
      (existingMap, newMap) -> {
        existingMap.putAll(newMap);
        return existingMap;
      }
    );

    Map<String, String> scheduledStopPointIdsPerServiceLinkId =
      netexDataResource
        .getFromToScheduledStopPointIdPerServiceLinkId()
        .entrySet()
        .stream()
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

    serviceLinksAndFromToScheduledStopPointIdCache.merge(
      validationReportId,
      scheduledStopPointIdsPerServiceLinkId,
      (existingMap, newMap) -> {
        existingMap.putAll(newMap);
        return existingMap;
      }
    );

    LOGGER.info(
      "{} Quay ids for ScheduledStopPoints cached for validation report with id: {}",
      scheduledStopPointAndQuayIdCache.get(validationReportId).size(),
      validationReportId
    );
  }

  @Override
  public void cleanUp(String validationReportId) {
    scheduledStopPointAndQuayIdCache.remove(validationReportId);
    serviceLinksAndFromToScheduledStopPointIdCache.remove(validationReportId);
    lineInfoCache.remove(validationReportId);
    redissonClient.getKeys().deleteByPattern(validationReportId + '*');
    serviceJourneyStopsCache
      .keySet()
      .removeIf(k -> k.startsWith(validationReportId));
    serviceJourneyInterchangeInfoCache
      .keySet()
      .removeIf(k -> k.startsWith(validationReportId));
  }
}
