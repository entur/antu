package no.entur.antu.config;

import no.entur.antu.cache.CacheAdmin;
import no.entur.antu.cache.RedissonCacheAdmin;
import no.entur.antu.validator.id.RedisNetexIdRepository;
import org.entur.netex.validation.validator.id.NetexIdRepository;
import org.redisson.Redisson;
import org.redisson.api.LocalCachedMapOptions;
import org.redisson.api.RLocalCachedMap;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;
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

import java.io.File;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    public static final String ORGANISATION_CACHE = "organisationCache";
    public static final String STOP_PLACE_AND_QUAY_CACHE = "stopPlaceAndQuayCache";
    public static final String COMMON_IDS_CACHE = "commonIdsCache";

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheConfig.class);


    @Bean
    public Config redissonConfig(RedisProperties redisProperties,
                                 @Value("${antu.redis.server.trust.store.file:}") String trustStoreFile,
                                 @Value("${antu.redis.server.trust.store.password:}") String trustStorePassword,
                                 @Value("${antu.redis.authentication.string:}") String authenticationString ) throws MalformedURLException {
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
            redissonConfig.useSingleServer()
                    .setAddress(address);
            return redissonConfig;
        } else {
            LOGGER.info("Configuring encrypted Redis connection");
            String address = String.format(
                    "rediss://%s:%s",
                    redisProperties.getHost(),
                    redisProperties.getPort()
            );
            redissonConfig.useSingleServer()
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


    @Bean
    public Map<String, Set<String>> stopPlaceAndQuayCache(RedissonClient redissonClient) {
        return redissonClient.getLocalCachedMap(STOP_PLACE_AND_QUAY_CACHE, LocalCachedMapOptions.defaults());
    }

    @Bean
    public Map<String, Set<String>> organisationCache(RedissonClient redissonClient) {
        return redissonClient.getLocalCachedMap(ORGANISATION_CACHE, LocalCachedMapOptions.defaults());
    }

    @Bean
    public RLocalCachedMap<String, Set<String>> commonIdsCache(RedissonClient redissonClient) {
        LocalCachedMapOptions<String, Set<String>> localCacheOptions = LocalCachedMapOptions.defaults();
        localCacheOptions.timeToLive(1, TimeUnit.HOURS);
        return redissonClient.getLocalCachedMap(COMMON_IDS_CACHE, localCacheOptions);
    }

    @Bean
    public NetexIdRepository netexIdRepository(RedissonClient redissonClient, @Qualifier("commonIdsCache") RLocalCachedMap<String, Set<String>> commonIdsCache) {
        return new RedisNetexIdRepository(redissonClient, commonIdsCache);
    }

    @Bean
    public CacheAdmin cacheAdmin(RedissonClient redissonClient) {
        return new RedissonCacheAdmin(redissonClient);
    }

}
