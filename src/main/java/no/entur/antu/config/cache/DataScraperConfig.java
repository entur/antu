package no.entur.antu.config.cache;

import static no.entur.antu.config.cache.CacheConfig.LINE_INFO_CACHE;

import java.util.List;
import java.util.Map;
import no.entur.antu.commondata.LineInfoScraper;
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
}
