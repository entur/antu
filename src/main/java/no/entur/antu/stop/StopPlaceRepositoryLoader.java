package no.entur.antu.stop;

import java.time.Instant;
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

  void createOrUpdateQuay(QuayId quayId, SimpleQuay quay);

  void createOrUpdateStopPlace(StopPlaceId id, SimpleStopPlace stopPlace);
}
