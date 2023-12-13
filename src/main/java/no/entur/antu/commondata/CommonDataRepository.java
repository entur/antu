package no.entur.antu.commondata;

import no.entur.antu.model.QuayId;

import java.util.Map;

public interface CommonDataRepository {

    Map<String, QuayId> isScheduledStopPointAssigned(String scheduledStopPoint, String validationReportId);

    boolean hasQuayIds(String validationReportId);

    QuayId findQuayIdForScheduledStopPoint(String scheduledStopPoint, String validationReportId);

    void loadCommonDataCache(byte[] fileContent, String validationReportId);

    void cleanUp(String validationReportId);
}
