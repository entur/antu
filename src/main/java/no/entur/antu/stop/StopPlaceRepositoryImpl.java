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

    private final NetexEntityFetcher<Quay, String> quayFetcher;
    private final NetexEntityFetcher<StopPlace, String> stopPlaceFetcher;

    public StopPlaceRepositoryImpl(StopPlaceResource stopPlaceResource,
                                   Map<String, Set<String>> stopPlaceCache,
                                   NetexEntityFetcher<Quay, String> quayFetcher,
                                   NetexEntityFetcher<StopPlace, String> stopPlaceFetcher) {
        this.stopPlaceResource = stopPlaceResource;
        this.stopPlaceCache = stopPlaceCache;
        this.quayFetcher = quayFetcher;
        this.stopPlaceFetcher = stopPlaceFetcher;
    }

    @Override
    public boolean hasStopPlaceId(String stopPlaceId) {
        Set<String> stopPlaceIds = stopPlaceCache.get(STOP_PLACE_CACHE_KEY);
        if (stopPlaceIds == null) {
            throw new AntuException("Stop place ids cache not found");
        }
        boolean idFoundInCache = stopPlaceIds.stream().anyMatch(id -> id.equals(stopPlaceId));
        return idFoundInCache || stopPlaceFetcher.tryFetch(stopPlaceId) != null;
    }

    @Override
    public boolean hasQuayId(String quayId) {
        Set<String> quayIds = stopPlaceCache.get(QUAY_CACHE_KEY);
        if (quayIds == null) {
            throw new AntuException("Quay ids cache not found");
        }
        boolean idFoundInCache = quayIds.stream().anyMatch(id -> id.equals(quayId));
        return idFoundInCache || quayFetcher.tryFetch(quayId) != null;
    }

    @Override
    public void refreshCache() {
        stopPlaceResource.loadStopPlacesDataset();
        stopPlaceCache.put(STOP_PLACE_CACHE_KEY, stopPlaceResource.getStopPlaceIds());
        stopPlaceCache.put(QUAY_CACHE_KEY, stopPlaceResource.getQuayIds());

        LOGGER.debug("Updated stop places and quays cache. Cache now has {} stop places and {} quays",
                stopPlaceCache.get(STOP_PLACE_CACHE_KEY).size(),
                stopPlaceCache.get(QUAY_CACHE_KEY).size());
    }
}
