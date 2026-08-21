package no.entur.antu.stop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import no.entur.antu.stop.registry.RegisteredStopPlace;
import no.entur.antu.stop.registry.StopPlaceRegistry;
import org.entur.netex.validation.validator.model.QuayCoordinates;
import org.entur.netex.validation.validator.model.QuayId;
import org.entur.netex.validation.validator.model.SimpleQuay;
import org.entur.netex.validation.validator.model.SimpleStopPlace;
import org.entur.netex.validation.validator.model.StopPlaceId;
import org.entur.netex.validation.validator.model.TransportModeAndSubMode;
import org.junit.jupiter.api.Test;
import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;

/**
 * The cache is rebuilt from a NeTEx export produced twice a day. These cover what happens to an id that
 * is younger than the last one.
 */
class DefaultStopPlaceRepositoryTest {

  private static final QuayId QUAY_ID = new QuayId("NSR:Quay:111871");
  private static final StopPlaceId STOP_PLACE_ID = new StopPlaceId(
    "NSR:StopPlace:64572"
  );

  private final Map<StopPlaceId, SimpleStopPlace> stopPlaceCache =
    new HashMap<>();
  private final Map<QuayId, SimpleQuay> quayCache = new HashMap<>();

  @Test
  void aQuayMissingFromTheCacheIsLookedUpInTheRegistry() {
    DefaultStopPlaceRepository repository = repository(registryWithJektevik());

    assertTrue(repository.hasQuayId(QUAY_ID));
  }

  @Test
  void aQuayFoundInTheRegistryIsCachedWithItsStopPlace() {
    DefaultStopPlaceRepository repository = repository(registryWithJektevik());

    repository.hasQuayId(QUAY_ID);

    assertEquals(
      "Jektevik terminal",
      repository.getStopPlaceNameForQuayId(QUAY_ID)
    );
    assertEquals(
      AllVehicleModesOfTransportEnumeration.WATER,
      repository.getTransportModesForQuayId(QUAY_ID).mode()
    );
    assertTrue(quayCache.containsKey(QUAY_ID));
    assertTrue(stopPlaceCache.containsKey(STOP_PLACE_ID));
  }

  @Test
  void aStopPlaceMissingFromTheCacheIsLookedUpInTheRegistry() {
    DefaultStopPlaceRepository repository = repository(registryWithJektevik());

    assertTrue(repository.hasStopPlaceId(STOP_PLACE_ID));
    assertTrue(stopPlaceCache.containsKey(STOP_PLACE_ID));
  }

  @Test
  void anIdTheRegistryDoesNotHaveStaysUnresolved() {
    DefaultStopPlaceRepository repository = repository(
      StopPlaceRegistry.disabled()
    );

    assertFalse(repository.hasQuayId(QUAY_ID));
    assertFalse(repository.hasStopPlaceId(STOP_PLACE_ID));
    assertTrue(quayCache.isEmpty());
    assertTrue(stopPlaceCache.isEmpty());
  }

  @Test
  void aCachedQuayIsNotLookedUp() {
    quayCache.put(
      QUAY_ID,
      new SimpleQuay(new QuayCoordinates(5.3, 60.3), STOP_PLACE_ID)
    );
    DefaultStopPlaceRepository repository = repository(failingRegistry());

    assertTrue(repository.hasQuayId(QUAY_ID));
  }

  private DefaultStopPlaceRepository repository(StopPlaceRegistry registry) {
    return new DefaultStopPlaceRepository(
      null,
      stopPlaceCache,
      quayCache,
      registry
    );
  }

  private static StopPlaceRegistry registryWithJektevik() {
    RegisteredStopPlace registered = new RegisteredStopPlace(
      STOP_PLACE_ID,
      new SimpleStopPlace(
        "Jektevik terminal",
        new TransportModeAndSubMode(
          AllVehicleModesOfTransportEnumeration.WATER,
          null
        )
      ),
      Map.of(
        QUAY_ID,
        new SimpleQuay(new QuayCoordinates(5.308161, 60.392249), STOP_PLACE_ID)
      )
    );
    return new StopPlaceRegistry() {
      @Override
      public RegisteredStopPlace findByQuayId(QuayId quayId) {
        return QUAY_ID.equals(quayId) ? registered : null;
      }

      @Override
      public RegisteredStopPlace findById(StopPlaceId stopPlaceId) {
        return STOP_PLACE_ID.equals(stopPlaceId) ? registered : null;
      }
    };
  }

  private static StopPlaceRegistry failingRegistry() {
    return new StopPlaceRegistry() {
      @Override
      public RegisteredStopPlace findByQuayId(QuayId quayId) {
        throw new AssertionError("the registry must not be asked");
      }

      @Override
      public RegisteredStopPlace findById(StopPlaceId stopPlaceId) {
        throw new AssertionError("the registry must not be asked");
      }
    };
  }
}
