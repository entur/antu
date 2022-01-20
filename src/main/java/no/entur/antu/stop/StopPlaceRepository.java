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

import java.util.Set;

/**
 * A repository to store and cache the stop place and quay ids retrieved from the National Stop Register.
 */
public interface StopPlaceRepository {


    /**
     * Return all stop place ids present in the cache.
     * @return all stop place ids present in the cache.
     */
    Set<String> getStopPlaceIds();

    /**
     * Return all quay ids present in the cache.
     * @return all quay ids present in the cache.
     */
    Set<String> getQuayIds();

    /**
     * Refresh the cache with data retrieved from the Stop Place Register.
     */
    void refreshCache();

}
