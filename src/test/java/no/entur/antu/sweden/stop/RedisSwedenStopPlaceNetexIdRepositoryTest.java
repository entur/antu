package no.entur.antu.sweden.stop;

import static no.entur.antu.config.cache.CacheConfig.VALIDATION_DATA_TTL;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import redis.embedded.RedisServer;

class RedisSwedenStopPlaceNetexIdRepositoryTest {

  private static final String TEST_REPORT_ID = "test-report-123";
  private static final int REDIS_PORT = 6372;

  private static RedisServer redisServer;
  private static RedissonClient redissonClient;

  private RedisSwedenStopPlaceNetexIdRepository repository;

  @BeforeAll
  static void startRedis() throws Exception {
    redisServer = new RedisServer(REDIS_PORT);
    redisServer.start();

    Config config = new Config();
    config.useSingleServer().setAddress("redis://127.0.0.1:" + REDIS_PORT);
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

  @BeforeEach
  void setUp() {
    repository = new RedisSwedenStopPlaceNetexIdRepository(redissonClient);
  }

  @AfterEach
  void tearDown() {
    redissonClient.getKeys().flushdb();
  }

  @Test
  void testSharedIdsRoundTrip() {
    Set<String> ids = Set.of("SE:StopPlace:1", "SE:Quay:1");
    repository.addSharedStopPlaceAndQuayIds(TEST_REPORT_ID, ids);

    Set<String> sharedIds = repository.getSharedStopPlaceAndQuayIds(
      TEST_REPORT_ID
    );

    assertEquals(ids, sharedIds);
  }

  /**
   * Per-report keys must carry a TTL so that validations that never reach
   * cleanUp do not leak keys.
   */
  @Test
  void testPerReportKeysHaveTtl() {
    repository.addSharedStopPlaceAndQuayIds(
      TEST_REPORT_ID,
      Set.of("SE:StopPlace:1")
    );

    assertKeyHasTtl("SHARED_STOP_PLACE_ID_SET_" + TEST_REPORT_ID);
    assertKeyHasTtl("SWEDEN_STOP_PLACE_SET_SEMAPHORE_" + TEST_REPORT_ID);
  }

  private static void assertKeyHasTtl(String key) {
    long ttl = redissonClient.getKeys().remainTimeToLive(key);
    assertTrue(
      ttl > 0 && ttl <= VALIDATION_DATA_TTL.toMillis(),
      "Key " +
      key +
      " remainTimeToLive=" +
      ttl +
      ", expected in (0, " +
      VALIDATION_DATA_TTL.toMillis() +
      "]"
    );
  }
}
