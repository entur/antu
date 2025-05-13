package no.entur.antu.netexdata;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import no.entur.antu.exception.AntuException;
import org.entur.netex.validation.validator.model.ActiveDates;
import org.entur.netex.validation.validator.model.ActiveDatesId;
import org.entur.netex.validation.validator.model.DayTypeId;
import org.entur.netex.validation.validator.model.OperatingDayId;
import org.entur.netex.validation.validator.model.ServiceJourneyId;
import org.entur.netex.validation.validator.model.ServiceJourneyInterchangeInfo;
import org.entur.netex.validation.validator.model.ServiceJourneyStop;
import org.entur.netex.validation.validator.model.SimpleLine;

/**
 * Default implementation of NetexDataRepository.
 * This repository is used to store and retrieve collected data from Redis cache.
 */
public class DefaultNetexDataRepository implements NetexDataRepositoryLoader {

  private final Map<String, List<String>> lineInfoCache;
  private final Map<String, Map<String, List<String>>> serviceJourneyStopsCache;
  private final Map<String, List<String>> serviceJourneyInterchangeInfoCache;
  private final Map<String, Map<String, List<LocalDateTime>>> activeDatesByServiceJourneyIdCache;

  public DefaultNetexDataRepository(
    Map<String, List<String>> lineInfoCache,
    Map<String, Map<String, List<String>>> serviceJourneyStopsCache,
    Map<String, List<String>> serviceJourneyInterchangeInfoCache,
    Map<String, Map<String, List<LocalDateTime>>> activeDatesByServiceJourneyId
  ) {
    this.lineInfoCache = lineInfoCache;
    this.serviceJourneyStopsCache = serviceJourneyStopsCache;
    this.serviceJourneyInterchangeInfoCache =
      serviceJourneyInterchangeInfoCache;
    this.activeDatesByServiceJourneyIdCache = activeDatesByServiceJourneyId;
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
  public Map<String, List<LocalDateTime>> serviceJourneyIdToActiveDates(
          String validationReportId
  ) {
    return activeDatesByServiceJourneyIdCache.get(validationReportId);
  }

  @Override
  public void cleanUp(String validationReportId) {
    lineInfoCache.remove(validationReportId);
    serviceJourneyStopsCache
      .keySet()
      .removeIf(k -> k.startsWith(validationReportId));
    serviceJourneyInterchangeInfoCache
      .keySet()
      .removeIf(k -> k.startsWith(validationReportId));
  }
}
