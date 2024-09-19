package no.entur.antu.memorystore;

import java.time.Duration;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

/**
 * Redis-based implementation of the temporary file repository.
 */
public class RedisTemporaryFileRepository implements TemporaryFileRepository {

  private static final String TEMPORARY_FILE_KEY_PREFIX = "TEMPORARY_FILE_";
  private final RedissonClient redissonClient;

  public RedisTemporaryFileRepository(RedissonClient redissonClient) {
    this.redissonClient = redissonClient;
  }

  @Override
  public void upload(
    String validationReportId,
    String fileName,
    byte[] content
  ) {
    RBucket<Object> temporaryFile = redissonClient.getBucket(
      getTemporaryFileKey(validationReportId, fileName)
    );
    temporaryFile.set(content, Duration.ofHours(1));
  }

  @Override
  public byte[] download(String validationReportId, String fileName) {
    RBucket<Object> temporaryFile = redissonClient.getBucket(
      getTemporaryFileKey(validationReportId, fileName)
    );
    if (temporaryFile.isExists()) {
      return (byte[]) temporaryFile.get();
    } else {
      throw new AntuMemoryStoreFileNotFoundException(
        "File " +
        fileName +
        " for validation report " +
        validationReportId +
        " not found in the memory store"
      );
    }
  }

  @Override
  public void cleanUp(String reportId) {
    redissonClient
      .getKeys()
      .deleteByPattern(TEMPORARY_FILE_KEY_PREFIX + reportId + '*');
  }

  private String getTemporaryFileKey(
    String validationReportId,
    String fileName
  ) {
    return TEMPORARY_FILE_KEY_PREFIX + validationReportId + '_' + fileName;
  }
}
