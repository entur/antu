package no.entur.antu.config.cache;

import static no.entur.antu.stop.DefaultStopPlaceRepository.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import no.entur.antu.cache.CacheAdmin;
import no.entur.antu.cache.RedissonCacheAdmin;
import no.entur.antu.cache.codec.QuayIdCodec;
import no.entur.antu.cache.codec.StopPlaceIdCodec;
import no.entur.antu.validation.validator.id.RedisNetexIdRepository;
import org.entur.netex.validation.validator.id.NetexIdRepository;
import org.entur.netex.validation.validator.model.QuayId;
import org.entur.netex.validation.validator.model.SimpleQuay;
import org.entur.netex.validation.validator.model.SimpleStopPlace;
import org.entur.netex.validation.validator.model.StopPlaceId;
import org.redisson.api.RLocalCachedMap;
import org.redisson.api.RedissonClient;
import org.redisson.api.options.LocalCachedMapOptions;
import org.redisson.client.codec.Codec;
import org.redisson.client.codec.StringCodec;
import org.redisson.codec.CompositeCodec;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.codec.Kryo5Codec;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {

  public static final String ORGANISATION_CACHE = "organisationCache";
  public static final String COMMON_IDS_CACHE = "commonIdsCache";
  public static final String SCHEDULED_STOP_POINT_AND_QUAY_ID_CACHE =
    "scheduledStopPointAndQuayIdCache";
  public static final String SERVICE_LINKS_AND_SCHEDULED_STOP_POINT_IDS_CACHE =
    "serviceLinksAndFromToScheduledStopPointIdCache";
  public static final String LINE_INFO_CACHE = "linesInfoCache";
  public static final String SERVICE_JOURNEY_STOPS_CACHE =
    "serviceJourneyStopsCache";
  public static final String QUAY_ID_NOT_FOUND_CACHE = "quayIdNotFoundCache";

  private static final Kryo5Codec DEFAULT_CODEC = new Kryo5Codec();

  /**
   * Distributed cache for StopPlaces.
   * The cache is refreshed  periodically by reading a new NeTEx stop dataset.
   */
  @Bean(name = STOP_PLACE_CACHE)
  public Map<StopPlaceId, SimpleStopPlace> stopPlaceCache(
    RedissonClient redissonClient
  ) {
    return getOrCreateApplicationScopedCache(
      redissonClient,
      STOP_PLACE_CACHE,
      new CompositeCodec(new StopPlaceIdCodec(), DEFAULT_CODEC)
    );
  }

  /**
   * Distributed cache for Quays.
   * The cache is refreshed  periodically by reading a new NeTEx stop dataset.
   */
  @Bean(name = QUAY_CACHE)
  public Map<QuayId, SimpleQuay> quayCache(RedissonClient redissonClient) {
    return getOrCreateApplicationScopedCache(
      redissonClient,
      QUAY_CACHE,
      new CompositeCodec(new QuayIdCodec(), DEFAULT_CODEC)
    );
  }

  /**
   * Maps an organisation codespace to the list of Authorities it can refer to in a NeTEx dataset.
   * The cache is refreshed  periodically by querying the organisation register.
   */
  @Bean
  public Map<String, Set<String>> organisationCache(
    RedissonClient redissonClient
  ) {
    return getOrCreateApplicationScopedCache(
      redissonClient,
      ORGANISATION_CACHE,
      DEFAULT_CODEC
    );
  }

  /**
   * Keep track of quays not found when querying the stop place REST API.
   */
  @Bean(name = QUAY_ID_NOT_FOUND_CACHE)
  public Set<QuayId> quayIdNotFoundCache(RedissonClient redissonClient) {
    return redissonClient.getSet(QUAY_ID_NOT_FOUND_CACHE);
  }

  /**
   * The set of NeTEx ids referenced in a NeTEx dataset.
   * The cache is report-scoped.
   * The cache key is the current validation report.
   */
  @Bean
  public RLocalCachedMap<String, Set<String>> commonIdsCache(
    RedissonClient redissonClient
  ) {
    return getOrCreateReportScopedCache(
      redissonClient,
      COMMON_IDS_CACHE,
      DEFAULT_CODEC
    );
  }

  /**
   * The mapping between schedule stop points and quays.
   * The cache is report-scoped.
   * The cache key is the current validation report.
   */
  @Bean(name = SCHEDULED_STOP_POINT_AND_QUAY_ID_CACHE)
  public Map<String, Map<String, String>> scheduledStopPointAndQuayIdCache(
    RedissonClient redissonClient
  ) {
    return getOrCreateReportScopedCache(
      redissonClient,
      SCHEDULED_STOP_POINT_AND_QUAY_ID_CACHE,
      new CompositeCodec(new StringCodec(), new JsonJacksonCodec())
    );
  }

  /**
   * The mapping between service links and schedule stop points.
   * The cache is report-scoped.
   * The cache key is the current validation report.
   */
  @Bean(name = SERVICE_LINKS_AND_SCHEDULED_STOP_POINT_IDS_CACHE)
  public Map<String, Map<String, String>> serviceLinksAndFromToScheduledStopPointIdCache(
    RedissonClient redissonClient
  ) {
    return getOrCreateReportScopedCache(
      redissonClient,
      SERVICE_LINKS_AND_SCHEDULED_STOP_POINT_IDS_CACHE,
      new CompositeCodec(new StringCodec(), new JsonJacksonCodec())
    );
  }

  /**
   * The list of line id/line name present in the current dataset.
   * The cache is report-scoped.
   * The cache key is the current validation report.
   */
  @Bean(name = LINE_INFO_CACHE)
  public Map<String, List<String>> lineNamesCache(
    RedissonClient redissonClient
  ) {
    return getOrCreateReportScopedCache(
      redissonClient,
      LINE_INFO_CACHE,
      new CompositeCodec(new StringCodec(), new JsonJacksonCodec())
    );
  }

  @Bean(name = SERVICE_JOURNEY_STOPS_CACHE)
  public Map<String, Map<String, List<String>>> serviceJourneyStopsCache(
    RedissonClient redissonClient
  ) {
    return getOrCreateReportScopedCache(
      redissonClient,
      SERVICE_JOURNEY_STOPS_CACHE,
      new CompositeCodec(new StringCodec(), new JsonJacksonCodec())
    );
  }

  @Bean
  public NetexIdRepository netexIdRepository(
    RedissonClient redissonClient,
    @Qualifier(
      "commonIdsCache"
    ) RLocalCachedMap<String, Set<String>> commonIdsCache
  ) {
    return new RedisNetexIdRepository(redissonClient, commonIdsCache);
  }

  @Bean
  public CacheAdmin cacheAdmin(RedissonClient redissonClient) {
    return new RedissonCacheAdmin(redissonClient);
  }

  private static <K, V> RLocalCachedMap<K, V> getOrCreateReportScopedCache(
    RedissonClient redissonClient,
    String cacheKey,
    Codec codec
  ) {
    LocalCachedMapOptions<K, V> options = LocalCachedMapOptions
      .<K, V>name(cacheKey)
      .codec(codec)
      .timeToLive(Duration.ofHours(1));
    return redissonClient.getLocalCachedMap(options);
  }

  private static <K, V> RLocalCachedMap<K, V> getOrCreateApplicationScopedCache(
    RedissonClient redissonClient,
    String cacheKey,
    Codec codec
  ) {
    LocalCachedMapOptions<K, V> options = LocalCachedMapOptions
      .<K, V>name(cacheKey)
      .codec(codec);
    return redissonClient.getLocalCachedMap(options);
  }
}
