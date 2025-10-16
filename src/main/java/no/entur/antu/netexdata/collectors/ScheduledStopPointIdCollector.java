package no.entur.antu.netexdata.collectors;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
import org.entur.netex.validation.validator.jaxb.NetexDataCollector;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.rutebanken.netex.model.*;

public class ScheduledStopPointIdCollector extends NetexDataCollector {

  private final RedissonClient redissonClient;
  private final Map<String, Set<String>> scheduledStopPointIdCache;

  public ScheduledStopPointIdCollector(
    RedissonClient redissonClient,
    Map<String, Set<String>> scheduledStopPointIdCache
  ) {
    this.redissonClient = redissonClient;
    this.scheduledStopPointIdCache = scheduledStopPointIdCache;
  }

  private Set<String> getScheduledStopPointIdsFromServiceFrame(
    ServiceFrame serviceFrame
  ) {
    ScheduledStopPointsInFrame_RelStructure serviceVersionFrameStructure =
      serviceFrame.getScheduledStopPoints();
    if (serviceVersionFrameStructure == null) {
      return Set.of();
    }
    List<ScheduledStopPoint> scheduledStopPoints =
      serviceVersionFrameStructure.getScheduledStopPoint();
    if (scheduledStopPoints == null || scheduledStopPoints.isEmpty()) {
      return Set.of();
    } else {
      return scheduledStopPoints
        .stream()
        .map(EntityStructure::getId)
        .collect(Collectors.toSet());
    }
  }

  private Set<String> getAllScheduledStopPointIdsForFile(
    JAXBValidationContext jaxbValidationContext
  ) {
    return jaxbValidationContext
      .getNetexEntitiesIndex()
      .getServiceFrames()
      .stream()
      .flatMap(serviceFrame ->
        getScheduledStopPointIdsFromServiceFrame(serviceFrame).stream()
      )
      .collect(Collectors.toSet());
  }

  private void collectScheduledStopPointIds(
    JAXBValidationContext jaxbValidationContext
  ) {
    Set<String> allScheduledStopPointIdsInFile =
      getAllScheduledStopPointIdsForFile(jaxbValidationContext);

    if (allScheduledStopPointIdsInFile.isEmpty()) {
      return;
    }

    RLock lock = redissonClient.getLock(
      jaxbValidationContext.getValidationReportId()
    );
    try {
      lock.lock();

      String validationReportId = jaxbValidationContext.getValidationReportId();
      if (scheduledStopPointIdCache.containsKey(validationReportId)) {
        Set<String> existingIdsWithNewOnes = scheduledStopPointIdCache.get(
          validationReportId
        );
        existingIdsWithNewOnes.addAll(allScheduledStopPointIdsInFile);
        scheduledStopPointIdCache.put(
          validationReportId,
          existingIdsWithNewOnes
        );
      } else {
        scheduledStopPointIdCache.put(
          validationReportId,
          allScheduledStopPointIdsInFile
        );
      }
    } finally {
      if (lock.isHeldByCurrentThread()) {
        lock.unlock();
      }
    }
  }

  @Override
  protected void collectDataFromLineFile(
    JAXBValidationContext jaxbValidationContext
  ) {
    collectScheduledStopPointIds(jaxbValidationContext);
  }

  @Override
  protected void collectDataFromCommonFile(
    JAXBValidationContext jaxbValidationContext
  ) {
    collectScheduledStopPointIds(jaxbValidationContext);
  }
}
