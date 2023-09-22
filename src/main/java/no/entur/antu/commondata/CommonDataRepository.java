package no.entur.antu.commondata;

import no.entur.antu.stop.model.QuayId;

public interface CommonDataRepository {

    QuayId findQuayId(String scheduledStopPoint);
    void loadCommonDataCache(byte[] fileContent);
    void cleanUp();
}
