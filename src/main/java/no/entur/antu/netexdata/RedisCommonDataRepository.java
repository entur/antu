package no.entur.antu.netexdata;

import java.util.Map;

import org.entur.netex.validation.validator.jaxb.DefaultCommonDataRepository;
import org.redisson.api.RedissonClient;

public class RedisCommonDataRepository extends DefaultCommonDataRepository {

  private final RedissonClient redissonClient;

  public RedisCommonDataRepository(
    RedissonClient redissonClient,
    Map<String, Map<String, String>> scheduledStopPointAndQuayIdCache,
    Map<String, Map<String, String>> serviceLinksAndFromToScheduledStopPointIdCache,
    Map<String, Map<String, String>> scheduledStopPointRefToFlexibleStopPointRefCache
  ) {
    super(
      scheduledStopPointAndQuayIdCache,
      serviceLinksAndFromToScheduledStopPointIdCache,
      scheduledStopPointRefToFlexibleStopPointRefCache
    );
    this.redissonClient = redissonClient;
  }

  @Override
  public void cleanUp(String validationReportId) {
    redissonClient.getKeys().deleteByPattern(validationReportId + '*');
    super.cleanUp(validationReportId);
  }
}
