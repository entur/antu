package no.entur.antu.netexdata;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.entur.netex.validation.validator.model.ServiceJourneyId;
import org.entur.netex.validation.validator.model.ServiceJourneyStop;
import org.redisson.api.RedissonClient;

public class RedisNetexDataRepository extends DefaultNetexDataRepository {

  private final RedissonClient redissonClient;

  public RedisNetexDataRepository(
    RedissonClient redissonClient,
    Map<String, List<String>> lineInfoCache,
    Map<String, Map<String, List<ServiceJourneyStop>>> serviceJourneyStopsCache,
    Map<String, List<String>> serviceJourneyInterchangeInfoCache,
    Map<String, Map<ServiceJourneyId, List<LocalDateTime>>> activeDatesByServiceJourneyIdCache,
    Map<String, Map<String, List<LocalDateTime>>> dayTypeActiveDatesCache,
    Map<String, Map<String, LocalDateTime>> operatingDayActiveDateCache
  ) {
    super(
      lineInfoCache,
      serviceJourneyStopsCache,
      serviceJourneyInterchangeInfoCache,
      activeDatesByServiceJourneyIdCache,
      dayTypeActiveDatesCache,
      operatingDayActiveDateCache
    );
    this.redissonClient = redissonClient;
  }

  @Override
  public void cleanUp(String validationReportId) {
    redissonClient.getKeys().deleteByPattern(validationReportId + '*');
    super.cleanUp(validationReportId);
  }
}
