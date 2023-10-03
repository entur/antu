package no.entur.antu.commondata;

import no.entur.antu.stop.model.QuayId;

public interface CommonDataRepository {

    QuayId findQuayId(String scheduledStopPoint, String validationReportId);
    void loadCommonDataCache(byte[] fileContent, String validationReportId);
    void cleanUp(String validationReportId);
}
