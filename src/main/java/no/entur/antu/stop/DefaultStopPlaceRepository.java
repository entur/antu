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

import java.util.*;
import no.entur.antu.model.*;
import no.entur.antu.stop.fetcher.NetexEntityFetcher;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.StopPlace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * StopPlaceRepository implementation using the new API exposed in Tiamat.
 */
public class DefaultStopPlaceRepository implements StopPlaceRepository {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    DefaultStopPlaceRepository.class
  );

  public static final String QUAY_CACHE = "quayCache";
  public static final String STOP_PLACE_CACHE = "stopPlaceCache";

  private final StopPlaceResource stopPlaceResource;
  private final NetexEntityFetcher<Quay, QuayId> quayFetcher;
  private final NetexEntityFetcher<StopPlace, StopPlaceId> stopPlaceFetcher;
  private final NetexEntityFetcher<StopPlace, QuayId> stopPlaceForQuayIdFetcher;
  private final Map<StopPlaceId, SimpleStopPlace> stopPlaceCache;
  private final Map<QuayId, SimpleQuay> quayCache;
  private final Set<QuayId> quayIdNotFoundCache;

  public DefaultStopPlaceRepository(
    StopPlaceResource stopPlaceResource,
    Map<StopPlaceId, SimpleStopPlace> stopPlaceCache,
    Map<QuayId, SimpleQuay> quayCache,
    NetexEntityFetcher<Quay, QuayId> quayFetcher,
    NetexEntityFetcher<StopPlace, StopPlaceId> stopPlaceFetcher,
    NetexEntityFetcher<StopPlace, QuayId> stopPlaceForQuayIdFetcher,
    Set<QuayId> quayIdNotFoundCache
  ) {
    this.stopPlaceResource = stopPlaceResource;
    this.stopPlaceCache = Objects.requireNonNull(stopPlaceCache);
    this.quayCache = Objects.requireNonNull(quayCache);
    this.quayIdNotFoundCache = quayIdNotFoundCache;
    this.quayFetcher = quayFetcher;
    this.stopPlaceFetcher = stopPlaceFetcher;
    this.stopPlaceForQuayIdFetcher = stopPlaceForQuayIdFetcher;
  }

  @Override
  public boolean hasStopPlaceId(StopPlaceId stopPlaceId) {
    if (stopPlaceCache.containsKey(stopPlaceId)) {
      return true;
    }
    StopPlace stopPlace = stopPlaceFetcher.tryFetch(stopPlaceId);
    if (stopPlace != null) {
      stopPlaceCache.put(
        stopPlaceId,
        new SimpleStopPlace(
          stopPlace.getName().getValue(),
          TransportModeAndSubMode.of(stopPlace)
        )
      );
      return true;
    }
    return false;
  }

  @Override
  public boolean hasQuayId(QuayId quayId) {
    if (quayCache.containsKey(quayId)) {
      return true;
    }
    return getQuay(quayId).isPresent();
  }

  private <R> R tryFetchWithNotFoundCheck(
    QuayId quayId,
    NetexEntityFetcher<R, QuayId> fetcherFunction
  ) {
    if (!quayIdNotFoundCache.contains(quayId)) {
      R result = fetcherFunction.tryFetch(quayId);
      if (result == null) {
        quayIdNotFoundCache.add(quayId);
      }
      return result;
    }
    return null;
  }

  @Override
  public TransportModeAndSubMode getTransportModesForQuayId(QuayId quayId) {
    return stopPlaceCache
      .get(
        getQuay(quayId)
          .orElseThrow(() ->
            new IllegalStateException(
              "The quay does not exist, this should be validated beforehand"
            )
          )
          .stopPlaceId()
      )
      .transportModeAndSubMode();
  }

  @Override
  public QuayCoordinates getCoordinatesForQuayId(QuayId quayId) {
    return getQuay(quayId)
      .orElseThrow(() ->
        new IllegalStateException(
          "The quay does not exist, this should be validated beforehand"
        )
      )
      .quayCoordinates();
  }

  private Optional<SimpleQuay> getQuay(QuayId quayId) {
    SimpleQuay quayFromCache = quayCache.get(quayId);
    if (quayFromCache != null) {
      return Optional.of(quayFromCache);
    }
    Quay quayFromReadApi = tryFetchWithNotFoundCheck(quayId, quayFetcher);
    if (quayFromReadApi != null) {
      StopPlace stopPlaceFromReadApi = stopPlaceForQuayIdFetcher.tryFetch(
        quayId
      );
      StopPlaceId stopPlaceId = new StopPlaceId(stopPlaceFromReadApi.getId());
      SimpleQuay simpleQuay = new SimpleQuay(
        QuayCoordinates.of(quayFromReadApi),
        stopPlaceId
      );
      quayCache.put(quayId, simpleQuay);
      stopPlaceCache.put(
        stopPlaceId,
        new SimpleStopPlace(
          stopPlaceFromReadApi.getName().getValue(),
          TransportModeAndSubMode.of(stopPlaceFromReadApi)
        )
      );
      return Optional.of(simpleQuay);
    }
    return Optional.empty();
  }

  @Override
  public String getStopPlaceNameForQuayId(QuayId quayId) {
    return stopPlaceCache
      .get(
        getQuay(quayId)
          .orElseThrow(() ->
            new IllegalStateException(
              "The quay does not exist, this should be validated beforehand"
            )
          )
          .stopPlaceId()
      )
      .name();
  }

  @Override
  public void refreshCache() {
    stopPlaceResource.loadStopPlacesDataset();
    LOGGER.info(
      "Loaded {} stop places and {} quays from NeTEx dataset",
      stopPlaceResource.getStopPlaces().size(),
      stopPlaceResource.getQuays().size()
    );

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

    quayIdNotFoundCache.clear();

    LOGGER.info(
      "Updated cache with " + "{} stop places ids, " + "{} quays ids ",
      stopPlaceCache.size(),
      quayCache.size()
    );
  }
}
