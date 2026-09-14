package no.entur.antu.stop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import no.entur.antu.exception.AntuException;
import org.entur.netex.validation.validator.model.QuayCoordinates;
import org.entur.netex.validation.validator.model.QuayId;
import org.entur.netex.validation.validator.model.SimpleQuay;
import org.entur.netex.validation.validator.model.SimpleStopPlace;
import org.entur.netex.validation.validator.model.StopPlaceId;
import org.junit.jupiter.api.Test;
import org.redisson.api.RMap;

/**
 * The national registry is never empty, so a dataset that yields nothing is a truncated or unparsed
 * export. It has to fail rather than log and return, which leaves the caller believing the cache was
 * refreshed.
 */
class DefaultStopPlaceRepositoryRefreshTest {

  private static final StopPlaceId STOP_PLACE_ID = new StopPlaceId(
    "NSR:StopPlace:1"
  );
  private static final QuayId QUAY_ID = new QuayId("NSR:Quay:1");

  private final Map<StopPlaceId, SimpleStopPlace> stopPlaceCache =
    new HashMap<>();
  private final Map<QuayId, SimpleQuay> quayCache = new HashMap<>();

  @Test
  void aDatasetWithStopPlacesAndQuaysIsLoaded() {
    DefaultStopPlaceRepository repository = repository(
      Map.of(STOP_PLACE_ID, new SimpleStopPlace("Jektevik terminal", null)),
      Map.of(
        QUAY_ID,
        new SimpleQuay(new QuayCoordinates(5.3, 60.3), STOP_PLACE_ID)
      )
    );

    repository.refreshCache();

    assertEquals(1, stopPlaceCache.size());
    assertEquals(1, quayCache.size());
  }

  @Test
  void aDatasetWithNoStopPlacesIsRefused() {
    DefaultStopPlaceRepository repository = repository(Map.of(), Map.of());

    assertThrows(AntuException.class, repository::refreshCache);
  }

  /**
   * Both halves are checked before either cache is touched: a half-applied refresh would pair the new
   * export's stop places with the previous one's quays, and the quays whose stop place the refresh dropped
   * have nothing to resolve against.
   */
  @Test
  void aDatasetWithNoQuaysIsRefusedAndChangesNothing() {
    StopPlaceId loadedEarlier = new StopPlaceId("NSR:StopPlace:2");
    stopPlaceCache.put(loadedEarlier, new SimpleStopPlace("Sandvika", null));
    quayCache.put(
      QUAY_ID,
      new SimpleQuay(new QuayCoordinates(5.3, 60.3), loadedEarlier)
    );
    DefaultStopPlaceRepository repository = repository(
      Map.of(STOP_PLACE_ID, new SimpleStopPlace("Jektevik terminal", null)),
      Map.of()
    );

    assertThrows(AntuException.class, repository::refreshCache);

    assertEquals(
      Map.of(loadedEarlier, new SimpleStopPlace("Sandvika", null)),
      stopPlaceCache
    );
    assertEquals(1, quayCache.size());
  }

  /**
   * loadCache clears the resource before merging, to avoid holding the export twice for the length of
   * the merge. Everything read from the resource therefore has to be read before that point:
   * {@link StopPlaceResource#getPublicationTime()} re-parses the dataset once the resource is cleared,
   * so reading it after the merge loads the national register a second time.
   */
  @Test
  void theExportIsParsedOncePerRefresh() {
    CountingStopPlaceResource resource = new CountingStopPlaceResource(
      Map.of(STOP_PLACE_ID, new SimpleStopPlace("Jektevik terminal", null)),
      Map.of(
        QUAY_ID,
        new SimpleQuay(new QuayCoordinates(5.3, 60.3), STOP_PLACE_ID)
      )
    );

    new DefaultStopPlaceRepository(resource, stopPlaceCache, quayCache)
      .refreshCache();

    assertEquals(1, resource.parseCount());
  }

  /** Models {@link DefaultStopPlaceResource}: the getters re-parse whenever the resource was cleared. */
  private static final class CountingStopPlaceResource
    implements StopPlaceResource {

    private final Map<StopPlaceId, SimpleStopPlace> stopPlaces;
    private final Map<QuayId, SimpleQuay> quays;
    private boolean parsed;
    private int parseCount;

    private CountingStopPlaceResource(
      Map<StopPlaceId, SimpleStopPlace> stopPlaces,
      Map<QuayId, SimpleQuay> quays
    ) {
      this.stopPlaces = stopPlaces;
      this.quays = quays;
    }

    private int parseCount() {
      return parseCount;
    }

    private void init() {
      if (!parsed) {
        parsed = true;
        parseCount++;
      }
    }

    @Override
    public Map<QuayId, SimpleQuay> getQuays() {
      init();
      return quays;
    }

    @Override
    public Map<StopPlaceId, SimpleStopPlace> getStopPlaces() {
      init();
      return stopPlaces;
    }

    @Override
    public Instant getPublicationTime() {
      init();
      return Instant.EPOCH;
    }

    @Override
    public void clear() {
      parsed = false;
    }
  }

  /**
   * Both putAll overloads leave the same contents behind, so the call itself is the only thing that
   * can be asserted, and it is the whole fix: the unbatched one serialises the register in a single
   * call and OOM-kills the container.
   */
  @Test
  @SuppressWarnings("unchecked")
  void theExportIsWrittenThroughTheBatchedPutAll() {
    RMap<StopPlaceId, SimpleStopPlace> stopPlaces = mock(RMap.class);
    RMap<QuayId, SimpleQuay> quays = mock(RMap.class);

    new DefaultStopPlaceRepository(
      resource(
        Map.of(STOP_PLACE_ID, new SimpleStopPlace("Jektevik terminal", null)),
        Map.of(
          QUAY_ID,
          new SimpleQuay(new QuayCoordinates(5.3, 60.3), STOP_PLACE_ID)
        )
      ),
      stopPlaces,
      quays
    )
      .refreshCache();

    verify(stopPlaces).putAll(anyMap(), anyInt());
    verify(quays).putAll(anyMap(), anyInt());
    verify(stopPlaces, never()).putAll(anyMap());
    verify(quays, never()).putAll(anyMap());
  }

  private DefaultStopPlaceRepository repository(
    Map<StopPlaceId, SimpleStopPlace> stopPlaces,
    Map<QuayId, SimpleQuay> quays
  ) {
    return new DefaultStopPlaceRepository(
      resource(stopPlaces, quays),
      stopPlaceCache,
      quayCache
    );
  }

  private static StopPlaceResource resource(
    Map<StopPlaceId, SimpleStopPlace> stopPlaces,
    Map<QuayId, SimpleQuay> quays
  ) {
    return new StopPlaceResource() {
      @Override
      public Map<QuayId, SimpleQuay> getQuays() {
        return quays;
      }

      @Override
      public Map<StopPlaceId, SimpleStopPlace> getStopPlaces() {
        return stopPlaces;
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
