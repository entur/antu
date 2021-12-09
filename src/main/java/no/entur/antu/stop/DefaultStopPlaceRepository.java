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

import java.util.Collections;
import java.util.Set;

public class DefaultStopPlaceRepository implements StopPlaceRepository {

    private final StopPlaceResource stopPlaceResource;

    private Set<String> stopPlaceIds;
    private Set<String> quayIds;

    public DefaultStopPlaceRepository(StopPlaceResource stopPlaceResource) {
        this.stopPlaceResource = stopPlaceResource;
        this.stopPlaceIds = Collections.emptySet();
        this.quayIds = Collections.emptySet();
    }

    @Override
    public Set<String> getStopPlaceIds() {
        return stopPlaceIds;
    }

    @Override
    public Set<String> getQuayIds() {
        return quayIds;
    }

    @Override
    public void refreshCache() {
        stopPlaceIds = stopPlaceResource.getStopPlaceIds();
        quayIds = stopPlaceResource.getQuayIds();

    }


}
