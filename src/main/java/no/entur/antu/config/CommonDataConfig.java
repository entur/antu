package no.entur.antu.config;

import static no.entur.antu.config.cache.CacheConfig.LINE_INFO_CACHE;
import static no.entur.antu.config.cache.CacheConfig.SCHEDULED_STOP_POINT_AND_QUAY_ID_CACHE;
import static no.entur.antu.config.cache.CacheConfig.SERVICE_JOURNEY_STOPS_CACHE;
import static no.entur.antu.config.cache.CacheConfig.SERVICE_LINKS_AND_SCHEDULED_STOP_POINT_IDS_CACHE;

import java.util.List;
import java.util.Map;
import no.entur.antu.commondata.DefaultNetexDataRepository;
import no.entur.antu.commondata.NetexDataResource;
import org.entur.netex.validation.validator.jaxb.NetexDataRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class CommonDataConfig {

  @Bean
  @Profile("!test")
  NetexDataResource commonDataResource() {
    return new NetexDataResource();
  }

  @Bean
  @Profile("!test")
  NetexDataRepository netexDataRepository(
    NetexDataResource netexDataResource,
    @Qualifier(
      SCHEDULED_STOP_POINT_AND_QUAY_ID_CACHE
    ) Map<String, Map<String, String>> scheduledStopPointAndQuayIdCache,
    @Qualifier(
      SERVICE_LINKS_AND_SCHEDULED_STOP_POINT_IDS_CACHE
    ) Map<String, Map<String, String>> serviceLinksAndFromToScheduledStopPointIdCache,
    @Qualifier(LINE_INFO_CACHE) Map<String, List<String>> lineInfoCache,
    @Qualifier(
      SERVICE_JOURNEY_STOPS_CACHE
    ) Map<String, Map<String, List<String>>> serviceJourneyStopsCache
  ) {
    return new DefaultNetexDataRepository(
      netexDataResource,
      scheduledStopPointAndQuayIdCache,
      serviceLinksAndFromToScheduledStopPointIdCache,
      lineInfoCache,
      serviceJourneyStopsCache
    );
  }
}
