package no.entur.antu.stop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import no.entur.antu.config.EmbeddedRedisTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StopPlaceDatasetVersionRepositoryTest extends EmbeddedRedisTestBase {

  private static final String EXPORT =
    "tiamat-export-CurrentAndFuture-202608211200010574.xml";

  private StopPlaceDatasetVersionRepository repository;

  @BeforeEach
  void setUp() {
    repository = new StopPlaceDatasetVersionRepository(redissonClient);
    repository.releaseRefresh();
  }

  @Test
  void theVersionSurvivesForTheNextPodToRead() {
    repository.set(EXPORT);

    assertEquals(EXPORT, repository.get());
  }

  @Test
  void anUnknownVersionIsNull() {
    redissonClient.getBucket("stopPlaceDatasetVersion").delete();

    assertNull(repository.get());
  }

  /**
   * The point of the claim: two pods deciding at the same instant that the export is new must produce one
   * refresh, not one each.
   */
  @Test
  void onlyOneCallerGetsTheRefreshClaim() {
    assertTrue(repository.claimRefresh());
    assertFalse(repository.claimRefresh());
  }

  @Test
  void aReleasedClaimCanBeTakenAgain() {
    repository.claimRefresh();
    repository.releaseRefresh();

    assertTrue(repository.claimRefresh());
  }
}
