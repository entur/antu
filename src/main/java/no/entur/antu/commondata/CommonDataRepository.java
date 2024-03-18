package no.entur.antu.commondata;

import no.entur.antu.model.QuayId;

/**
 * Repository for common data from the Netex Common file.
 * This repository is used to store and retrieve common data.
 */
public interface CommonDataRepository {
  boolean hasQuayIds(String validationReportId);

  QuayId findQuayIdForScheduledStopPoint(
    String scheduledStopPoint,
    String validationReportId
  );

  void loadCommonDataCache(byte[] fileContent, String validationReportId);

  /**
   * Clean up the common data cache.
   * This method is used to clean up the common data cache.
   * Caution: Not an unused method, referred in camel route AggregateValidationReportsRouteBuilder
   */
  void cleanUp(String validationReportId);
}
