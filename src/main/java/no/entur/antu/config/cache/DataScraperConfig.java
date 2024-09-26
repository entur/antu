package no.entur.antu.config.cache;

import static no.entur.antu.config.cache.CacheConfig.LINE_INFO_CACHE;
import static no.entur.antu.config.cache.CacheConfig.SERVICE_JOURNEY_STOPS_CACHE;

import java.util.List;
import java.util.Map;
import no.entur.antu.commondata.scraper.LineInfoCollector;
import no.entur.antu.commondata.scraper.ServiceJourneyStopsCollector;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataScraperConfig {

  @Bean
  public LineInfoCollector lineInfoScraper(
    RedissonClient redissonClient,
    @Qualifier(LINE_INFO_CACHE) Map<String, List<String>> lineInfoCache
  ) {
    return new LineInfoCollector(redissonClient, lineInfoCache);
  }

  @Bean
  public ServiceJourneyStopsCollector serviceJourneyStopsCollector(
    @Qualifier(
      SERVICE_JOURNEY_STOPS_CACHE
    ) Map<String, Map<String, List<String>>> serviceJourneyStopsCache
  ) {
    return new ServiceJourneyStopsCollector(serviceJourneyStopsCache);
  }
}
