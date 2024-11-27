package no.entur.antu.netexdata.collectors;

import static no.entur.antu.config.cache.CacheConfig.SERVICE_JOURNEY_DAY_TYPES_CACHE;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import no.entur.antu.validation.validator.support.NetexUtils;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
import org.entur.netex.validation.validator.jaxb.NetexDataCollector;
import org.entur.netex.validation.validator.model.DayTypeId;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;

public class ServiceJourneyDayTypesCollector extends NetexDataCollector {

  private final RedissonClient redissonClient;
  private final Map<String, Map<String, String>> serviceJourneyDayTypesCache;

  public ServiceJourneyDayTypesCollector(
    RedissonClient redissonClient,
    Map<String, Map<String, String>> serviceJourneyDayTypesCache
  ) {
    this.redissonClient = redissonClient;
    this.serviceJourneyDayTypesCache = serviceJourneyDayTypesCache;
  }

  @Override
  protected void collectDataFromLineFile(
    JAXBValidationContext validationContext
  ) {
    Map<String, String> serviceJourneyDayTypes = NetexUtils
      .validServiceJourneys(validationContext)
      .stream()
      .map(serviceJourney ->
        Map.entry(
          serviceJourney.getId(),
          DayTypeId
            .of(serviceJourney)
            .stream()
            .map(DayTypeId::toString)
            .toList()
        )
      )
      .filter(entry -> !entry.getValue().isEmpty())
      .collect(
        Collectors.toMap(
          Map.Entry::getKey,
          entry -> String.join(",", entry.getValue())
        )
      );

    if (!serviceJourneyDayTypes.isEmpty()) {
      addServiceJourneyDayTypes(
        validationContext.getValidationReportId(),
        validationContext.getFileName(),
        serviceJourneyDayTypes
      );
    }
  }

  @Override
  protected void collectDataFromCommonFile(
    JAXBValidationContext validationContext
  ) {
    // No service journeys and journey patterns in common files
  }

  private void addServiceJourneyDayTypes(
    String validationReportId,
    String filename,
    Map<String, String> serviceJourneyDayTypes
  ) {
    RLock lock = redissonClient.getLock(validationReportId);
    try {
      lock.lock();

      String keyName =
        validationReportId +
        "_" +
        SERVICE_JOURNEY_DAY_TYPES_CACHE +
        "_" +
        filename;

      RMap<String, String> serviceJourneyDayTypesMap = redissonClient.getMap(
        keyName
      );
      serviceJourneyDayTypesMap.putAll(serviceJourneyDayTypes);
      serviceJourneyDayTypesCache.put(keyName, serviceJourneyDayTypesMap);
    } finally {
      if (lock.isHeldByCurrentThread()) {
        lock.unlock();
      }
    }
  }
}
