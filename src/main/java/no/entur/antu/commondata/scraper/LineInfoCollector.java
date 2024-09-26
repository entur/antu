package no.entur.antu.commondata.scraper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import no.entur.antu.validation.AntuNetexData;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
import org.entur.netex.validation.validator.jaxb.NetexDataCollector;
import org.entur.netex.validation.validator.model.SimpleLine;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

@Component
public class LineInfoCollector extends NetexDataCollector {

  private final RedissonClient redissonClient;
  private final Map<String, List<String>> lineInfoCache;

  public LineInfoCollector(
    RedissonClient redissonClient,
    Map<String, List<String>> lineInfoCache
  ) {
    this.redissonClient = redissonClient;
    this.lineInfoCache = lineInfoCache;
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
    addLineName(
      antuNetexData.validationReportId(),
      antuNetexData.lineInfo(validationContext.getFileName())
    );
  }

  @Override
  protected void collectDataFromCommonFile(
    JAXBValidationContext validationContext
  ) {
    // No Lines in common files
  }

  public void addLineName(String validationReportId, SimpleLine lineInfo) {
    RLock lock = redissonClient.getLock(validationReportId);
    try {
      lock.lock();

      lineInfoCache.merge(
        validationReportId,
        new ArrayList<>(List.of(lineInfo.toString())),
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
