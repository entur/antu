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
 *
 */

package no.entur.antu.config;

import no.entur.antu.stop.StopPlaceResourceImpl;
import no.entur.antu.stop.StopPlaceRepositoryImpl;
import no.entur.antu.stop.StopPlaceRepository;
import no.entur.antu.stop.fetcher.QuayFetcher;
import no.entur.antu.stop.fetcher.StopPlaceFetcher;
import no.entur.antu.stop.fetcher.StopPlaceForQuayIdFetcher;
import no.entur.antu.stop.loader.StopPlacesDatasetLoader;
import no.entur.antu.model.QuayId;
import no.entur.antu.model.TransportModes;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.Map;
import java.util.Set;

import static no.entur.antu.config.CacheConfig.QUAY_ID_NOT_FOUND_CACHE;
import static no.entur.antu.config.CacheConfig.TRANSPORT_MODES_FOR_QUAY_ID_CACHE;

@Configuration
public class StopPlaceConfig {

    @Bean
    @Profile("!test")
    StopPlaceResourceImpl stopPlaceResource(StopPlacesDatasetLoader stopPlacesDatasetLoader) {
        return new StopPlaceResourceImpl(stopPlacesDatasetLoader);
    }

    @Bean
    @Profile("!test")
    StopPlaceRepository stopPlaceRepository(@Qualifier("stopPlaceAndQuayCache")
                                            Map<String, Set<String>> stopPlaceCache,
                                            StopPlaceFetcher stopPlaceFetcher,
                                            QuayFetcher quayFetcher,
                                            StopPlaceForQuayIdFetcher stopPlaceForQuayIdFetcher,
                                            @Qualifier(TRANSPORT_MODES_FOR_QUAY_ID_CACHE)
                                            Map<QuayId, TransportModes> transportModesForQuayIdCache,
                                            @Qualifier(QUAY_ID_NOT_FOUND_CACHE)
                                            Set<QuayId> quayIdNotFoundCache,
                                            @Qualifier("stopPlaceResource")
                                            StopPlaceResourceImpl stopPlaceResourceImpl) {
        return new StopPlaceRepositoryImpl(
                stopPlaceResourceImpl,
                stopPlaceCache,
                quayIdNotFoundCache,
                transportModesForQuayIdCache,
                quayFetcher,
                stopPlaceFetcher,
                stopPlaceForQuayIdFetcher
        );
    }
}
