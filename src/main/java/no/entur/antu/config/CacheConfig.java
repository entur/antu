package no.entur.antu.config;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import no.entur.antu.cache.CacheAdmin;
import no.entur.antu.cache.RedissonCacheAdmin;
import no.entur.antu.codec.QuayCoordinatesCodec;
import no.entur.antu.codec.QuayIdCodec;
import no.entur.antu.codec.TransportModesCodec;
import no.entur.antu.model.QuayCoordinates;
import no.entur.antu.model.QuayId;
import no.entur.antu.model.TransportModes;
import no.entur.antu.validation.validator.id.RedisNetexIdRepository;
import org.entur.netex.validation.validator.id.NetexIdRepository;
import org.redisson.Redisson;
import org.redisson.api.LocalCachedMapOptions;
import org.redisson.api.RLocalCachedMap;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;
import org.redisson.client.codec.StringCodec;
import org.redisson.codec.CompositeCodec;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.codec.Kryo5Codec;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class CacheConfig {

  public static final String ORGANISATION_CACHE = "organisationCache";
  public static final String STOP_PLACE_AND_QUAY_CACHE =
    "stopPlaceAndQuayCache";
  public static final String TRANSPORT_MODES_FOR_QUAY_ID_CACHE =
    "transportModesForQuayIdCache";
  public static final String COORDINATES_PER_QUAY_ID_CACHE =
    "coordinatesPerQuayIdCache";
  public static final String STOP_PLACE_NAME_PER_QUAY_ID_CACHE =
    "stopPlaceNamePerQuayIdCache";
  public static final String COMMON_IDS_CACHE = "commonIdsCache";
  public static final String SCHEDULED_STOP_POINT_AND_QUAY_ID_CACHE =
    "scheduledStopPointAndQuayIdCache";
  public static final String QUAY_ID_NOT_FOUND_CACHE = "quayIdNotFoundCache";
  private static final Logger LOGGER = LoggerFactory.getLogger(
    CacheConfig.class
  );

  @Bean
  public Config redissonConfig(
    RedisProperties redisProperties,
    @Value("${antu.redis.server.trust.store.file:}") String trustStoreFile,
    @Value(
      "${antu.redis.server.trust.store.password:}"
    ) String trustStorePassword,
    @Value("${antu.redis.authentication.string:}") String authenticationString
  ) throws MalformedURLException {
    Config redissonConfig = new Config();

    Codec codec = new Kryo5Codec(this.getClass().getClassLoader());
    redissonConfig.setCodec(codec);

    if (trustStoreFile.isEmpty()) {
      LOGGER.info("Configuring non-encrypted Redis connection");
      String address = String.format(
        "redis://%s:%s",
        redisProperties.getHost(),
        redisProperties.getPort()
      );
      redissonConfig.useSingleServer().setAddress(address);
      return redissonConfig;
    } else {
      LOGGER.info("Configuring encrypted Redis connection");
      String address = String.format(
        "rediss://%s:%s",
        redisProperties.getHost(),
        redisProperties.getPort()
      );
      redissonConfig
        .useSingleServer()
        .setAddress(address)
        .setSslTruststore(new File(trustStoreFile).toURI().toURL())
        .setSslTruststorePassword(trustStorePassword)
        .setPassword(authenticationString);
      return redissonConfig;
    }
  }

  @Bean(destroyMethod = "shutdown")
  @Profile("!test")
  public RedissonClient redissonClient(Config redissonConfig) {
    return Redisson.create(redissonConfig);
  }

  @Bean(name = TRANSPORT_MODES_FOR_QUAY_ID_CACHE)
  public Map<QuayId, TransportModes> transportModesForQuayIdCache(
    RedissonClient redissonClient
  ) {
    return redissonClient.getLocalCachedMap(
      TRANSPORT_MODES_FOR_QUAY_ID_CACHE,
      new CompositeCodec(new QuayIdCodec(), new TransportModesCodec()),
      LocalCachedMapOptions.defaults()
    );
  }

  @Bean(name = SCHEDULED_STOP_POINT_AND_QUAY_ID_CACHE)
  public Map<String, Map<String, String>> scheduledStopPointAndQuayIdCache(
    RedissonClient redissonClient
  ) {
    return redissonClient.getLocalCachedMap(
      SCHEDULED_STOP_POINT_AND_QUAY_ID_CACHE,
      new CompositeCodec(new StringCodec(), new JsonJacksonCodec()),
      LocalCachedMapOptions.defaults()
    );
  }

  @Bean(name = STOP_PLACE_NAME_PER_QUAY_ID_CACHE)
  public Map<QuayId, String> stopPlaceNamePerQuayIdCache(
    RedissonClient redissonClient
  ) {
    return redissonClient.getLocalCachedMap(
      STOP_PLACE_NAME_PER_QUAY_ID_CACHE,
      new CompositeCodec(new QuayIdCodec(), new StringCodec()),
      LocalCachedMapOptions.defaults()
    );
  }

  @Bean(name = COORDINATES_PER_QUAY_ID_CACHE)
  public Map<QuayId, QuayCoordinates> coordinatesPerQuayIdCache(
    RedissonClient redissonClient
  ) {
    return redissonClient.getLocalCachedMap(
      COORDINATES_PER_QUAY_ID_CACHE,
      new CompositeCodec(new QuayIdCodec(), new QuayCoordinatesCodec()),
      LocalCachedMapOptions.defaults()
    );
  }

  @Bean(name = STOP_PLACE_AND_QUAY_CACHE)
  public Map<String, Set<String>> stopPlaceAndQuayCache(
    RedissonClient redissonClient
  ) {
    return redissonClient.getLocalCachedMap(
      STOP_PLACE_AND_QUAY_CACHE,
      LocalCachedMapOptions.defaults()
    );
  }

  @Bean
  public Map<String, Set<String>> organisationCache(
    RedissonClient redissonClient
  ) {
    return redissonClient.getLocalCachedMap(
      ORGANISATION_CACHE,
      LocalCachedMapOptions.defaults()
    );
  }

  @Bean(name = QUAY_ID_NOT_FOUND_CACHE)
  public Set<QuayId> quayIdNotFoundCache(RedissonClient redissonClient) {
    return redissonClient.getSet(QUAY_ID_NOT_FOUND_CACHE);
  }

  @Bean
  public RLocalCachedMap<String, Set<String>> commonIdsCache(
    RedissonClient redissonClient
  ) {
    LocalCachedMapOptions<String, Set<String>> localCacheOptions =
      LocalCachedMapOptions.defaults();
    localCacheOptions.timeToLive(1, TimeUnit.HOURS);
    return redissonClient.getLocalCachedMap(
      COMMON_IDS_CACHE,
      localCacheOptions
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
}
