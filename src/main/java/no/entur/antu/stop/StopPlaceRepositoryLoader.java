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
   *
   * @return
   */
  Instant refreshCache();

  void createOrUpdateQuay(QuayId quayId, SimpleQuay quay);

  void createOrUpdateStopPlace(StopPlaceId id, SimpleStopPlace stopPlace);
}
