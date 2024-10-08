package no.entur.antu.netexdata.collectors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import no.entur.antu.validation.AntuNetexData;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
import org.entur.netex.validation.validator.jaxb.NetexDataCollector;
import org.entur.netex.validation.validator.model.ServiceJourneyInterchangeInfo;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

public class ServiceJourneyInterchangeInfoCollector extends NetexDataCollector {

  private final RedissonClient redissonClient;
  private final Map<String, List<String>> serviceJourneyInterchangeInfoCache;

  public ServiceJourneyInterchangeInfoCollector(
    RedissonClient redissonClient,
    Map<String, List<String>> serviceJourneyInterchangeInfoCache
  ) {
    this.redissonClient = redissonClient;
    this.serviceJourneyInterchangeInfoCache =
      serviceJourneyInterchangeInfoCache;
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

    antuNetexData
      .serviceJourneyInterchanges()
      .map(serviceJourneyInterchange ->
        ServiceJourneyInterchangeInfo.of(
          validationContext.getFileName(),
          serviceJourneyInterchange
        )
      )
      .forEach(serviceJourneyInterchangeInfo ->
        addData(
          antuNetexData.validationReportId(),
          serviceJourneyInterchangeInfo
        )
      );
  }

  @Override
  protected void collectDataFromCommonFile(
    JAXBValidationContext validationContext
  ) {
    // ServiceJourneyInterchange are only in line files
  }

  private void addData(
    String validationReportId,
    ServiceJourneyInterchangeInfo serviceJourneyInterchangeInfo
  ) {
    RLock lock = redissonClient.getLock(validationReportId);
    try {
      lock.lock();

      serviceJourneyInterchangeInfoCache.merge(
        validationReportId,
        new ArrayList<>(List.of(serviceJourneyInterchangeInfo.toString())),
        (existingList, newList) -> {
          existingList.addAll(newList);
          return existingList;
        }
      );
    } finally {
      if (lock.isHeldByCurrentThread()) {
        lock.unlock();
      }
    }
  }
}
