package no.entur.antu.config;

import java.io.File;
import java.net.MalformedURLException;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;
import org.redisson.codec.Kryo5Codec;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class RedisClientConfig {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    RedisClientConfig.class
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
        .setNettyThreads(64)
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
}
