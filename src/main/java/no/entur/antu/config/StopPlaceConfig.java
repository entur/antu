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

import no.entur.antu.stop.DefaultStopPlaceRepository;
import no.entur.antu.stop.DefaultStopPlaceResource;
import no.entur.antu.stop.StopPlaceRepository;
import no.entur.antu.stop.StopPlaceResource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.Set;

@Configuration
public class StopPlaceConfig {


    @Bean
    @Profile("!test")
    StopPlaceResource stopPlaceResource(@Value("${antu.stop.registry.id.url}") String stopIdsEndpoint, WebClient.Builder webClientBuilder) {
        return new DefaultStopPlaceResource(stopIdsEndpoint, webClientBuilder);
    }

    @Bean
    @Profile("!test")
    StopPlaceRepository stopPlaceRepository(StopPlaceResource stopPlaceResource, @Qualifier("stopPlaceAndQuayCache") Map<String, Set<String>> stopPlaceCache) {
        return new DefaultStopPlaceRepository(stopPlaceResource, stopPlaceCache);
    }

}
