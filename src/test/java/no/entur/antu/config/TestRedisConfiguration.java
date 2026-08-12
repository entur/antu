package no.entur.antu.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import redis.embedded.RedisServer;

@Configuration
public class TestRedisConfiguration {

  /**
   * One server for the whole JVM, on the fixed port from the test properties.
   *
   * <p>Spring caches contexts, so several of them are alive at once and each would otherwise try to
   * bind the same port. The server outlives every context and is reclaimed when the JVM exits.
   */
  private static RedisServer sharedServer;

  @Bean(destroyMethod = "shutdown")
  @DependsOn("redissonServer")
  public RedissonClient redissonClient(Config redissonConfig) {
    return Redisson.create(redissonConfig);
  }

  @Bean
  public synchronized RedisServer redissonServer(
    DataRedisProperties redisProperties
  ) {
    if (sharedServer == null) {
      RedisServer server = new RedisServer(redisProperties.getPort());
      server.start();
      sharedServer = server;
    }
    return sharedServer;
  }
}
