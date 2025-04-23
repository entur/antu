package no.entur.antu.config;

import static no.entur.antu.config.cache.CacheConfig.*;

import java.util.Map;
import no.entur.antu.netexdata.RedisCommonDataRepository;
import org.entur.netex.validation.validator.jaxb.CommonDataRepositoryLoader;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class CommonDataConfig {

  @Bean
  @Profile("!test")
  CommonDataRepositoryLoader commonDataRepository(
    RedissonClient redissonClient,
    @Qualifier(
      SCHEDULED_STOP_POINT_AND_QUAY_ID_CACHE
    ) Map<String, Map<String, String>> scheduledStopPointAndQuayIdCache,
    @Qualifier(
      SERVICE_LINKS_AND_SCHEDULED_STOP_POINT_IDS_CACHE
    ) Map<String, Map<String, String>> serviceLinksAndFromToScheduledStopPointIdCache,
    @Qualifier(
      SCHEDULED_STOP_POINT_REF_TO_FLEXIBLE_STOP_POINT_REF_CACHE
    ) Map<String, Map<String, String>> scheduledStopPointRefToFlexibleStopPointRefCache
  ) {
    return new RedisCommonDataRepository(
      redissonClient,
      scheduledStopPointAndQuayIdCache,
      serviceLinksAndFromToScheduledStopPointIdCache,
      scheduledStopPointRefToFlexibleStopPointRefCache
    );
  }
}
