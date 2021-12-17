package no.entur.antu.config;

import no.entur.antu.validator.id.NetexIdRepository;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;
import org.redisson.codec.Kryo5Codec;
import org.redisson.config.Config;
import org.redisson.jcache.configuration.RedissonConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.cache.Cache;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import java.util.Set;

@Configuration
public class CacheConfig {

    public static final String STOP_PLACE_CACHE_KEY = "stopPlaceCache";
    public static final String ID_VERSION_CACHE_KEY = "idVersionCache";

    @Bean
    public Config redissonConfig(RedisProperties redisProperties) {
        Config redissonConfig = new Config();

        Codec codec = new Kryo5Codec(this.getClass().getClassLoader());
        redissonConfig.setCodec(codec);

        String address = String.format(
                "redis://%s:%s",
                redisProperties.getHost(),
                redisProperties.getPort()
        );
        redissonConfig.useSingleServer()
                .setAddress(address);
        return redissonConfig;
    }

    @Bean(destroyMethod = "shutdown")
    @Profile("!test")
    public RedissonClient redissonClient(Config redissonConfig) {
        return Redisson.create(redissonConfig);
    }

    @Bean
    public Cache<String, Set<String>> stopPlaceCache(Config redissonConfig) {
        MutableConfiguration<String, Set<String>> cacheConfig = new MutableConfiguration<>();
        var redissonCacheConfig = RedissonConfiguration.fromConfig(redissonConfig, cacheConfig);
        var manager = Caching.getCachingProvider().getCacheManager();
        return manager.createCache(STOP_PLACE_CACHE_KEY, redissonCacheConfig);
    }

    @Bean
    public NetexIdRepository netexIdRepository(RedissonClient redissonClient) {
        return new NetexIdRepository(redissonClient);
    }
}
