package no.entur.antu.stop;

import java.util.Map;
import no.entur.antu.model.QuayId;
import no.entur.antu.model.SimpleQuay;
import no.entur.antu.model.SimpleStopPlace;
import no.entur.antu.model.StopPlaceId;

/**
 * A resource to query the National Stop Place Register.
 */
public interface StopPlaceResource {
  void loadStopPlacesDataset();

  Map<StopPlaceId, SimpleStopPlace> getStopPlaces();

  Map<QuayId, SimpleQuay> getQuays();
}
