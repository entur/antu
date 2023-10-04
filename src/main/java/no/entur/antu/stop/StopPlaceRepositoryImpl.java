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
import no.entur.antu.stop.model.QuayId;
import no.entur.antu.stop.model.StopPlaceId;
import no.entur.antu.stop.model.TransportSubMode;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.StopPlace;
import org.rutebanken.netex.model.VehicleModeEnumeration;
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
    private final Map<QuayId, VehicleModeEnumeration> transportModePerQuayIdCache;
    private final Map<QuayId, TransportSubMode> transportSubModePerQuayIdCache;
    private final NetexEntityFetcher<Quay, QuayId> quayFetcher;
    private final NetexEntityFetcher<StopPlace, StopPlaceId> stopPlaceFetcher;
    private final NetexEntityFetcher<StopPlace, QuayId> stopPlaceForQuayIdFetcher;

    public StopPlaceRepositoryImpl(StopPlaceResource stopPlaceResource,
                                   Map<String, Set<String>> stopPlaceCache,
                                   Map<QuayId, VehicleModeEnumeration> transportModePerQuayIdCache,
                                   Map<QuayId, TransportSubMode> transportSubModePerQuayIdCache,
                                   NetexEntityFetcher<Quay, QuayId> quayFetcher,
                                   NetexEntityFetcher<StopPlace, StopPlaceId> stopPlaceFetcher,
                                   NetexEntityFetcher<StopPlace, QuayId> stopPlaceForQuayIdFetcher) {
        this.stopPlaceResource = stopPlaceResource;
        this.stopPlaceCache = stopPlaceCache;
        this.transportModePerQuayIdCache = transportModePerQuayIdCache;
        this.transportSubModePerQuayIdCache = transportSubModePerQuayIdCache;
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
        return idFoundInCache || quayFetcher.tryFetch(quayId) != null;
    }

    @Override
    public VehicleModeEnumeration getTransportModeForQuayId(QuayId quayId) {
        return transportModePerQuayIdCache.computeIfAbsent(
                quayId,
                id -> stopPlaceForQuayIdFetcher.tryFetch(id).getTransportMode());
    }

    @Override
    public TransportSubMode getTransportSubModeForQuayId(QuayId quayId) {
        return transportSubModePerQuayIdCache.computeIfAbsent(
                quayId,
                id -> TransportSubMode
                        .from(stopPlaceForQuayIdFetcher.tryFetch(id))
                        .orElse(null)
        );
    }

    @Override
    public void refreshCache() {
        stopPlaceResource.loadStopPlacesDataset();
        stopPlaceCache.put(STOP_PLACE_CACHE_KEY, stopPlaceResource.getStopPlaceIds());
        stopPlaceCache.put(QUAY_CACHE_KEY, stopPlaceResource.getQuayIds());
        transportModePerQuayIdCache.putAll(stopPlaceResource.getTransportModesPerQuayId());
        transportSubModePerQuayIdCache.putAll(stopPlaceResource.getTransportSubModesPerQuayId());

        LOGGER.info("Updated cache with " +
                     "{} stop places ids, " +
                     "{} quays ids, " +
                     "{} transport modes per quay id, " +
                     "{} transport sub modes per quay id",
                stopPlaceCache.get(STOP_PLACE_CACHE_KEY).size(),
                stopPlaceCache.get(QUAY_CACHE_KEY).size(),
                transportModePerQuayIdCache.size(),
                transportSubModePerQuayIdCache.size());
    }
}
