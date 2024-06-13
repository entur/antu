package no.entur.antu.config;

import static no.entur.antu.config.cache.CacheConfig.LINE_INFO_CACHE;
import static no.entur.antu.config.cache.CacheConfig.SCHEDULED_STOP_POINT_AND_QUAY_ID_CACHE;
import static no.entur.antu.config.cache.CacheConfig.SERVICE_JOURNEY_TO_SCHEDULED_STOP_POINTS_CACHE;
import static no.entur.antu.config.cache.CacheConfig.SERVICE_LINKS_AND_SCHEDULED_STOP_POINT_IDS_CACHE;

import java.util.List;
import java.util.Map;
import no.entur.antu.commondata.CommonDataRepository;
import no.entur.antu.commondata.CommonDataResource;
import no.entur.antu.commondata.DefaultCommonDataRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class CommonDataConfig {

  @Bean
  @Profile("!test")
  CommonDataResource commonDataResource() {
    return new CommonDataResource();
  }

  @Bean
  @Profile("!test")
  CommonDataRepository commonDataRepository(
    CommonDataResource commonDataResource,
    @Qualifier(
      SCHEDULED_STOP_POINT_AND_QUAY_ID_CACHE
    ) Map<String, Map<String, String>> scheduledStopPointAndQuayIdCache,
    @Qualifier(
      SERVICE_LINKS_AND_SCHEDULED_STOP_POINT_IDS_CACHE
    ) Map<String, Map<String, String>> serviceLinksAndScheduledStopPointIdsCache,
    @Qualifier(LINE_INFO_CACHE) Map<String, List<String>> lineInfoCache,
    @Qualifier(
      SERVICE_JOURNEY_TO_SCHEDULED_STOP_POINTS_CACHE
    ) Map<String, Map<String, List<String>>> serviceJourneyToScheduledStopPointsCache
  ) {
    return new DefaultCommonDataRepository(
      commonDataResource,
      scheduledStopPointAndQuayIdCache,
      serviceLinksAndScheduledStopPointIdsCache,
      lineInfoCache,
      serviceJourneyToScheduledStopPointsCache
    );
  }
}
