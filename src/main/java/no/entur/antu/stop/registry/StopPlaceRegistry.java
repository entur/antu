package no.entur.antu.stop.registry;

import javax.annotation.Nullable;
import org.entur.netex.validation.validator.model.QuayId;
import org.entur.netex.validation.validator.model.StopPlaceId;

/**
 * Single stop place lookups against the National Stop Place Registry, for the ids the daily NeTEx
 * export does not cover yet.
 */
public interface StopPlaceRegistry {
  /**
   * Return the stop place holding the given quay, or null if the registry does not have it.
   */
  @Nullable
  RegisteredStopPlace findByQuayId(QuayId quayId);

  /**
   * Return the given stop place, or null if the registry does not have it.
   */
  @Nullable
  RegisteredStopPlace findById(StopPlaceId stopPlaceId);

  /**
   * A registry that knows nothing, for environments without a reachable stop place registry.
   */
  static StopPlaceRegistry disabled() {
    return new StopPlaceRegistry() {
      @Override
      public RegisteredStopPlace findByQuayId(QuayId quayId) {
        return null;
      }

      @Override
      public RegisteredStopPlace findById(StopPlaceId stopPlaceId) {
        return null;
      }
    };
  }
}
