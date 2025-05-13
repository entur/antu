package no.entur.antu.netexdata;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.redisson.api.RedissonClient;

public class RedisNetexDataRepository extends DefaultNetexDataRepository {

  private final RedissonClient redissonClient;

  public RedisNetexDataRepository(
    RedissonClient redissonClient,
    Map<String, List<String>> lineInfoCache,
    Map<String, Map<String, List<String>>> serviceJourneyStopsCache,
    Map<String, List<String>> serviceJourneyInterchangeInfoCache,
    Map<String, Map<String, List<LocalDateTime>>> activeDatesByServiceJourneyIdCache
  ) {
    super(
      lineInfoCache,
      serviceJourneyStopsCache,
      serviceJourneyInterchangeInfoCache,
      activeDatesByServiceJourneyIdCache
    );
    this.redissonClient = redissonClient;
  }

  @Override
  public void cleanUp(String validationReportId) {
    redissonClient.getKeys().deleteByPattern(validationReportId + '*');
    super.cleanUp(validationReportId);
  }
}
