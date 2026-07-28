/*
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *   https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 *
 */

package no.entur.antu.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.redisson.config.SingleServerConfig;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;

class RedisClientConfigTest {

  @Test
  void encryptedRedisConnection(@TempDir Path tempDir) throws Exception {
    Path trustStore = Files.createFile(tempDir.resolve("redis-truststore.jks"));

    DataRedisProperties redisProperties = new DataRedisProperties();
    redisProperties.setHost("redis.example.org");
    redisProperties.setPort(6379);

    SingleServerConfig singleServerConfig = new RedisClientConfig()
      .redissonConfig(
        redisProperties,
        trustStore.toString(),
        "trustStorePassword",
        "authenticationString"
      )
      .useSingleServer();

    assertEquals(
      "rediss://redis.example.org:6379",
      singleServerConfig.getAddress()
    );
    assertEquals(
      trustStore.toUri().toURL().toString(),
      singleServerConfig.getSslTruststore().toString()
    );
    assertEquals(
      "trustStorePassword",
      singleServerConfig.getSslTruststorePassword()
    );
    assertEquals("authenticationString", singleServerConfig.getPassword());
  }
}
