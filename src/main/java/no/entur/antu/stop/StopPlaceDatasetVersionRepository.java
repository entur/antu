package no.entur.antu.stop;

import javax.annotation.Nullable;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;

/**
 * The version of the stop place dataset the cache was last built from. Shared between pods, because the
 * pod that decides a refresh is due is not the pod that runs it.
 */
public class StopPlaceDatasetVersionRepository {

  private static final String STOP_PLACE_DATASET_VERSION =
    "stopPlaceDatasetVersion";

  private final RedissonClient redissonClient;

  public StopPlaceDatasetVersionRepository(RedissonClient redissonClient) {
    this.redissonClient = redissonClient;
  }

  @Nullable
  public String get() {
    return version().get();
  }

  public void set(@Nullable String datasetVersion) {
    if (datasetVersion == null) {
      return;
    }
    version().set(datasetVersion);
  }

  /**
   * Clear the recorded version, but only while it is still the one given. A caller rolling back its own
   * write must not erase a newer export another pod recorded in the meantime.
   */
  public void clearIfStill(@Nullable String datasetVersion) {
    if (datasetVersion == null) {
      return;
    }
    version().compareAndSet(datasetVersion, null);
  }

  /**
   * Plain strings, not the client's Kryo codec: this is the state an operator reaches for when the cache
   * looks wrong, and a generation that reads back as {@code \x03175...} from redis-cli does not compare
   * with what gcloud prints.
   */
  private RBucket<String> version() {
    return redissonClient.getBucket(
      STOP_PLACE_DATASET_VERSION,
      StringCodec.INSTANCE
    );
  }
}
