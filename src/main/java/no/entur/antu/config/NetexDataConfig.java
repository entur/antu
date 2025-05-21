package no.entur.antu.config;

import static no.entur.antu.config.cache.CacheConfig.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import no.entur.antu.netexdata.RedisNetexDataRepository;
import org.entur.netex.validation.validator.jaxb.NetexDataRepository;
import org.entur.netex.validation.validator.model.ServiceJourneyId;
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
    ) Map<String, Map<String, List<String>>> serviceJourneyStopsCache,
    @Qualifier(
      SERVICE_JOURNEY_INTERCHANGE_INFO_CACHE
    ) Map<String, List<String>> serviceJourneyInterchangeInfoCache,
    @Qualifier(
      ACTIVE_DATES_BY_SERVICE_JOURNEY_REF
    ) Map<String, Map<ServiceJourneyId, List<LocalDateTime>>> activeDatesByServiceJourneyRefCache
  ) {
    return new RedisNetexDataRepository(
      redissonClient,
      lineInfoCache,
      serviceJourneyStopsCache,
      serviceJourneyInterchangeInfoCache,
      activeDatesByServiceJourneyRefCache
    );
  }
}
