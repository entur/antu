package no.entur.antu.stop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import no.entur.antu.config.EmbeddedRedisTestBase;
import org.entur.netex.validation.validator.model.QuayCoordinates;
import org.entur.netex.validation.validator.model.QuayId;
import org.entur.netex.validation.validator.model.SimpleQuay;
import org.entur.netex.validation.validator.model.SimpleStopPlace;
import org.entur.netex.validation.validator.model.StopPlaceId;
import org.junit.jupiter.api.Test;
import org.redisson.api.options.LocalCachedMapOptions;

/**
 * The refresh writes to Redisson maps in production, and only that path batches.
 * {@link DefaultStopPlaceRepositoryRefreshTest} injects plain maps and never reaches it.
 */
class DefaultStopPlaceRepositoryRedisRefreshTest extends EmbeddedRedisTestBase {

  /** Spans several batches at any plausible MERGE_BATCH_SIZE, and ends mid-batch. */
  private static final int ENTRIES = 10_001;

  @Test
  void anExportSpanningSeveralBatchesIsWrittenWhole() {
    Map<StopPlaceId, SimpleStopPlace> stopPlaceCache =
      redissonClient.getLocalCachedMap(
        LocalCachedMapOptions.<StopPlaceId, SimpleStopPlace>name(
          "stopPlaceCacheTest"
        )
      );
    Map<QuayId, SimpleQuay> quayCache = redissonClient.getLocalCachedMap(
      LocalCachedMapOptions.<QuayId, SimpleQuay>name("quayCacheTest")
    );

    StopPlaceId stale = new StopPlaceId("NSR:StopPlace:stale");
    stopPlaceCache.put(stale, new SimpleStopPlace("Dropped", null));

    new DefaultStopPlaceRepository(
      resource(stopPlaces(), quays()),
      stopPlaceCache,
      quayCache
    )
      .refreshCache();

    assertEquals(ENTRIES, stopPlaceCache.size());
    assertEquals(ENTRIES, quayCache.size());
    assertFalse(stopPlaceCache.containsKey(stale));
    assertTrue(
      quayCache.containsKey(new QuayId("NSR:Quay:" + (ENTRIES - 1))),
      "the tail batch was not written"
    );
  }

  private static Map<StopPlaceId, SimpleStopPlace> stopPlaces() {
    return IntStream
      .range(0, ENTRIES)
      .boxed()
      .collect(
        Collectors.toMap(
          i -> new StopPlaceId("NSR:StopPlace:" + i),
          i -> new SimpleStopPlace("Stop " + i, null)
        )
      );
  }

  private static Map<QuayId, SimpleQuay> quays() {
    return IntStream
      .range(0, ENTRIES)
      .boxed()
      .collect(
        Collectors.toMap(
          i -> new QuayId("NSR:Quay:" + i),
          i ->
            new SimpleQuay(
              new QuayCoordinates(5.3, 60.3),
              new StopPlaceId("NSR:StopPlace:" + i)
            )
        )
      );
  }

  private static StopPlaceResource resource(
    Map<StopPlaceId, SimpleStopPlace> stopPlaces,
    Map<QuayId, SimpleQuay> quays
  ) {
    return new StopPlaceResource() {
      @Override
      public Map<QuayId, SimpleQuay> getQuays() {
        return new HashMap<>(quays);
      }

      @Override
      public Map<StopPlaceId, SimpleStopPlace> getStopPlaces() {
        return new HashMap<>(stopPlaces);
      }

      @Override
      public Instant getPublicationTime() {
        return Instant.EPOCH;
      }

      @Override
      public void clear() {}
    };
  }
}
