package no.entur.antu.stop;

import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;
import java.util.Map;
import org.entur.netex.validation.validator.model.QuayCoordinates;
import org.entur.netex.validation.validator.model.QuayId;
import org.entur.netex.validation.validator.model.SimpleQuay;
import org.entur.netex.validation.validator.model.SimpleStopPlace;
import org.entur.netex.validation.validator.model.StopPlaceId;
import org.junit.jupiter.api.Test;

/**
 * A quay can outlive its stop place: separate changelog events update the two caches, and a refresh
 * replaces the stop places before the quays. The lookups must answer rather than throw — a throw
 * aborts the whole file, while an absent answer is a gap callers already skip.
 */
class DefaultStopPlaceRepositoryLookupTest {

  private static final StopPlaceId DELETED_STOP_PLACE_ID = new StopPlaceId(
    "NSR:StopPlace:58070"
  );
  private static final QuayId ORPHANED_QUAY_ID = new QuayId("NSR:Quay:99563");

  private final Map<StopPlaceId, SimpleStopPlace> stopPlaceCache =
    new HashMap<>();
  private final Map<QuayId, SimpleQuay> quayCache = new HashMap<>();

  private DefaultStopPlaceRepository repositoryWithOrphanedQuay() {
    quayCache.put(
      ORPHANED_QUAY_ID,
      new SimpleQuay(
        new QuayCoordinates(12.244322, 61.306698),
        DELETED_STOP_PLACE_ID
      )
    );
    return new DefaultStopPlaceRepository(null, stopPlaceCache, quayCache);
  }

  @Test
  void transportModesAreAbsentWhenTheParentStopPlaceIsGone() {
    assertNull(
      repositoryWithOrphanedQuay().getTransportModesForQuayId(ORPHANED_QUAY_ID)
    );
  }

  @Test
  void stopPlaceNameIsAbsentWhenTheParentStopPlaceIsGone() {
    assertNull(
      repositoryWithOrphanedQuay().getStopPlaceNameForQuayId(ORPHANED_QUAY_ID)
    );
  }
}
