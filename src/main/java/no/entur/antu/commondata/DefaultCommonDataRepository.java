package no.entur.antu.commondata;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import no.entur.antu.exception.AntuException;
import no.entur.antu.model.LineInfo;
import no.entur.antu.model.QuayId;
import no.entur.antu.model.ScheduledStopPointId;
import no.entur.antu.model.ScheduledStopPointIds;
import no.entur.antu.model.ServiceLinkId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of CommonDataRepository.
 * This repository is used to store and retrieve common data from Redis cache.
 */
public class DefaultCommonDataRepository implements CommonDataRepository {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    DefaultCommonDataRepository.class
  );

  private final CommonDataResource commonDataResource;
  private final Map<String, Map<String, String>> scheduledStopPointAndQuayIdCache;
  private final Map<String, Map<String, String>> serviceLinksAndScheduledStopPointIdsCache;
  private final Map<String, List<String>> lineInfoCache;

  public DefaultCommonDataRepository(
    CommonDataResource commonDataResource,
    Map<String, Map<String, String>> scheduledStopPointAndQuayIdCache,
    Map<String, Map<String, String>> serviceLinksAndScheduledStopPointIdsCache,
    Map<String, List<String>> lineInfoCache
  ) {
    this.commonDataResource = commonDataResource;
    this.scheduledStopPointAndQuayIdCache = scheduledStopPointAndQuayIdCache;
    this.serviceLinksAndScheduledStopPointIdsCache =
      serviceLinksAndScheduledStopPointIdsCache;
    this.lineInfoCache = lineInfoCache;
  }

  @Override
  public boolean hasQuayIds(String validationReportId) {
    Map<String, String> idsForReport = scheduledStopPointAndQuayIdCache.get(
      validationReportId
    );
    return idsForReport != null && !idsForReport.isEmpty();
  }

  @Override
  public QuayId findQuayIdForScheduledStopPoint(
    ScheduledStopPointId scheduledStopPointId,
    String validationReportId
  ) {
    Map<String, String> idsForReport = scheduledStopPointAndQuayIdCache.get(
      validationReportId
    );
    if (idsForReport == null) {
      throw new AntuException(
        "Quay ids cache not found for validation report with id: " +
        validationReportId
      );
    }
    return QuayId.ofValidId(idsForReport.get(scheduledStopPointId.id()));
  }

  @Override
  public ScheduledStopPointIds findScheduledStopPointIdsForServiceLink(
    ServiceLinkId serviceLinkId,
    String validationReportId
  ) {
    Map<String, String> idsForReport =
      serviceLinksAndScheduledStopPointIdsCache.get(validationReportId);
    if (idsForReport == null) {
      throw new AntuException(
        "Service links cache not found for validation report with id: " +
        validationReportId
      );
    }
    return ScheduledStopPointIds.fromString(
      idsForReport.get(serviceLinkId.id())
    );
  }

  @Override
  public List<LineInfo> getLineNames(String validationReportId) {
    List<String> lineInfoForReportId = lineInfoCache.get(validationReportId);
    if (lineInfoForReportId == null) {
      throw new AntuException(
        "Line names not found for validation report with id: " +
        validationReportId
      );
    }
    return lineInfoForReportId.stream().map(LineInfo::fromString).toList();
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

    Map<String, String> scheduledStopPointIdsPerServiceLinkId =
      commonDataResource
        .getScheduledStopPointIdsPerServiceLinkId()
        .entrySet()
        .stream()
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    serviceLinksAndScheduledStopPointIdsCache.merge(
      validationReportId,
      scheduledStopPointIdsPerServiceLinkId,
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
    serviceLinksAndScheduledStopPointIdsCache.remove(validationReportId);
  }
}
