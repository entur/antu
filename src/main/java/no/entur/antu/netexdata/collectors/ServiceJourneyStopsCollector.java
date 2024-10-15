package no.entur.antu.netexdata.collectors;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import no.entur.antu.validation.AntuNetexData;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
import org.entur.netex.validation.validator.jaxb.NetexDataCollector;
import org.entur.netex.validation.validator.model.ScheduledStopPointId;
import org.entur.netex.validation.validator.model.ServiceJourneyStop;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

@Component
public class ServiceJourneyStopsCollector extends NetexDataCollector {

  private final RedissonClient redissonClient;
  private final Map<String, Map<String, List<String>>> serviceJourneyStopsCache;

  public ServiceJourneyStopsCollector(
    RedissonClient redissonClient,
    Map<String, Map<String, List<String>>> serviceJourneyStopsCache
  ) {
    this.redissonClient = redissonClient;
    this.serviceJourneyStopsCache = serviceJourneyStopsCache;
  }

  @Override
  protected void collectDataFromLineFile(
    JAXBValidationContext validationContext
  ) {
    AntuNetexData antuNetexData = new AntuNetexData(
      validationContext.getValidationReportId(),
      validationContext.getNetexEntitiesIndex(),
      validationContext.getNetexDataRepository(),
      validationContext.getStopPlaceRepository()
    );

    // TODO: Do I need to check that? Is it possible that some line files
    //  dont have interchanges in it, but other have references to
    //  service journeys in them?
    // if (antuNetexData.serviceJourneyInterchanges().findAny().isPresent()) {}

    Map<String, List<String>> serviceJourneyStops = antuNetexData
      .validServiceJourneys()
      // TODO: unique service journeys ids
      .map(serviceJourney -> {
        Map<String, ScheduledStopPointId> scheduledStopPointIdMap =
          AntuNetexData.scheduledStopPointIdByStopPointId(
            antuNetexData.journeyPattern(serviceJourney)
          );
        return Map.entry(
          serviceJourney.getId(),
          antuNetexData
            .timetabledPassingTimes(serviceJourney)
            .map(passingTime ->
              ServiceJourneyStop.of(
                scheduledStopPointIdMap.get(
                  AntuNetexData.stopPointRef(passingTime)
                ),
                passingTime
              )
            )
            .filter(ServiceJourneyStop::isValid)
            .map(ServiceJourneyStop::toString)
            .toList()
        );
      })
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    addServiceJourneyStops(
      validationContext.getValidationReportId(),
      serviceJourneyStops
    );
  }

  @Override
  protected void collectDataFromCommonFile(
    JAXBValidationContext validationContext
  ) {
    // No service journeys and journey patterns in common files
  }

  private void addServiceJourneyStops(
    String validationReportId,
    Map<String, List<String>> serviceJourneyStops
  ) {
    RLock lock = redissonClient.getLock(validationReportId);
    try {
      lock.lock();

      serviceJourneyStopsCache.merge(
        validationReportId,
        serviceJourneyStops,
        (existingMap, newMap) -> {
          existingMap.putAll(newMap);
          return existingMap;
        }
      );
    } finally {
      if (lock.isHeldByCurrentThread()) {
        lock.unlock();
      }
    }
  }
}
