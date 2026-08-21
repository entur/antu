package no.entur.antu.config;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import no.entur.antu.stop.registry.NotFoundIdCache;
import no.entur.antu.stop.registry.StopPlaceRegistry;
import no.entur.antu.stop.registry.TiamatStopPlaceRegistry;
import org.entur.netex.validation.validator.model.QuayId;
import org.junit.jupiter.api.Test;

class StopPlaceConfigTest {

  private final StopPlaceConfig config = new StopPlaceConfig();

  @Test
  void aConfiguredUrlGivesARegistryBackedByTheReadApi() {
    StopPlaceRegistry registry = config.stopPlaceRegistry(
      "https://api.dev.entur.io/stop-places/v1/read",
      "entur-antu",
      neverAsked()
    );

    assertInstanceOf(TiamatStopPlaceRegistry.class, registry);
  }

  /**
   * Environments without a reachable registry, the local cluster among them, must still start, and
   * must answer as they did before the lookup existed.
   */
  @Test
  void noUrlGivesARegistryThatKnowsNothing() {
    StopPlaceRegistry registry = config.stopPlaceRegistry(
      "",
      "entur-antu",
      neverAsked()
    );

    assertNotNull(registry);
    assertNull(registry.findByQuayId(new QuayId("NSR:Quay:111871")));
  }

  private static NotFoundIdCache neverAsked() {
    return new NotFoundIdCache() {
      @Override
      public boolean contains(String id) {
        throw new AssertionError("the cache must not be asked");
      }

      @Override
      public void remember(String id) {
        throw new AssertionError("the cache must not be asked");
      }
    };
  }
}
