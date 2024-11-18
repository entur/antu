package no.entur.antu.netexdata.collectors;

import static no.entur.antu.config.cache.CacheConfig.SERVICE_JOURNEY_INTERCHANGE_INFO_CACHE;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
import org.entur.netex.validation.validator.jaxb.NetexDataCollector;
import org.entur.netex.validation.validator.model.ServiceJourneyInterchangeInfo;
import org.redisson.api.RList;
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
    addData(
      validationContext.getFileName(),
      validationContext.getValidationReportId(),
      validationContext
        .serviceJourneyInterchanges()
        .stream()
        .map(serviceJourneyInterchange ->
          ServiceJourneyInterchangeInfo.of(
            validationContext.getFileName(),
            serviceJourneyInterchange
          )
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
    String fileName,
    String validationReportId,
    Stream<ServiceJourneyInterchangeInfo> serviceJourneyInterchangeInfos
  ) {
    RLock lock = redissonClient.getLock(validationReportId);
    try {
      lock.lock();

      String keyName =
        validationReportId +
        "_" +
        SERVICE_JOURNEY_INTERCHANGE_INFO_CACHE +
        "_" +
        fileName;

      RList<String> serviceJourneyInterchangeInfosListCache =
        redissonClient.getList(keyName);
      serviceJourneyInterchangeInfosListCache.addAll(
        serviceJourneyInterchangeInfos
          .map(ServiceJourneyInterchangeInfo::toString)
          .toList()
      );
      serviceJourneyInterchangeInfoCache.put(
        keyName,
        serviceJourneyInterchangeInfosListCache
      );
    } finally {
      if (lock.isHeldByCurrentThread()) {
        lock.unlock();
      }
    }
  }
}
