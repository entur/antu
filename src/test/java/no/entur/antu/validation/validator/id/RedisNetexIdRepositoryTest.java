package no.entur.antu.validation.validator.id;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RLocalCachedMap;
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
  private static final Kryo5Codec CODEC = new Kryo5Codec();
  private static final int REDIS_PORT = 6371;

  private static RedisServer redisServer;
  private static RedissonClient redissonClient;

  private RedisNetexIdRepository repository;

  @BeforeAll
  static void startRedis() throws Exception {
    // Start embedded Redis server for testing
    redisServer = new RedisServer(REDIS_PORT);
    redisServer.start();

    // Create Redisson client
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
    RLocalCachedMap<String, Set<String>> commonIdsCache =
      redissonClient.getLocalCachedMap(
        LocalCachedMapOptions
          .<String, Set<String>>name("testCommonIdsCache")
          .codec(CODEC)
      );
    repository = new RedisNetexIdRepository(redissonClient, commonIdsCache);
  }

  @AfterEach
  void tearDown() {
    // Clean up Redis after each test
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
}
