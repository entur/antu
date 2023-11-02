package no.entur.antu.config;

import no.entur.antu.commondata.CommonDataRepository;
import no.entur.antu.commondata.CommonDataRepositoryImpl;
import no.entur.antu.commondata.CommonDataResource;
import no.entur.antu.model.QuayId;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.Map;

import static no.entur.antu.config.CacheConfig.QUAY_ID_FOR_SCHEDULED_STOP_POINT_CACHE;

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
                                              @Qualifier(QUAY_ID_FOR_SCHEDULED_STOP_POINT_CACHE)
                                              Map<String, Map<String, QuayId>> quayIdForScheduledStopPointCache) {
        return new CommonDataRepositoryImpl(commonDataResource, quayIdForScheduledStopPointCache);
    }
}
