package no.entur.antu.netexdata.collectors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
import org.entur.netex.validation.validator.jaxb.NetexDataCollector;
import org.entur.netex.validation.validator.model.SimpleLine;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.rutebanken.netex.model.FlexibleLineTypeEnumeration;

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
    addLineName(
      validationContext.getValidationReportId(),
      lineInfo(validationContext)
    );
  }

  @Override
  protected void collectDataFromCommonFile(
    JAXBValidationContext jaxbValidationContext
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

  private SimpleLine lineInfo(JAXBValidationContext validationContext) {
    return validationContext
      .lines()
      .stream()
      .findFirst()
      .map(line -> SimpleLine.of(line, validationContext.getFileName()))
      .orElse(
        validationContext
          .flexibleLines()
          .stream()
          .filter(f ->
            f.getFlexibleLineType() == FlexibleLineTypeEnumeration.FIXED
          )
          .findFirst()
          .map(line -> SimpleLine.of(line, validationContext.getFileName()))
          .orElse(null)
      );
  }
}
