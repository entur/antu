package no.entur.antu.config;

import static no.entur.antu.config.cache.CacheConfig.LINE_INFO_CACHE;
import static no.entur.antu.config.cache.CacheConfig.SERVICE_JOURNEY_INTERCHANGE_INFO_CACHE;
import static no.entur.antu.config.cache.CacheConfig.SERVICE_JOURNEY_STOPS_CACHE;

import java.util.List;
import java.util.Map;
import no.entur.antu.netexdata.RedisNetexDataRepository;
import org.entur.netex.validation.validator.jaxb.NetexDataRepository;
import org.entur.netex.validation.validator.model.ServiceJourneyStop;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class NetexDataConfig {

  @Bean
  @Profile("!test")
  NetexDataRepository netexDataRepository(
    RedissonClient redissonClient,
    @Qualifier(LINE_INFO_CACHE) Map<String, List<String>> lineInfoCache,
    @Qualifier(
      SERVICE_JOURNEY_STOPS_CACHE
    ) Map<String, Map<String, List<ServiceJourneyStop>>> serviceJourneyStopsCache,
    @Qualifier(
      SERVICE_JOURNEY_INTERCHANGE_INFO_CACHE
    ) Map<String, List<String>> serviceJourneyInterchangeInfoCache
  ) {
    return new RedisNetexDataRepository(
      redissonClient,
      lineInfoCache,
      serviceJourneyStopsCache,
      serviceJourneyInterchangeInfoCache
    );
  }
}
