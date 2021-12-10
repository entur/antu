package no.entur.antu.config;

import org.redisson.config.Config;
import org.redisson.jcache.configuration.RedissonConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.cache.Cache;
import javax.cache.Caching;
import javax.cache.configuration.Factory;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.expiry.ExpiryPolicy;
import java.util.Set;

@Configuration
public class CacheConfig {

    public static final String STOP_PLACE_CACHE_KEY = "stopPlaceCache";

    @Bean
    public Config redissonConfig(RedisProperties redisProperties) {
        Config redissonConfig = new Config();
        String address = String.format(
                "redis://%s:%s",
                redisProperties.getHost(),
                redisProperties.getPort()
        );
        redissonConfig.useSingleServer()
                .setAddress(address);
        return redissonConfig;
    }

    @Bean
    public Cache<String, Set<String>> stopPlaceCache(Config redissonConfig) {
        MutableConfiguration<String, Set<String>> cacheConfig = new MutableConfiguration<>();
        cacheConfig.setExpiryPolicyFactory((Factory<ExpiryPolicy>) () -> new CreatedExpiryPolicy(Duration.ONE_DAY));
        var redissonCacheConfig = RedissonConfiguration.fromConfig(redissonConfig, cacheConfig);
        var manager = Caching.getCachingProvider().getCacheManager();
        return manager.createCache(STOP_PLACE_CACHE_KEY, redissonCacheConfig);
    }
}
