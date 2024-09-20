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
import java.util.function.Function;
import java.util.stream.Collectors;

import no.entur.antu.model.QuayCoordinates;
import no.entur.antu.model.QuayId;
import no.entur.antu.model.StopPlaceId;
import no.entur.antu.model.TransportModeAndSubMode;
import no.entur.antu.stop.fetcher.NetexEntityFetcher;
import org.rutebanken.netex.model.MultilingualString;
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
  public static final String STOP_PLACE_ID_CACHE = "stopPlaceCache";
  public static final String QUAY_ID_CACHE = "quayCache";

  private final StopPlaceResource stopPlaceResource;
  private final Set<QuayId> quayIds;
  private final Set<StopPlaceId> stopPlaceIds;
  private final Set<QuayId> quayIdNotFoundCache;
  private final Map<QuayId, TransportModeAndSubMode> transportModesForQuayIdCache;
  private final Map<QuayId, QuayCoordinates> coordinatesPerQuayIdCache;
  private final Map<QuayId, String> stopPlaceNamePerQuayIdCache;
  private final NetexEntityFetcher<Quay, QuayId> quayFetcher;
  private final NetexEntityFetcher<StopPlace, StopPlaceId> stopPlaceFetcher;
  private final NetexEntityFetcher<StopPlace, QuayId> stopPlaceForQuayIdFetcher;

  public DefaultStopPlaceRepository(
          StopPlaceResource stopPlaceResource,
          Set<StopPlaceId> stopPlaceIds,
          Set<QuayId> quayIds,
          Set<QuayId> quayIdNotFoundCache,
          Map<QuayId, TransportModeAndSubMode> transportModesForQuayIdCache,
          Map<QuayId, QuayCoordinates> coordinatesPerQuayIdCache,
          Map<QuayId, String> stopPlaceNamePerQuayIdCache,
          NetexEntityFetcher<Quay, QuayId> quayFetcher,
          NetexEntityFetcher<StopPlace, StopPlaceId> stopPlaceFetcher,
          NetexEntityFetcher<StopPlace, QuayId> stopPlaceForQuayIdFetcher
  ) {
    this.stopPlaceResource = stopPlaceResource;
    this.quayIds = Objects.requireNonNull(quayIds);
    this.stopPlaceIds = Objects.requireNonNull(stopPlaceIds);
    this.transportModesForQuayIdCache = transportModesForQuayIdCache;
    this.quayIdNotFoundCache = quayIdNotFoundCache;
    this.coordinatesPerQuayIdCache = coordinatesPerQuayIdCache;
    this.stopPlaceNamePerQuayIdCache = stopPlaceNamePerQuayIdCache;
    this.quayFetcher = quayFetcher;
    this.stopPlaceFetcher = stopPlaceFetcher;
    this.stopPlaceForQuayIdFetcher = stopPlaceForQuayIdFetcher;
  }

  @Override
  public boolean hasStopPlaceId(StopPlaceId stopPlaceId) {
    if (stopPlaceIds.contains(stopPlaceId)) {
      return true;
    }

    StopPlace stopPlace = stopPlaceFetcher.tryFetch(stopPlaceId);
    if (stopPlace != null) {
      stopPlaceIds.add(stopPlaceId);
      return true;
    }
    return false;
  }

  @Override
  public boolean hasQuayId(QuayId quayId) {
    if (quayIds.contains(quayId)) {
      return true;
    }
    if (coordinatesPerQuayIdCache.containsKey(quayId)) {
      return true;
    }

    Quay quay = tryFetchWithNotFoundCheck(quayId, quayFetcher);
    if (quay != null) {
      quayIds.add(quayId);
      coordinatesPerQuayIdCache.put(quayId, QuayCoordinates.of(quay));
      StopPlace stopPlace = stopPlaceForQuayIdFetcher.tryFetch(quayId);
      if (stopPlace != null) {
        transportModesForQuayIdCache.put(
          quayId,
          TransportModeAndSubMode.of(stopPlace)
        );
        stopPlaceNamePerQuayIdCache.put(quayId, stopPlace.getName().getValue());
      }
      return true;
    }
    return false;
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
    return getDataForQuayId(
      quayId,
      transportModesForQuayIdCache,
      TransportModeAndSubMode::of
    );
  }

  @Override
  public QuayCoordinates getCoordinatesForQuayId(QuayId quayId) {
    return getDataForQuayId(
      quayId,
      coordinatesPerQuayIdCache,
      QuayCoordinates::of
    );
  }

  @Override
  public String getStopPlaceNameForQuayId(QuayId quayId) {
    return getDataForQuayId(
      quayId,
      stopPlaceNamePerQuayIdCache,
      stopPlace ->
        Optional
          .of(stopPlace.getName())
          .map(MultilingualString::getValue)
          .orElse(null)
    );
  }

  public <D> D getDataForQuayId(
    QuayId quayId,
    Map<QuayId, D> cache,
    Function<StopPlace, D> dataOfStopPlace
  ) {
    // Intentionally not using the "Map.computeIfAbsent()", because we need
    // to call the readApi from computeIfAbsent, which is somewhat long-running
    // operation, which holds the RedissonLock.lock, causing java.lang.InterruptedException.
    D data = cache.get(quayId);
    if (data == null) {
      StopPlace stopPlace = tryFetchWithNotFoundCheck(
        quayId,
        stopPlaceForQuayIdFetcher
      );
      if (stopPlace != null) {
        D dataFromReadApi = dataOfStopPlace.apply(stopPlace);
        cache.put(quayId, dataFromReadApi);
        return dataFromReadApi;
      }
    }
    return data;
  }

  @Override
  public void refreshCache() {
    stopPlaceResource.loadStopPlacesDataset();
    LOGGER.info("Loaded {} stop places and {} quays from NeTEx dataset", stopPlaceResource
            .getStopPlaceIds().size(), stopPlaceResource
            .getQuayIds().size());

    Set<StopPlaceId> newStopPlaceIds = stopPlaceResource
      .getStopPlaceIds()
      .stream()
      .map(StopPlaceId::new)
      .collect(Collectors.toUnmodifiableSet());

    // TODO: Keep warning logs for testing, remove them later
    if (newStopPlaceIds.isEmpty()) {
      LOGGER.warn("Unable to refresh cache, no stop place ids found");
    } else {
      // TODO redisson bug: cannot use retainAll on the entire collection https://github.com/redisson/redisson/issues/6186
      stopPlaceIds.removeIf(stopPlaceId -> !newStopPlaceIds.contains(stopPlaceId));
      stopPlaceIds.addAll(newStopPlaceIds);
    }
    LOGGER.info("Updated Stop place cache");

    Set<QuayId> newQuayIds = stopPlaceResource
      .getQuayIds()
      .stream()
      .map(QuayId::new)
      .collect(Collectors.toUnmodifiableSet());
    if (newQuayIds.isEmpty()) {
      LOGGER.warn("Unable to refresh cache, no quay ids found");
    } else {
      // TODO redisson bug: cannot use retainAll on the entire collection https://github.com/redisson/redisson/issues/6186
      quayIds.removeIf(quayId -> !newQuayIds.contains(quayId));
      quayIds.addAll(newQuayIds);
    }
    LOGGER.info("Updated Quay cache");

    Map<QuayId, TransportModeAndSubMode> transportModesPerQuayId =
      stopPlaceResource.getTransportModesPerQuayId();
    if (transportModesPerQuayId == null || transportModesPerQuayId.isEmpty()) {
      LOGGER.warn("Unable to refresh cache, no transport modes found");
    } else {
      transportModesForQuayIdCache.putAll(transportModesPerQuayId);
    }

    Map<QuayId, QuayCoordinates> coordinatesPerQuayId =
      stopPlaceResource.getCoordinatesPerQuayId();
    if (coordinatesPerQuayId == null || coordinatesPerQuayId.isEmpty()) {
      LOGGER.warn("Unable to refresh cache, no coordinates found");
    } else {
      coordinatesPerQuayIdCache.putAll(coordinatesPerQuayId);
    }

    quayIdNotFoundCache.clear();

    LOGGER.info(
      "Updated cache with " +
      "{} stop places ids, " +
      "{} quays ids, " +
      "{} transport modes per quay id, " +
      "{} coordinates per quay id",
      stopPlaceIds.size(),
      quayIds.size(),
      transportModesForQuayIdCache.size(),
      coordinatesPerQuayIdCache.size()
    );
  }
}
