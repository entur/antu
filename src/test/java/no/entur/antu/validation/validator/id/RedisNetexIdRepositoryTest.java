package no.entur.antu.validation.validator.id;

import static no.entur.antu.config.cache.CacheConfig.VALIDATION_DATA_TTL;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;
import org.entur.netex.validation.validator.id.IdVersion;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RLocalCachedMap;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.api.options.LocalCachedMapOptions;
import org.redisson.codec.Kryo5Codec;
import org.redisson.config.Config;
import redis.embedded.RedisServer;

/**
 * Test for RedisNetexIdRepository to verify duplicate ID detection across multiple files.
 */
class RedisNetexIdRepositoryTest {

  private static final String TEST_REPORT_ID = "test-report-123";
  private static final String CACHE_NAME = "testCommonIdsCache";
  private static final Kryo5Codec CODEC = new Kryo5Codec();
  private static final int REDIS_PORT = 6371;

  private static RedisServer redisServer;
  private static RedissonClient redissonClient;
  private static RedissonClient redissonClient2;

  private RedisNetexIdRepository repository;
  private RedisNetexIdRepository repository2;

  @BeforeAll
  static void startRedis() throws Exception {
    // Start embedded Redis server for testing
    redisServer = new RedisServer(REDIS_PORT);
    redisServer.start();

    // Create Redisson client
    Config config = new Config();
    config.useSingleServer().setAddress("redis://127.0.0.1:" + REDIS_PORT);
    redissonClient = Redisson.create(config);
    redissonClient2 = Redisson.create(config);
  }

  @AfterAll
  static void stopRedis() {
    if (redissonClient != null) {
      redissonClient.shutdown();
    }
    if (redissonClient2 != null) {
      redissonClient2.shutdown();
    }
    if (redisServer != null) {
      redisServer.stop();
    }
  }

  @BeforeEach
  void setUp() {
    RLocalCachedMap<String, Set<String>> commonIdsCache =
      redissonClient.getLocalCachedMap(
        LocalCachedMapOptions.<String, Set<String>>name(CACHE_NAME).codec(CODEC)
      );
    repository = new RedisNetexIdRepository(redissonClient, commonIdsCache);

    RLocalCachedMap<String, Set<String>> commonIdsCache2 =
      redissonClient2.getLocalCachedMap(
        LocalCachedMapOptions.<String, Set<String>>name(CACHE_NAME).codec(CODEC)
      );
    repository2 = new RedisNetexIdRepository(redissonClient2, commonIdsCache2);
  }

  @AfterEach
  void tearDown() {
    repository.cleanUp(TEST_REPORT_ID);
    redissonClient.getKeys().flushdb();
  }

  @Test
  void testDuplicateDetectionAcrossMultipleFiles() {
    // Given: Three files with overlapping IDs
    // FileA has: ID1, ID2, ID3
    // FileB has: ID3, ID4, ID5 (ID3 is duplicate with FileA)
    // FileC has: ID1, ID6, ID7 (ID1 is duplicate with FileA)

    // Process File A
    Set<String> fileAIds = Set.of(
      "TST:Line:ID1",
      "TST:Line:ID2",
      "TST:Line:ID3"
    );
    Set<String> duplicatesInFileA = repository.getDuplicateNetexIds(
      TEST_REPORT_ID,
      "fileA.xml",
      fileAIds
    );

    // File A has no duplicates (first file processed)
    assertEquals(
      0,
      duplicatesInFileA.size(),
      "File A should have no duplicates as it's the first file"
    );

    // Process File B
    Set<String> fileBIds = Set.of(
      "TST:Line:ID3",
      "TST:Line:ID4",
      "TST:Line:ID5"
    );
    Set<String> duplicatesInFileB = repository.getDuplicateNetexIds(
      TEST_REPORT_ID,
      "fileB.xml",
      fileBIds
    );

    // File B should detect ID3 as duplicate (shared with File A)
    assertEquals(
      1,
      duplicatesInFileB.size(),
      "File B should detect 1 duplicate (ID3)"
    );
    assertTrue(
      duplicatesInFileB.contains("TST:Line:ID3"),
      "ID3 should be detected as duplicate"
    );

    // Process File C
    Set<String> fileCIds = Set.of(
      "TST:Line:ID1",
      "TST:Line:ID6",
      "TST:Line:ID7"
    );
    Set<String> duplicatesInFileC = repository.getDuplicateNetexIds(
      TEST_REPORT_ID,
      "fileC.xml",
      fileCIds
    );

    // File C should detect ID1 as duplicate (shared with File A)

    assertEquals(
      1,
      duplicatesInFileC.size(),
      "File C should detect 1 duplicate (ID1)"
    );
    assertTrue(
      duplicatesInFileC.contains("TST:Line:ID1"),
      "ID1 should be detected as duplicate"
    );
  }

  /**
   * This test verifies idempotency - processing the same file twice
   * should return the same duplicates.
   */
  @Test
  void testIdempotency_ProcessingSameFileTwice() {
    Set<String> fileIds = Set.of("TST:Line:A", "TST:Line:B");

    // Process file first time
    Set<String> firstResult = repository.getDuplicateNetexIds(
      TEST_REPORT_ID,
      "test.xml",
      fileIds
    );

    // Process same file again (simulating duplicate PubSub message)
    Set<String> secondResult = repository.getDuplicateNetexIds(
      TEST_REPORT_ID,
      "test.xml",
      fileIds
    );

    // Results should be identical
    assertEquals(
      firstResult,
      secondResult,
      "Processing same file twice should return same results (idempotency)"
    );
  }

  /**
   * Per-report keys must carry a TTL so that validations that never reach
   * cleanUp do not leak keys. Pins the expire-after-write ordering: EXPIRE
   * on a nonexistent key is a no-op.
   */
  @Test
  void testPerReportKeysHaveTtl() {
    repository.getDuplicateNetexIds(
      TEST_REPORT_ID,
      "fileA.xml",
      Set.of("TST:Line:ID1", "TST:Line:ID2")
    );
    // second file with a duplicate so the duplicated ids set is created
    repository.getDuplicateNetexIds(
      TEST_REPORT_ID,
      "fileB.xml",
      Set.of("TST:Line:ID1")
    );

    assertKeyHasTtl("NETEX_LOCAL_ID_SET_" + TEST_REPORT_ID + "_fileA.xml");
    assertKeyHasTtl("NETEX_LOCAL_ID_SET_" + TEST_REPORT_ID + "_fileB.xml");
    assertKeyHasTtl("DUPLICATED_ID_SET_" + TEST_REPORT_ID + "_fileB.xml");
    assertKeyHasTtl("ACCUMULATED_NETEX_ID_SET_" + TEST_REPORT_ID);
  }

  @Test
  void testAddSharedNetexIdsAccumulatesAcrossMultipleCalls() {
    Set<IdVersion> file1Ids = Set.of(
      new IdVersion("mor:DayType:1", null, "DayType", null, null, 0, 0),
      new IdVersion("mor:DayType:2", null, "DayType", null, null, 0, 0)
    );
    Set<IdVersion> file2Ids = Set.of(
      new IdVersion("mor:Operator:1", null, "Operator", null, null, 0, 0)
    );
    Set<IdVersion> file3Ids = Set.of(
      new IdVersion("mor:DayType:3", null, "DayType", null, null, 0, 0)
    );

    repository.addSharedNetexIds(TEST_REPORT_ID, file1Ids);
    repository.addSharedNetexIds(TEST_REPORT_ID, file2Ids);
    repository.addSharedNetexIds(TEST_REPORT_ID, file3Ids);

    Set<String> shared = repository.getSharedNetexIds(TEST_REPORT_ID);
    assertEquals(4, shared.size());
    assertTrue(shared.contains("mor:DayType:1"));
    assertTrue(shared.contains("mor:DayType:2"));
    assertTrue(shared.contains("mor:Operator:1"));
    assertTrue(shared.contains("mor:DayType:3"));
  }

  /**
   * The stale read this fixes needs a local cache that is populated <em>and</em> out of date. Racing two
   * threads does not produce that: both local caches start empty, and a miss falls through to Redis, so
   * whichever pod takes the lock second reads fresh even on the unfixed code.
   *
   * <p>A plain RMap write publishes no invalidation, which makes the staleness deterministic with no
   * threads, no latch and no dependence on pub/sub timing. Against the unfixed addSharedNetexIds this
   * fails: pod B reads its stale local entry and writes mor:DayType:2 back out of existence.
   */
  @Test
  void testAddSharedNetexIdsReadsRedisRatherThanTheStaleLocalCache() {
    // Mirrors RedisNetexIdRepository.getCommonNetexIdsKey, which is private.
    String cacheKey = "COMMON_NETEX_ID_SET_" + TEST_REPORT_ID;
    RMap<String, Set<String>> redisView = redissonClient.getMap(
      CACHE_NAME,
      CODEC
    );

    // Pod B validates a common file, so its local cache now holds what it wrote.
    repository2.addSharedNetexIds(TEST_REPORT_ID, Set.of(id("mor:DayType:1")));

    // Another pod adds an id straight to Redis. No invalidation is broadcast, so pod B's local cache
    // is now definitively behind.
    redisView.put(
      cacheKey,
      new HashSet<>(Set.of("mor:DayType:1", "mor:DayType:2"))
    );

    // Pod B validates a second common file.
    repository2.addSharedNetexIds(TEST_REPORT_ID, Set.of(id("mor:Operator:1")));

    assertEquals(
      Set.of("mor:DayType:1", "mor:DayType:2", "mor:Operator:1"),
      redisView.get(cacheKey),
      "the id added while pod B's local cache was stale must survive pod B's next accumulate"
    );
  }

  /** addSharedNetexIds reads only getId(), so the other components do not matter here. */
  private static IdVersion id(String id) {
    return new IdVersion(id, null, "DayType", null, null, 0, 0);
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
