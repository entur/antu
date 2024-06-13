package no.entur.antu.commondata;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import no.entur.antu.model.LineInfo;
import no.entur.antu.validation.AntuNetexData;
import no.entur.antu.validation.ValidationContextWithNetexEntitiesIndex;
import org.entur.netex.validation.validator.xpath.ValidationContext;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

public class LineInfoScraper implements CommonDataScraper {

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
  public void scrapeData(ValidationContext validationContext) {
    if (
      validationContext instanceof ValidationContextWithNetexEntitiesIndex validationContextWithNetexEntitiesIndex
    ) {
      AntuNetexData antuNetexData =
        validationContextWithNetexEntitiesIndex.getAntuNetexData();
      addLineInfo(
        validationContextWithNetexEntitiesIndex
          .getAntuNetexData()
          .validationReportId(),
        antuNetexData.lineInfo(validationContext.getFileName())
      );
    } else {
      throw new IllegalArgumentException(
        "ValidationContext must be of type ValidationContextWithNetexEntitiesIndex"
      );
    }
  }

  public void addLineInfo(String validationReportId, LineInfo lineInfo) {
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
