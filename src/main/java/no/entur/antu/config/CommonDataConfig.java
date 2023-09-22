package no.entur.antu.config;

import no.entur.antu.commondata.CommonDataRepository;
import no.entur.antu.commondata.CommonDataRepositoryImpl;
import no.entur.antu.commondata.CommonDataResource;
import no.entur.antu.stop.model.QuayId;
import org.redisson.api.RLocalCachedMap;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static no.entur.antu.config.CacheConfig.QUAY_IDS_PER_SCHEDULED_STOP_POINTS_CACHE;

@Configuration
public class CommonDataConfig {

    @Bean
    @Profile("!test")
    CommonDataResource commonDataResource() {
        return new CommonDataResource();
    }

    @Bean
    @Profile("!test")
    CommonDataRepository commonDataRepository(CommonDataResource commonDataResource,
                                              @Qualifier(QUAY_IDS_PER_SCHEDULED_STOP_POINTS_CACHE)
                                              RLocalCachedMap<String, QuayId> quayIdsPerScheduledStopPointsCache) {
        return new CommonDataRepositoryImpl(commonDataResource, quayIdsPerScheduledStopPointsCache);
    }
}
