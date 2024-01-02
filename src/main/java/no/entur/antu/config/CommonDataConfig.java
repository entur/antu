package no.entur.antu.config;

import static no.entur.antu.config.CacheConfig.SCHEDULED_STOP_POINT_AND_QUAY_ID_CACHE;

import java.util.Map;
import no.entur.antu.commondata.CommonDataRepository;
import no.entur.antu.commondata.CommonDataRepositoryImpl;
import no.entur.antu.commondata.CommonDataResource;
import no.entur.antu.model.QuayId;
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
    ) Map<String, Map<String, QuayId>> scheduledStopPointAndQuayIdCache
  ) {
    return new CommonDataRepositoryImpl(
      commonDataResource,
      scheduledStopPointAndQuayIdCache
    );
  }
}
