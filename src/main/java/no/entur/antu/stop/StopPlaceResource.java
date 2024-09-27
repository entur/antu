package no.entur.antu.stop;

import java.util.Map;
import org.entur.netex.validation.validator.model.QuayId;
import org.entur.netex.validation.validator.model.SimpleQuay;
import org.entur.netex.validation.validator.model.SimpleStopPlace;
import org.entur.netex.validation.validator.model.StopPlaceId;

/**
 * A resource to query the National Stop Place Register.
 */
public interface StopPlaceResource {
  /**
   * Return a light-way representation of the stop places in the National Stop Place Register.
   * Only data relevant to validation is kept.
   */
  Map<StopPlaceId, SimpleStopPlace> getStopPlaces();

  /**
   * Return a light-way representation of the quays in the National Stop Place Register.
   * Only data relevant to validation is kept.
   */
  Map<QuayId, SimpleQuay> getQuays();

  /**
   * Clear the maps of StopPlaces and Quays.
   * NSR data will be reloaded the next time {@link #getStopPlaces()} or {@link #getQuays()} are called.
   */
  void clear();
}
