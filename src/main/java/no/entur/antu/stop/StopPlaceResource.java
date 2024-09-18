package no.entur.antu.stop;

import java.util.Map;
import java.util.Set;
import no.entur.antu.model.QuayCoordinates;
import no.entur.antu.model.QuayId;
import no.entur.antu.model.TransportModeAndSubMode;

/**
 * A resource to query the National Stop Place Register.
 */
public interface StopPlaceResource {
  /**
   * Returns all quay ids.
   *
   * @return all quay ids.
   */
  Set<String> getQuayIds();

  /**
   * Returns all stop place ids.
   *
   * @return all stop place ids.
   */
  Set<String> getStopPlaceIds();

  /**
   * Returns transport modes per quay ids.
   *
   * @return map of quay ids and transport mode.
   */
  Map<QuayId, TransportModeAndSubMode> getTransportModesPerQuayId();

  /**
   * Returns coordinates per quay ids.
   *
   * @return map of quay ids and coordinates.
   */
  Map<QuayId, QuayCoordinates> getCoordinatesPerQuayId();

  /**
   * Returns stop place names per quay ids.
   *
   * @return map of quay ids and stop place names.
   */
  Map<QuayId, String> getStopPlaceNamesPerQuayId();

  void loadStopPlacesDataset();
}
