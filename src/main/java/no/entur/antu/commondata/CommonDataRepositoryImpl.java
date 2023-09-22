package no.entur.antu.commondata;

import no.entur.antu.stop.model.QuayId;

import java.util.Map;

public class CommonDataRepositoryImpl implements CommonDataRepository {

    private final CommonDataResource commonDataResource;
    private final Map<String, QuayId> quayIdsPerScheduledStopPointsCache;

    public CommonDataRepositoryImpl(CommonDataResource commonDataResource,
                                    Map<String, QuayId> quayIdsPerScheduledStopPointsCache) {
        this.commonDataResource = commonDataResource;
        this.quayIdsPerScheduledStopPointsCache = quayIdsPerScheduledStopPointsCache;
    }

    public QuayId findQuayId(String scheduledStopPoint) {
        return quayIdsPerScheduledStopPointsCache.get(scheduledStopPoint);
    }

    public void loadCommonDataCache(byte[] fileContent) {
        commonDataResource.loadCommonData(fileContent);
        quayIdsPerScheduledStopPointsCache.putAll(
                commonDataResource.getQuayIdsPerScheduledStopPoints()
        );
    }

    public void cleanUp() {
        quayIdsPerScheduledStopPointsCache.clear();
    }
}
