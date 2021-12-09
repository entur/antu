package no.entur.antu.config;

import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {

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
}
