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

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import no.entur.antu.exception.AntuException;
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
  public Set<String> getQuaysForStopPlaceId(StopPlaceId stopPlaceId) {
    return quayCache
      .entrySet()
      .stream()
      .filter(entry -> entry.getValue().stopPlaceId().equals(stopPlaceId))
      .map(entry -> entry.getKey().id())
      .collect(Collectors.toSet());
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
  public Instant refreshCache() {
    try {
      return loadCache();
    } finally {
      // The resource holds the parsed dataset, tens of megabytes of it, until this is called. On the way
      // out of a refusal too, or the pod carries it until the next refresh.
      stopPlaceResource.clear();
    }
  }

  private Instant loadCache() {
    stopPlaceResource.clear();
    Map<StopPlaceId, SimpleStopPlace> newStopPlaceCache =
      stopPlaceResource.getStopPlaces();
    Map<QuayId, SimpleQuay> newQuayCache = stopPlaceResource.getQuays();

    // The national registry is never empty, so this is a truncated or unparsed export. Refuse it rather
    // than log and carry on, which leaves the caller believing the cache was refreshed. Checked before
    // either cache is touched, so the caches keep the export they had rather than pairing new stop
    // places with the previous quays.
    if (newStopPlaceCache.isEmpty() || newQuayCache.isEmpty()) {
      throw new AntuException(
        "Refusing to refresh the stop place cache from a dataset with %d stop places and %d quays".formatted(
            newStopPlaceCache.size(),
            newQuayCache.size()
          )
      );
    }

    stopPlaceCache.keySet().retainAll(newStopPlaceCache.keySet());
    stopPlaceCache.putAll(newStopPlaceCache);
    LOGGER.info("Updated Stop place cache");

    quayCache.keySet().retainAll(newQuayCache.keySet());
    quayCache.putAll(newQuayCache);
    LOGGER.info("Updated Quay cache");

    Instant publicationTime = stopPlaceResource.getPublicationTime();

    LOGGER.info(
      "Updated cache with " +
      "{} stop place ids, " +
      "{} quay ids. Publication time: {} ",
      stopPlaceCache.size(),
      quayCache.size(),
      publicationTime
    );
    return publicationTime;
  }

  @Override
  public void createOrUpdateQuay(QuayId id, SimpleQuay quay) {
    quayCache.put(id, quay);
  }

  @Override
  public void createOrUpdateStopPlace(
    StopPlaceId id,
    SimpleStopPlace stopPlace
  ) {
    stopPlaceCache.put(id, stopPlace);
  }

  @Override
  public void deleteStopPlace(StopPlaceId stopPlaceId) {
    stopPlaceCache.remove(stopPlaceId);
  }

  @Override
  public void deleteQuay(QuayId quayId) {
    quayCache.remove(quayId);
  }

  private Optional<SimpleQuay> getQuay(QuayId quayId) {
    SimpleQuay quayFromCache = quayCache.get(quayId);
    if (quayFromCache != null) {
      return Optional.of(quayFromCache);
    }
    return Optional.empty();
  }
}
