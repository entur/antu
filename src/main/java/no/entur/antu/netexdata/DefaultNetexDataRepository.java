package no.entur.antu.netexdata;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import no.entur.antu.exception.AntuException;
import no.entur.antu.memorystore.LineInfoMemStoreRepository;
import org.entur.netex.validation.validator.model.ActiveDates;
import org.entur.netex.validation.validator.model.ActiveDatesId;
import org.entur.netex.validation.validator.model.DayTypeId;
import org.entur.netex.validation.validator.model.OperatingDayId;
import org.entur.netex.validation.validator.model.ServiceJourneyId;
import org.entur.netex.validation.validator.model.ServiceJourneyInterchangeInfo;
import org.entur.netex.validation.validator.model.ServiceJourneyStop;
import org.entur.netex.validation.validator.model.SimpleLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of NetexDataRepository.
 * This repository is used to store and retrieve collected data from Redis cache.
 */
public class DefaultNetexDataRepository implements NetexDataRepositoryLoader {

  private final LineInfoMemStoreRepository lineInfoMemStoreRepository;
  private final Map<String, Map<String, List<ServiceJourneyStop>>> serviceJourneyStopsCache;
  private final Map<String, List<String>> serviceJourneyInterchangeInfoCache;
  private final Map<String, Map<ServiceJourneyId, List<LocalDateTime>>> activeDatesByServiceJourneyIdCache;
  private final Map<String, Map<String, List<LocalDateTime>>> dayTypeActiveDatesCache;
  private final Map<String, Map<String, LocalDateTime>> operatingDayActiveDateCache;
  private final Map<String, Set<String>> scheduledStopPointIdsCache;

  private static final Logger LOGGER = LoggerFactory.getLogger(
    DefaultNetexDataRepository.class
  );

  public DefaultNetexDataRepository(
    LineInfoMemStoreRepository lineInfoMemStoreRepository,
    Map<String, Map<String, List<ServiceJourneyStop>>> serviceJourneyStopsCache,
    Map<String, List<String>> serviceJourneyInterchangeInfoCache,
    Map<String, Map<ServiceJourneyId, List<LocalDateTime>>> activeDatesByServiceJourneyId,
    Map<String, Map<String, List<LocalDateTime>>> dayTypeActiveDatesCache,
    Map<String, Map<String, LocalDateTime>> operatingDayActiveDateCache,
    Map<String, Set<String>> scheduledStopPointIdsCache
  ) {
    this.lineInfoMemStoreRepository = lineInfoMemStoreRepository;
    this.serviceJourneyStopsCache = serviceJourneyStopsCache;
    this.serviceJourneyInterchangeInfoCache =
      serviceJourneyInterchangeInfoCache;
    this.activeDatesByServiceJourneyIdCache = activeDatesByServiceJourneyId;
    this.dayTypeActiveDatesCache = dayTypeActiveDatesCache;
    this.operatingDayActiveDateCache = operatingDayActiveDateCache;
    this.scheduledStopPointIdsCache = scheduledStopPointIdsCache;
  }

  @Override
  public List<SimpleLine> lineNames(String validationReportId) {
    List<String> lineInfoForReportId = lineInfoMemStoreRepository.getLineInfo(
      validationReportId
    );
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
          e -> ServiceJourneyId.ofValidId(e.getKey()),
          Map.Entry::getValue,
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
  public Map<ServiceJourneyId, List<DayTypeId>> serviceJourneyDayTypes(
    String validationReportId
  ) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Map<ActiveDatesId, ActiveDates> activeDates(
    String validationReportId
  ) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Map<ServiceJourneyId, List<OperatingDayId>> serviceJourneyOperatingDays(
    String validationReportId
  ) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Map<ServiceJourneyId, List<LocalDateTime>> serviceJourneyIdToActiveDates(
    String validationReportId
  ) {
    return activeDatesByServiceJourneyIdCache.get(validationReportId);
  }

  @Override
  public Set<String> scheduledStopPointIds(String validationReportId) {
    return scheduledStopPointIdsCache.get(validationReportId);
  }

  @Override
  public void cleanUp(String validationReportId) {
    LOGGER.info(
      "Clearing caches for validation report: {}",
      validationReportId
    );

    lineInfoMemStoreRepository.removeLineInfo(validationReportId);
    activeDatesByServiceJourneyIdCache.remove(validationReportId);
    dayTypeActiveDatesCache.remove(validationReportId);
    operatingDayActiveDateCache.remove(validationReportId);

    scheduledStopPointIdsCache
      .keySet()
      .removeIf(k -> k.startsWith(validationReportId));
    serviceJourneyStopsCache
      .keySet()
      .removeIf(k -> k.startsWith(validationReportId));
    serviceJourneyInterchangeInfoCache
      .keySet()
      .removeIf(k -> k.startsWith(validationReportId));

    LOGGER.info(
      "Done clearing caches for validation report: {}",
      validationReportId
    );
  }
}
