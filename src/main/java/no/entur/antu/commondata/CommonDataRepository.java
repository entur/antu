package no.entur.antu.commondata;

import java.util.List;
import java.util.Map;
import no.entur.antu.model.LineInfo;
import no.entur.antu.model.QuayId;
import no.entur.antu.model.ScheduledStopPointId;
import no.entur.antu.model.ScheduledStopPointIds;
import no.entur.antu.model.ServiceLinkId;

/**
 * Repository for common data from the Netex Common file.
 * This repository is used to store and retrieve common data.
 */
public interface CommonDataRepository {
  boolean hasQuayIds(String validationReportId);

  QuayId findQuayIdForScheduledStopPoint(
    ScheduledStopPointId scheduledStopPointId,
    String validationReportId
  );

  ScheduledStopPointIds findScheduledStopPointIdsForServiceLink(
    ServiceLinkId serviceLinkId,
    String validationReportId
  );

  List<LineInfo> getLineNames(String validationReportId);

  List<ScheduledStopPointId> getScheduledStopPointsForServiceJourney(
    String validationReportId,
    String serviceJourneyId
  );

  void loadCommonDataCache(byte[] fileContent, String validationReportId);

  /**
   * Clean up the common data cache.
   * This method is used to clean up the common data cache.
   * Caution: Not an unused method, referred in camel route AggregateValidationReportsRouteBuilder
   */
  void cleanUp(String validationReportId);
}
