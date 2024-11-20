package no.entur.antu.stop;

import org.entur.netex.validation.validator.jaxb.StopPlaceRepository;

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
   */
  void refreshCache();
}
