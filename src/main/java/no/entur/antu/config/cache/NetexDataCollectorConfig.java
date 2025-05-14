package no.entur.antu.config.cache;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import no.entur.antu.netexdata.collectors.LineInfoCollector;
import no.entur.antu.netexdata.collectors.ServiceJourneyActiveDatesCollector;
import no.entur.antu.netexdata.collectors.ServiceJourneyInterchangeInfoCollector;
import no.entur.antu.netexdata.collectors.ServiceJourneyStopsCollector;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static no.entur.antu.config.cache.CacheConfig.*;

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

  @Bean
  public ServiceJourneyActiveDatesCollector serviceJourneyActiveDatesCollector(
          RedissonClient redissonClient,
          @Qualifier(
                  ACTIVE_DATES_BY_DAY_TYPE_REF
          ) Map<String, Map<String, List<LocalDateTime>>> dayTypeActiveDatesCache,
          @Qualifier(
                  ACTIVE_DATES_BY_SERVICE_JOURNEY_REF
          ) Map<String, Map<String, List<LocalDateTime>>> serviceJourneyActiveDatesCache,
          @Qualifier(
                  ACTIVE_DATE_BY_OPERATING_DAY_REF
          ) Map<String, Map<String, LocalDateTime>> operatingDayActiveDateCache
  ) {
    return new ServiceJourneyActiveDatesCollector(
            redissonClient,
            dayTypeActiveDatesCache,
            serviceJourneyActiveDatesCache,
            operatingDayActiveDateCache
    );
  }
}
