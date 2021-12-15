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

import javax.cache.Cache;
import java.util.Set;

public class DefaultStopPlaceRepository implements StopPlaceRepository {

    public static final String STOP_PLACE_CACHE_KEY = "stopPlaceCache";
    public static final String QUAY_CACHE_KEY = "quayCache";

    private final StopPlaceResource stopPlaceResource;
    private final Cache<String, Set<String>> stopPlaceCache;

    public DefaultStopPlaceRepository(StopPlaceResource stopPlaceResource, Cache<String, Set<String>> stopPlaceCache) {
        this.stopPlaceResource = stopPlaceResource;
        this.stopPlaceCache = stopPlaceCache;
    }

    @Override
    public Set<String> getStopPlaceIds() {
        Set<String> stopPlaceIds = stopPlaceCache.get(STOP_PLACE_CACHE_KEY);
        if(stopPlaceIds == null) {
            throw new AntuException("Stop place ids cache not found");
        }
        return stopPlaceIds;
    }

    @Override
    public Set<String> getQuayIds() {
        Set<String> quayIds = stopPlaceCache.get(QUAY_CACHE_KEY);
        if(quayIds == null) {
            throw new AntuException("Quay ids cache not found");
        }
        return quayIds;
    }

    @Override
    public void refreshCache() {
        stopPlaceCache.put(STOP_PLACE_CACHE_KEY, stopPlaceResource.getStopPlaceIds());
        stopPlaceCache.put(QUAY_CACHE_KEY, stopPlaceResource.getQuayIds());
    }


}
