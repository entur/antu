package no.entur.antu.commondata;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import no.entur.antu.exception.AntuException;
import org.entur.netex.validation.validator.jaxb.*;
import org.entur.netex.validation.validator.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of NetexDataRepository.
 * This repository is used to store and retrieve common data from Redis cache.
 */
public class DefaultNetexDataRepository implements NetexDataRepository {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    DefaultNetexDataRepository.class
  );

  private final CommonDataResource commonDataResource;
  private final Map<String, Map<String, String>> scheduledStopPointAndQuayIdCache;
  private final Map<String, Map<String, String>> serviceLinksAndFromToScheduledStopPointIdCache;
  private final Map<String, List<String>> lineInfoCache;

  public DefaultNetexDataRepository(
    CommonDataResource commonDataResource,
    Map<String, Map<String, String>> scheduledStopPointAndQuayIdCache,
    Map<String, Map<String, String>> serviceLinksAndFromToScheduledStopPointIdCache,
    Map<String, List<String>> lineInfoCache
  ) {
    this.commonDataResource = commonDataResource;
    this.scheduledStopPointAndQuayIdCache = scheduledStopPointAndQuayIdCache;
    this.serviceLinksAndFromToScheduledStopPointIdCache =
      serviceLinksAndFromToScheduledStopPointIdCache;
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
  public FromToScheduledStopPointId findFromToScheduledStopPointIdForServiceLink(
    ServiceLinkId serviceLinkId,
    String validationReportId
  ) {
    Map<String, String> idsForReport =
      serviceLinksAndFromToScheduledStopPointIdCache.get(validationReportId);
    if (idsForReport == null) {
      throw new AntuException(
        "Service links cache not found for validation report with id: " +
        validationReportId
      );
    }
    return FromToScheduledStopPointId.fromString(
      idsForReport.get(serviceLinkId.id())
    );
  }

  @Override
  public List<SimpleLine> getLineNames(String validationReportId) {
    List<String> lineInfoForReportId = lineInfoCache.get(validationReportId);
    if (lineInfoForReportId == null) {
      throw new AntuException(
        "Line names not found for validation report with id: " +
        validationReportId
      );
    }
    return lineInfoForReportId.stream().map(SimpleLine::fromString).toList();
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
        .getFromToScheduledStopPointIdPerServiceLinkId()
        .entrySet()
        .stream()
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    serviceLinksAndFromToScheduledStopPointIdCache.merge(
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
    LOGGER.info(
      "Cleaning up common data cache for validation report with id: {}",
      validationReportId
    );
    scheduledStopPointAndQuayIdCache.remove(validationReportId);
    serviceLinksAndFromToScheduledStopPointIdCache.remove(validationReportId);
    lineInfoCache.remove(validationReportId);
  }
}
