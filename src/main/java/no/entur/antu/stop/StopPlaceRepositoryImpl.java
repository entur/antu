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

import no.entur.antu.exception.AntuException;
import no.entur.antu.stop.fetcher.NetexEntityFetcher;
import no.entur.antu.model.QuayId;
import no.entur.antu.model.StopPlaceId;
import no.entur.antu.model.TransportModes;
import no.entur.antu.model.TransportSubMode;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.StopPlace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

/**
 * StopPlaceRepository implementation using the new API exposed in Tiamat.
 */
public class StopPlaceRepositoryImpl implements StopPlaceRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(StopPlaceRepositoryImpl.class);
    public static final String STOP_PLACE_CACHE_KEY = "stopPlaceCache";
    public static final String QUAY_CACHE_KEY = "quayCache";

    private final StopPlaceResource stopPlaceResource;
    private final Map<String, Set<String>> stopPlaceCache;
    private final Set<QuayId> quayIdNotFoundCache;
    private final Map<QuayId, TransportModes> transportModesForQuayIdCache;
    private final NetexEntityFetcher<Quay, QuayId> quayFetcher;
    private final NetexEntityFetcher<StopPlace, StopPlaceId> stopPlaceFetcher;
    private final NetexEntityFetcher<StopPlace, QuayId> stopPlaceForQuayIdFetcher;

    public StopPlaceRepositoryImpl(StopPlaceResource stopPlaceResource,
                                   Map<String, Set<String>> stopPlaceCache,
                                   Set<QuayId> quayIdNotFoundCache,
                                   Map<QuayId, TransportModes> transportModesForQuayIdCache,
                                   NetexEntityFetcher<Quay, QuayId> quayFetcher,
                                   NetexEntityFetcher<StopPlace, StopPlaceId> stopPlaceFetcher,
                                   NetexEntityFetcher<StopPlace, QuayId> stopPlaceForQuayIdFetcher) {
        this.stopPlaceResource = stopPlaceResource;
        this.stopPlaceCache = stopPlaceCache;
        this.transportModesForQuayIdCache = transportModesForQuayIdCache;
        this.quayIdNotFoundCache = quayIdNotFoundCache;
        this.quayFetcher = quayFetcher;
        this.stopPlaceFetcher = stopPlaceFetcher;
        this.stopPlaceForQuayIdFetcher = stopPlaceForQuayIdFetcher;
    }

    @Override
    public boolean hasStopPlaceId(StopPlaceId stopPlaceId) {
        Set<String> stopPlaceIds = stopPlaceCache.get(STOP_PLACE_CACHE_KEY);
        if (stopPlaceIds == null) {
            throw new AntuException("Stop place ids cache not found");
        }
        boolean idFoundInCache = stopPlaceIds.stream().anyMatch(id -> id.equals(stopPlaceId.id()));
        return idFoundInCache || stopPlaceFetcher.tryFetch(stopPlaceId) != null;
    }

    @Override
    public boolean hasQuayId(QuayId quayId) {
        Set<String> quayIds = stopPlaceCache.get(QUAY_CACHE_KEY);
        if (quayIds == null) {
            throw new AntuException("Quay ids cache not found");
        }
        boolean idFoundInCache = quayIds.stream().anyMatch(id -> id.equals(quayId.id()));
        return idFoundInCache || tryFetchWithNotFoundCheck(quayId, quayFetcher) != null;
    }

    private <R> R tryFetchWithNotFoundCheck(QuayId quayId,
                                            NetexEntityFetcher<R, QuayId> fetcherFunction) {
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
    public TransportModes getTransportModesForQuayId(QuayId quayId) {
        // Intentionally not using the "Map.computeIfAbsent()", because we need
        // to call the readApi from computeIfAbsent, which is somewhat long-running
        // operation, which holds the RedissonLock.lock, causing java.lang.InterruptedException.
        TransportModes transportModes = transportModesForQuayIdCache.get(quayId);
        if (transportModes == null) {
            StopPlace stopPlace = tryFetchWithNotFoundCheck(quayId, stopPlaceForQuayIdFetcher);
            if (stopPlace != null) {
                TransportModes transportModesFromReadApi = new TransportModes(
                        stopPlace.getTransportMode(),
                        TransportSubMode.from(stopPlace).orElse(null)
                );
                transportModesForQuayIdCache.put(quayId, transportModesFromReadApi);
                return transportModesFromReadApi;
            }
        }
        return transportModes;
    }

    @Override
    public void refreshCache() {
        stopPlaceResource.loadStopPlacesDataset();
        stopPlaceCache.put(STOP_PLACE_CACHE_KEY, stopPlaceResource.getStopPlaceIds());
        stopPlaceCache.put(QUAY_CACHE_KEY, stopPlaceResource.getQuayIds());
        transportModesForQuayIdCache.putAll(stopPlaceResource.getTransportModesPerQuayId());
        quayIdNotFoundCache.clear();

        LOGGER.info("Updated cache with " +
                        "{} stop places ids, " +
                        "{} quays ids, " +
                        "{} transport modes per quay id",
                stopPlaceCache.get(STOP_PLACE_CACHE_KEY).size(),
                stopPlaceCache.get(QUAY_CACHE_KEY).size(),
                transportModesForQuayIdCache.size());
    }
}
