package no.entur.antu.stop;

import java.time.Instant;
import java.util.Set;
import org.entur.netex.validation.validator.jaxb.StopPlaceRepository;
import org.entur.netex.validation.validator.model.QuayId;
import org.entur.netex.validation.validator.model.SimpleQuay;
import org.entur.netex.validation.validator.model.SimpleStopPlace;
import org.entur.netex.validation.validator.model.StopPlaceId;

/**
 * This interface extends the read-only interface {@link StopPlaceRepository} with methods for refreshing the repository.
 */
public interface StopPlaceRepositoryLoader extends StopPlaceRepository {
  /**
   * Return true if the repository is not primed.
   */
  boolean isEmpty();

  /**
   * Refresh the repository.
   * This rebuilds the repository from scratch. Real-time updates, if any, are lost and should be applied again.
   *
   * @return the publication time of the stop place repository NeTEx export used to refresh the repository.
   */
  Instant refreshCache();

  /**
   * Create a quay or update it if it is already present in the cache.
   */
  void createOrUpdateQuay(QuayId quayId, SimpleQuay quay);

  /**
   * Create a stop place or update it if it is already present in the cache.
   * This does not update the quays attached to this stop place.
   */
  void createOrUpdateStopPlace(StopPlaceId id, SimpleStopPlace stopPlace);

  /**
   * Delete a stop place by its ID.
   */
  void deleteStopPlace(StopPlaceId stopPlaceId);

  /**
   * Delete a quay by its ID.
   */
  void deleteQuay(QuayId quayId);

  /**
   * Return the set of quay IDs for a given stop place ID.
   */
  Set<String> getQuaysForStopPlaceId(StopPlaceId stopPlaceId);
}
