/*
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *   https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */

package no.entur.antu.stop;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.entur.netex.validation.validator.model.QuayCoordinates;
import org.entur.netex.validation.validator.model.QuayId;
import org.entur.netex.validation.validator.model.SimpleQuay;
import org.entur.netex.validation.validator.model.SimpleStopPlace;
import org.entur.netex.validation.validator.model.StopPlaceId;
import org.entur.netex.validation.validator.model.TransportModeAndSubMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * StopPlaceRepository implementation using the new API exposed in Tiamat.
 */
public class DefaultStopPlaceRepository implements StopPlaceRepositoryLoader {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    DefaultStopPlaceRepository.class
  );

  public static final String QUAY_CACHE = "quayCache";
  public static final String STOP_PLACE_CACHE = "stopPlaceCache";

  private final StopPlaceResource stopPlaceResource;
  private final Map<StopPlaceId, SimpleStopPlace> stopPlaceCache;
  private final Map<QuayId, SimpleQuay> quayCache;

  public DefaultStopPlaceRepository(
    StopPlaceResource stopPlaceResource,
    Map<StopPlaceId, SimpleStopPlace> stopPlaceCache,
    Map<QuayId, SimpleQuay> quayCache
  ) {
    this.stopPlaceResource = stopPlaceResource;
    this.stopPlaceCache = Objects.requireNonNull(stopPlaceCache);
    this.quayCache = Objects.requireNonNull(quayCache);
  }

  @Override
  public boolean hasStopPlaceId(StopPlaceId stopPlaceId) {
    return stopPlaceCache.containsKey(stopPlaceId);
  }

  @Override
  public boolean hasQuayId(QuayId quayId) {
    if (quayCache.containsKey(quayId)) {
      return true;
    }
    return getQuay(quayId).isPresent();
  }

  @Override
  public TransportModeAndSubMode getTransportModesForQuayId(QuayId quayId) {
    return getQuay(quayId)
      .map(quay ->
        stopPlaceCache.get(quay.stopPlaceId()).transportModeAndSubMode()
      )
      .orElse(null);
  }

  @Override
  public QuayCoordinates getCoordinatesForQuayId(QuayId quayId) {
    return getQuay(quayId).map(SimpleQuay::quayCoordinates).orElse(null);
  }

  @Override
  public String getStopPlaceNameForQuayId(QuayId quayId) {
    return getQuay(quayId)
      .map(quay -> stopPlaceCache.get(quay.stopPlaceId()).name())
      .orElse(null);
  }

  @Override
  public boolean isEmpty() {
    return quayCache.isEmpty() || stopPlaceCache.isEmpty();
  }

  @Override
  public void refreshCache() {
    stopPlaceResource.clear();
    Map<StopPlaceId, SimpleStopPlace> newStopPlaceCache =
      stopPlaceResource.getStopPlaces();

    // TODO: Keep warning logs for testing, remove them later
    if (newStopPlaceCache.isEmpty()) {
      LOGGER.warn("Unable to refresh cache, no stop place found");
    } else {
      stopPlaceCache.keySet().retainAll(newStopPlaceCache.keySet());
      stopPlaceCache.putAll(newStopPlaceCache);
    }
    LOGGER.info("Updated Stop place cache");

    Map<QuayId, SimpleQuay> newQuayCache = stopPlaceResource.getQuays();
    if (newQuayCache.isEmpty()) {
      LOGGER.warn("Unable to refresh cache, no quay found");
    } else {
      quayCache.keySet().retainAll(newQuayCache.keySet());
      quayCache.putAll(newQuayCache);
    }
    LOGGER.info("Updated Quay cache");

    stopPlaceResource.clear();

    LOGGER.info(
      "Updated cache with " + "{} stop places ids, " + "{} quays ids ",
      stopPlaceCache.size(),
      quayCache.size()
    );
  }

  private Optional<SimpleQuay> getQuay(QuayId quayId) {
    SimpleQuay quayFromCache = quayCache.get(quayId);
    if (quayFromCache != null) {
      return Optional.of(quayFromCache);
    }
    return Optional.empty();
  }
}
