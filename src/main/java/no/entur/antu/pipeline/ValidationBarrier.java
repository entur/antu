package no.entur.antu.pipeline;

import static no.entur.antu.config.cache.CacheConfig.VALIDATION_DATA_TTL;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.redisson.api.RBucket;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Waits for every file of a dataset to reach a given stage before letting exactly one caller proceed.
 *
 * <p>The pipeline has two of these: the line files may not be validated until all common files are, and the
 * reports may not be merged until every file has produced one. The work is spread over an arbitrary number
 * of pods, so the count is kept in Redis and any pod can close any barrier. No leader is needed.
 */
@Component
public class ValidationBarrier {

  public enum Stage {
    COMMON_FILES_VALIDATED,
    REPORTS_WRITTEN,
  }

  private static final Logger LOGGER = LoggerFactory.getLogger(
    ValidationBarrier.class
  );

  private static final String KEY_PREFIX = "BARRIER_";
  private static final String PASSED_SUFFIX = "_passed";

  private final RedissonClient redissonClient;

  public ValidationBarrier(RedissonClient redissonClient) {
    this.redissonClient = redissonClient;
  }

  /**
   * Record that {@code fileName} reached {@code stage}, and run {@code onOpen} if that was the last file
   * the barrier was waiting for.
   *
   * <p>{@code onOpen} receives the names of every file that arrived and runs for exactly one caller, no
   * matter how many pods finish at the same instant. It runs while the barrier is held, and the hold is
   * released if it throws: recording an arrival is idempotent, but passing the barrier is a one-time claim,
   * and a claim taken before an action that then failed would leave the dataset waiting forever with
   * nothing left to release it.
   *
   * @param expected the number of distinct files that have to arrive before the barrier opens.
   */
  public void arrive(
    Stage stage,
    String validationReportId,
    String fileName,
    int expected,
    Consumer<List<String>> onOpen
  ) {
    String key = key(stage, validationReportId);
    RSet<String> arrived = redissonClient.getSet(key);
    arrived.add(fileName);
    // EXPIRE on a key that does not exist yet is a silent no-op, so it has to follow the write.
    arrived.expire(VALIDATION_DATA_TTL);

    int arrivedCount = arrived.size();
    LOGGER.debug(
      "{}: {} of {} files arrived for report {}",
      stage,
      arrivedCount,
      expected,
      validationReportId
    );
    if (arrivedCount < expected) {
      return;
    }

    // Several pods can see a full set at once. Claiming the barrier before reading the set makes exactly
    // one of them proceed; the claim is what the next stage is guarded by, not the count.
    RBucket<Boolean> passed = redissonClient.getBucket(key + PASSED_SUFFIX);
    if (!passed.setIfAbsent(Boolean.TRUE, VALIDATION_DATA_TTL)) {
      LOGGER.debug(
        "{}: barrier for report {} was already passed",
        stage,
        validationReportId
      );
      return;
    }

    Set<String> fileNames = arrived.readAll();
    LOGGER.info(
      "{}: barrier opened for report {} with {} files",
      stage,
      validationReportId,
      fileNames.size()
    );
    RedisClaim.runOrRelease(
      passed,
      stage + " barrier for report " + validationReportId,
      () -> onOpen.accept(fileNames.stream().sorted().toList())
    );
  }

  public void cleanUp(String validationReportId) {
    for (Stage stage : Stage.values()) {
      String key = key(stage, validationReportId);
      redissonClient.getSet(key).delete();
      redissonClient.getBucket(key + PASSED_SUFFIX).delete();
    }
  }

  private static String key(Stage stage, String validationReportId) {
    return KEY_PREFIX + stage.name() + '_' + validationReportId;
  }
}
