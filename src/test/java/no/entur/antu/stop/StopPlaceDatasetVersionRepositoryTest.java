package no.entur.antu.stop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import no.entur.antu.config.EmbeddedRedisTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.client.codec.StringCodec;

class StopPlaceDatasetVersionRepositoryTest extends EmbeddedRedisTestBase {

  private static final String GENERATION = "1787306879708878";
  private static final String NEWER_GENERATION = "1787306879708879";

  private StopPlaceDatasetVersionRepository repository;

  @BeforeEach
  void setUp() {
    repository = new StopPlaceDatasetVersionRepository(redissonClient);
    bucket().delete();
  }

  @Test
  void theVersionSurvivesForTheNextPodToRead() {
    repository.set(GENERATION);

    assertEquals(GENERATION, repository.get());
  }

  /**
   * Stored as a plain string, so that an operator debugging the cache can compare what redis-cli prints
   * with the generation gcloud reports.
   */
  @Test
  void theVersionIsReadableOutsideTheClient() {
    repository.set(GENERATION);

    assertEquals(GENERATION, bucket().get());
  }

  @Test
  void clearingLetsTheNextCheckSeeTheDatasetAsNew() {
    repository.set(GENERATION);

    repository.clearIfStill(GENERATION);

    assertNull(repository.get());
  }

  /**
   * A caller rolling back its own write must not erase a newer export another pod recorded while it was
   * on its way: that one would then never be loaded.
   */
  @Test
  void aNewerVersionIsNotCleared() {
    repository.set(NEWER_GENERATION);

    repository.clearIfStill(GENERATION);

    assertEquals(NEWER_GENERATION, repository.get());
  }

  @Test
  void clearingAnUnrecordedVersionIsHarmless() {
    repository.clearIfStill(GENERATION);

    assertNull(repository.get());
  }

  private RBucket<String> bucket() {
    return redissonClient.getBucket(
      "stopPlaceDatasetVersion",
      StringCodec.INSTANCE
    );
  }
}
