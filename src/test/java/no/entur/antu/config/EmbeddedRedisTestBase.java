package no.entur.antu.config;

import java.io.IOException;
import java.net.ServerSocket;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.Kryo5Codec;
import org.redisson.config.Config;
import redis.embedded.RedisServer;

/**
 * A real Redis for the tests that exercise the coordination primitives.
 *
 * <p>No Spring context: these classes take a RedissonClient and nothing else, and starting an embedded
 * server is quicker than building an application context.
 */
public abstract class EmbeddedRedisTestBase {

  private static RedisServer redisServer;
  protected static RedissonClient redissonClient;

  @BeforeAll
  static void startRedis() throws IOException {
    int port = freePort();
    redisServer = new RedisServer(port);
    redisServer.start();

    Config config = new Config();
    config.setCodec(
      new Kryo5Codec(EmbeddedRedisTestBase.class.getClassLoader())
    );
    config.useSingleServer().setAddress("redis://127.0.0.1:" + port);
    redissonClient = Redisson.create(config);
  }

  @AfterAll
  static void stopRedis() {
    if (redissonClient != null) {
      redissonClient.shutdown();
    }
    if (redisServer != null) {
      redisServer.stop();
    }
  }

  private static int freePort() throws IOException {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    }
  }
}
