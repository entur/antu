package no.entur.antu.commondata;

import no.entur.antu.exception.AntuException;
import no.entur.antu.stop.model.QuayId;

import java.util.Map;

public class CommonDataRepositoryImpl implements CommonDataRepository {

    private final CommonDataResource commonDataResource;
    private final Map<String, Map<String, QuayId>> quayIdForScheduledStopPointCache;

    public CommonDataRepositoryImpl(CommonDataResource commonDataResource,
                                    Map<String, Map<String, QuayId>> quayIdForScheduledStopPointCache) {
        this.commonDataResource = commonDataResource;
        this.quayIdForScheduledStopPointCache = quayIdForScheduledStopPointCache;
    }

    public QuayId findQuayId(String scheduledStopPoint, String validationReportId) {

        Map<String, QuayId> idsForReport = quayIdForScheduledStopPointCache.get(validationReportId);
        if (idsForReport == null) {
            throw new AntuException("Quay ids cache not found for validation report with id: " + validationReportId);
        }
        return idsForReport.get(scheduledStopPoint);
    }

    public void loadCommonDataCache(byte[] fileContent, String validationReportId) {
        commonDataResource.loadCommonData(fileContent);
        // Merging with the existing map, for handing the case where there are
        // multiple common files in the dataset.
        quayIdForScheduledStopPointCache.merge(
                validationReportId,
                commonDataResource.getQuayIdsPerScheduledStopPoints(),
                (existingMap, newMap) -> {
                    existingMap.putAll(newMap);
                    return existingMap;
                });
    }

    public void cleanUp(String validationReportId) {
        quayIdForScheduledStopPointCache.remove(validationReportId);
    }
}
