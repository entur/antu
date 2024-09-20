package no.entur.antu.stop;

import java.util.Map;
import no.entur.antu.model.NetexQuay;
import no.entur.antu.model.NetexStopPlace;
import no.entur.antu.model.QuayId;
import no.entur.antu.model.StopPlaceId;

/**
 * A resource to query the National Stop Place Register.
 */
public interface StopPlaceResource {
  void loadStopPlacesDataset();

  Map<StopPlaceId, NetexStopPlace> getStopPlaces();

  Map<QuayId, NetexQuay> getQuays();
}
