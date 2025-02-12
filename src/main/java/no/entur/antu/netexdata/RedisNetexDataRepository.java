package no.entur.antu.netexdata;

import java.util.List;
import java.util.Map;
import org.redisson.api.RedissonClient;

public class RedisNetexDataRepository extends DefaultNetexDataRepository {

  private final RedissonClient redissonClient;

  public RedisNetexDataRepository(
    RedissonClient redissonClient,
    Map<String, List<String>> lineInfoCache,
    Map<String, Map<String, List<String>>> serviceJourneyStopsCache,
    Map<String, List<String>> serviceJourneyInterchangeInfoCache
  ) {
    super(
      lineInfoCache,
      serviceJourneyStopsCache,
      serviceJourneyInterchangeInfoCache
    );
    this.redissonClient = redissonClient;
  }

  @Override
  public void cleanUp(String validationReportId) {
    redissonClient.getKeys().deleteByPattern(validationReportId + '*');
    super.cleanUp(validationReportId);
  }
}
