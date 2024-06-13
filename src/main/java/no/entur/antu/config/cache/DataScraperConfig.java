package no.entur.antu.config.cache;

import static no.entur.antu.config.cache.CacheConfig.LINE_INFO_CACHE;
import static no.entur.antu.config.cache.CacheConfig.SERVICE_JOURNEY_TO_SCHEDULED_STOP_POINTS_CACHE;

import java.util.List;
import java.util.Map;
import no.entur.antu.commondata.LineInfoScraper;
import no.entur.antu.commondata.ScheduledStopPointsScraper;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataScraperConfig {

  @Bean
  public LineInfoScraper lineInfoScraper(
    RedissonClient redissonClient,
    @Qualifier(LINE_INFO_CACHE) Map<String, List<String>> lineInfoCache
  ) {
    return new LineInfoScraper(redissonClient, lineInfoCache);
  }

  @Bean
  public ScheduledStopPointsScraper scheduledStopPointsScraper(
    @Qualifier(
      SERVICE_JOURNEY_TO_SCHEDULED_STOP_POINTS_CACHE
    ) Map<String, Map<String, List<String>>> serviceJourneyToScheduledStopPointsCache
  ) {
    return new ScheduledStopPointsScraper(
      serviceJourneyToScheduledStopPointsCache
    );
  }
}
