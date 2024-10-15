package no.entur.antu.config.cache;

import static no.entur.antu.config.cache.CacheConfig.LINE_INFO_CACHE;
import static no.entur.antu.config.cache.CacheConfig.SERVICE_JOURNEY_INTERCHANGE_INFO_CACHE;
import static no.entur.antu.config.cache.CacheConfig.SERVICE_JOURNEY_STOPS_CACHE;

import java.util.List;
import java.util.Map;
import no.entur.antu.netexdata.collectors.LineInfoCollector;
import no.entur.antu.netexdata.collectors.ServiceJourneyInterchangeInfoCollector;
import no.entur.antu.netexdata.collectors.ServiceJourneyStopsCollector;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NetexDataCollectorConfig {

  @Bean
  public LineInfoCollector lineInfoScraper(
    RedissonClient redissonClient,
    @Qualifier(LINE_INFO_CACHE) Map<String, List<String>> lineInfoCache
  ) {
    return new LineInfoCollector(redissonClient, lineInfoCache);
  }

  @Bean
  public ServiceJourneyInterchangeInfoCollector serviceJourneyInterchangeInfoCollector(
    RedissonClient redissonClient,
    @Qualifier(
      SERVICE_JOURNEY_INTERCHANGE_INFO_CACHE
    ) Map<String, List<String>> serviceJourneyInterchangeInfoCache
  ) {
    return new ServiceJourneyInterchangeInfoCollector(
      redissonClient,
      serviceJourneyInterchangeInfoCache
    );
  }

  @Bean
  public ServiceJourneyStopsCollector serviceJourneyStopsCollector(
    RedissonClient redissonClient,
    @Qualifier(
      SERVICE_JOURNEY_STOPS_CACHE
    ) Map<String, Map<String, List<String>>> serviceJourneyStopsCache
  ) {
    return new ServiceJourneyStopsCollector(
      redissonClient,
      serviceJourneyStopsCache
    );
  }
}
