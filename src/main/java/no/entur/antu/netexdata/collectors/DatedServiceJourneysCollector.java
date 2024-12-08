package no.entur.antu.netexdata.collectors;

import static no.entur.antu.config.cache.CacheConfig.SERVICE_JOURNEY_OPERATING_DAYS_CACHE;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import jakarta.xml.bind.JAXBElement;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
import org.entur.netex.validation.validator.jaxb.NetexDataCollector;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.rutebanken.netex.model.DatedServiceJourney;
import org.rutebanken.netex.model.ServiceJourneyRefStructure;

public class DatedServiceJourneysCollector extends NetexDataCollector {

  private final RedissonClient redissonClient;
  private final Map<String, Map<String, String>> serviceJourneyOperatingDaysCache;

  public DatedServiceJourneysCollector(
    RedissonClient redissonClient,
    Map<String, Map<String, String>> serviceJourneyOperatingDaysCache
  ) {
    this.redissonClient = redissonClient;
    this.serviceJourneyOperatingDaysCache = serviceJourneyOperatingDaysCache;
  }

  @Override
  protected void collectDataFromLineFile(
    JAXBValidationContext validationContext
  ) {
    Multimap<String, String> serviceJourneyOperatingDays = validationContext
      .datedServiceJourneys()
      .stream()
      .map(DatedServiceJourneysCollector::operatingDaysRefsPerServiceJourney)
      .filter(Optional::isPresent)
      .map(Optional::get)
      .collect(toMultimap(Map.Entry::getKey, Map.Entry::getValue));

    addServiceJourneyOperatingDays(
      validationContext.getValidationReportId(),
      validationContext.getFileName(),
      serviceJourneyOperatingDays
        .asMap()
        .entrySet()
        .stream()
        .collect(
          Collectors.toMap(
            Map.Entry::getKey,
            entry -> String.join(",", entry.getValue())
          )
        )
    );
  }

  @Override
  protected void collectDataFromCommonFile(
    JAXBValidationContext validationContext
  ) {
    // No service journeys and journey patterns in common files
  }

  /**
   * List of operating days references per service journey.
   * There is only one serviceJourneyRef per datedServiceJourney.
   */
  private static Optional<Map.Entry<String, String>> operatingDaysRefsPerServiceJourney(
    DatedServiceJourney datedServiceJourney
  ) {
    return datedServiceJourney
      .getJourneyRef()
      .stream()
      .map(JAXBElement::getValue)
      .filter(ServiceJourneyRefStructure.class::isInstance)
      .map(ServiceJourneyRefStructure.class::cast)
      .filter(serviceJourneyRef -> serviceJourneyRef.getRef() != null)
      .map(serviceJourneyRef ->
        Map.entry(
          serviceJourneyRef.getRef(),
          datedServiceJourney.getOperatingDayRef().getRef()
        )
      )
      .findFirst();
  }

  private void addServiceJourneyOperatingDays(
    String validationReportId,
    String filename,
    Map<String, String> serviceJourneyOperatingDays
  ) {
    RLock lock = redissonClient.getLock(validationReportId);
    try {
      lock.lock();

      String keyName =
        validationReportId +
        "_" +
        SERVICE_JOURNEY_OPERATING_DAYS_CACHE +
        "_" +
        filename;

      RMap<String, String> serviceJourneyOperatingDaysForFile =
        redissonClient.getMap(keyName);
      serviceJourneyOperatingDaysForFile.putAll(serviceJourneyOperatingDays);
      serviceJourneyOperatingDaysCache.put(
        keyName,
        serviceJourneyOperatingDaysForFile
      );
    } finally {
      if (lock.isHeldByCurrentThread()) {
        lock.unlock();
      }
    }
  }

  static <K1, K, V> Collector<K1, ?, Multimap<K, V>> toMultimap(
    Function<K1, K> keyMapper,
    Function<K1, V> valueMapper
  ) {
    return Collector.of(
      ArrayListMultimap::create, // Supplier: Create a new Multimap
      (multimap, assignment) ->
        multimap.put(
          keyMapper.apply(assignment),
          valueMapper.apply(assignment)
        ), // Accumulator
      (m1, m2) -> { // Combiner
        m1.putAll(m2);
        return m1;
      }
    );
  }
}
