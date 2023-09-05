package no.entur.antu.commondata;

import java.util.Map;

public class CommonDataRepository {

    private final CommonDataResource commonDataResource;
    private final Map<String, String> stopPlaceIdPerScheduledStopPointsCache;

    public CommonDataRepository(CommonDataResource commonDataResource,
                                Map<String, String> stopPlaceIdPerScheduledStopPointsCache) {
        this.commonDataResource = commonDataResource;
        this.stopPlaceIdPerScheduledStopPointsCache = stopPlaceIdPerScheduledStopPointsCache;
    }

    public String findStopPlaceId(String scheduledStopPoint) {
        return stopPlaceIdPerScheduledStopPointsCache.get(scheduledStopPoint);
    }

    public void loadCommonDataCache(byte[] fileContent) {
        commonDataResource.loadCommonData(fileContent);
        stopPlaceIdPerScheduledStopPointsCache.putAll(
                commonDataResource.getStopPlaceIdsPerScheduledStopPoints()
        );
    }

    public void cleanUp() {
        stopPlaceIdPerScheduledStopPointsCache.clear();
    }
}
