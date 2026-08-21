package no.entur.antu.stop;

import java.time.Duration;
import javax.annotation.Nullable;
import org.redisson.api.RedissonClient;

/**
 * What the cluster knows about refreshing the cache from the stop place dataset: the version it was
 * last built from, and whether a refresh is already on its way. Both are shared between pods, because
 * the pod that decides a refresh is due is not the pod that runs it.
 */
public class StopPlaceDatasetVersionRepository {

  private static final String STOP_PLACE_DATASET_VERSION =
    "stopPlaceDatasetVersion";
  private static final String STOP_PLACE_DATASET_REFRESH_CLAIM =
    "stopPlaceDatasetRefreshClaim";

  /**
   * Only a backstop for a pod that dies between claiming and refreshing: a refresh takes under a
   * minute, and the pod that runs it releases the claim whether it worked or not.
   */
  private static final Duration REFRESH_CLAIM_TTL = Duration.ofMinutes(30);

  private final RedissonClient redissonClient;

  public StopPlaceDatasetVersionRepository(RedissonClient redissonClient) {
    this.redissonClient = redissonClient;
  }

  @Nullable
  public String get() {
    return redissonClient.<String>getBucket(STOP_PLACE_DATASET_VERSION).get();
  }

  public void set(@Nullable String version) {
    if (version == null) {
      return;
    }
    redissonClient.<String>getBucket(STOP_PLACE_DATASET_VERSION).set(version);
  }

  /**
   * Take the claim on refreshing from the dataset, and return false if another pod holds it. Atomic:
   * the version alone cannot deduplicate, because it is only written once a refresh has finished, so
   * every trigger firing in between would queue another full dataset parse.
   */
  public boolean claimRefresh() {
    return redissonClient
      .<Boolean>getBucket(STOP_PLACE_DATASET_REFRESH_CLAIM)
      .setIfAbsent(Boolean.TRUE, REFRESH_CLAIM_TTL);
  }

  /**
   * Forget which dataset the cache was built from, so the next check refreshes from whatever is in the
   * bucket.
   */
  public void clear() {
    redissonClient.getBucket(STOP_PLACE_DATASET_VERSION).delete();
  }

  public void releaseRefresh() {
    redissonClient.getBucket(STOP_PLACE_DATASET_REFRESH_CLAIM).delete();
  }
}
