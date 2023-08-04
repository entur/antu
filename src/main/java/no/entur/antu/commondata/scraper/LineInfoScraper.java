package no.entur.antu.commondata.scraper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import no.entur.antu.model.LineInfo;
import no.entur.antu.validation.AntuNetexData;
import no.entur.antu.validation.ValidationContextWithNetexEntitiesIndex;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

public class LineInfoScraper extends CommonDataScraper {

  private final RedissonClient redissonClient;
  private final Map<String, List<String>> lineInfoCache;

  public LineInfoScraper(
    RedissonClient redissonClient,
    Map<String, List<String>> lineInfoCache
  ) {
    this.redissonClient = redissonClient;
    this.lineInfoCache = lineInfoCache;
  }

  @Override
  protected void scrapeDataFromLineFile(
    ValidationContextWithNetexEntitiesIndex validationContext
  ) {
    AntuNetexData antuNetexData = validationContext.getAntuNetexData();
    addLineInfo(
      validationContext.getAntuNetexData().validationReportId(),
      antuNetexData.lineInfo(validationContext.getFileName())
    );
  }

  @Override
  protected void scrapeDataFromCommonFile(
    ValidationContextWithNetexEntitiesIndex validationContext
  ) {
    // No lines in common files
  }

  private void addLineInfo(String validationReportId, LineInfo lineInfo) {
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
