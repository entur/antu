package no.entur.antu.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.net.MalformedURLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.redisson.config.Config;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;

class RedisClientConfigTest {

  private final RedisClientConfig redisClientConfig = new RedisClientConfig();

  @Test
  void nonEncryptedConnectionHasNoSslConfig() throws MalformedURLException {
    DataRedisProperties props = new DataRedisProperties();
    props.setHost("localhost");
    props.setPort(6379);

    Config config = redisClientConfig.redissonConfig(props, "", "", "");

    assertNull(config.getSslTruststore());
    assertNull(config.getPassword());
  }

  @Test
  void encryptedConnectionSetsSslAndPassword(@TempDir File tempDir)
    throws MalformedURLException {
    DataRedisProperties props = new DataRedisProperties();
    props.setHost("localhost");
    props.setPort(6380);

    File trustStoreFile = new File(tempDir, "truststore.jks");

    Config config = redisClientConfig.redissonConfig(
      props,
      trustStoreFile.getAbsolutePath(),
      "test-truststore-password",
      "test-auth-string"
    );

    assertNotNull(config.getSslTruststore());
    assertEquals(trustStoreFile.toURI().toURL(), config.getSslTruststore());
    assertEquals("test-truststore-password", config.getSslTruststorePassword());
    assertEquals("test-auth-string", config.getPassword());
  }
}
