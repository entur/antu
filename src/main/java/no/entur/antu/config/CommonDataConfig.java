package no.entur.antu.config;

import no.entur.antu.commondata.CommonDataRepository;
import no.entur.antu.commondata.CommonDataResource;
import org.redisson.api.RLocalCachedMap;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class CommonDataConfig {

    @Bean
    @Profile("!test")
    public CommonDataResource commonDataResource() {
        return new CommonDataResource();
    }

    @Bean
    @Profile("!test")
    public CommonDataRepository commonDataRepository(CommonDataResource commonDataResource,
                                                     @Qualifier("stopPlaceIdPerScheduledStopPointsCache")
                                                     RLocalCachedMap<String, String> stopPlaceIdPerScheduledStopPointsCache) {
        return new CommonDataRepository(commonDataResource, stopPlaceIdPerScheduledStopPointsCache);
    }
}
