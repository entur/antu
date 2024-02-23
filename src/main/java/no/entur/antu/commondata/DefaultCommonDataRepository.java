package no.entur.antu.commondata;

import java.util.Map;
import no.entur.antu.exception.AntuException;
import no.entur.antu.model.QuayId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultCommonDataRepository implements CommonDataRepository {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    DefaultCommonDataRepository.class
  );

  private final CommonDataResource commonDataResource;
  private final Map<String, Map<String, QuayId>> scheduledStopPointAndQuayIdCache;

  public DefaultCommonDataRepository(
    CommonDataResource commonDataResource,
    Map<String, Map<String, QuayId>> scheduledStopPointAndQuayIdCache
  ) {
    this.commonDataResource = commonDataResource;
    this.scheduledStopPointAndQuayIdCache = scheduledStopPointAndQuayIdCache;
  }

  @Override
  public boolean hasQuayIds(String validationReportId) {
    Map<String, QuayId> idsForReport = scheduledStopPointAndQuayIdCache.get(
      validationReportId
    );
    return idsForReport != null && !idsForReport.isEmpty();
  }

  @Override
  public QuayId findQuayIdForScheduledStopPoint(
    String scheduledStopPoint,
    String validationReportId
  ) {
    Map<String, QuayId> idsForReport = scheduledStopPointAndQuayIdCache.get(
      validationReportId
    );
    if (idsForReport == null) {
      throw new AntuException(
        "Quay ids cache not found for validation report with id: " +
        validationReportId
      );
    }
    return idsForReport.get(scheduledStopPoint);
  }

  @Override
  public void loadCommonDataCache(
    byte[] fileContent,
    String validationReportId
  ) {
    commonDataResource.loadCommonData(fileContent);
    // Merging with the existing map, for handing the case where there are
    // multiple common files in the dataset.
    scheduledStopPointAndQuayIdCache.merge(
      validationReportId,
      commonDataResource.getQuayIdsPerScheduledStopPoints(),
      (existingMap, newMap) -> {
        existingMap.putAll(newMap);
        return existingMap;
      }
    );

    LOGGER.info(
      "{} Quay ids for ScheduledStopPoints cached for validation report with id: {}",
      scheduledStopPointAndQuayIdCache.get(validationReportId).size(),
      validationReportId
    );
  }

  @Override
  public void cleanUp(String validationReportId) {
    scheduledStopPointAndQuayIdCache.remove(validationReportId);
  }
}
