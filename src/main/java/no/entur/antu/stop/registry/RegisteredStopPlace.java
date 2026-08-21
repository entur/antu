package no.entur.antu.stop.registry;

import java.util.Map;
import org.entur.netex.validation.validator.model.QuayId;
import org.entur.netex.validation.validator.model.SimpleQuay;
import org.entur.netex.validation.validator.model.SimpleStopPlace;
import org.entur.netex.validation.validator.model.StopPlaceId;

/**
 * A stop place as the registry returned it, in the shape the cache stores.
 */
public record RegisteredStopPlace(
  StopPlaceId id,
  SimpleStopPlace stopPlace,
  Map<QuayId, SimpleQuay> quays
) {
  public RegisteredStopPlace {
    quays = Map.copyOf(quays);
  }
}
