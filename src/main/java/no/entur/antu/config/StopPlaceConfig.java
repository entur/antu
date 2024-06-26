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

import static no.entur.antu.config.cache.CacheConfig.COORDINATES_PER_QUAY_ID_CACHE;
import static no.entur.antu.config.cache.CacheConfig.QUAY_ID_NOT_FOUND_CACHE;
import static no.entur.antu.config.cache.CacheConfig.STOP_PLACE_AND_QUAY_CACHE;
import static no.entur.antu.config.cache.CacheConfig.STOP_PLACE_NAME_PER_QUAY_ID_CACHE;
import static no.entur.antu.config.cache.CacheConfig.TRANSPORT_MODES_FOR_QUAY_ID_CACHE;

import java.util.Map;
import java.util.Set;
import no.entur.antu.model.QuayCoordinates;
import no.entur.antu.model.QuayId;
import no.entur.antu.model.TransportModes;
import no.entur.antu.stop.DefaultStopPlaceRepository;
import no.entur.antu.stop.DefaultStopPlaceResource;
import no.entur.antu.stop.StopPlaceRepository;
import no.entur.antu.stop.fetcher.QuayFetcher;
import no.entur.antu.stop.fetcher.StopPlaceFetcher;
import no.entur.antu.stop.fetcher.StopPlaceForQuayIdFetcher;
import no.entur.antu.stop.loader.StopPlacesDatasetLoader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class StopPlaceConfig {

  private static final String ET_CLIENT_ID_HEADER = "ET-Client-ID";
  private static final String ET_CLIENT_NAME_HEADER = "ET-Client-Name";

  @Bean
  @Profile("!test")
  DefaultStopPlaceResource stopPlaceResource(
    StopPlacesDatasetLoader stopPlacesDatasetLoader
  ) {
    return new DefaultStopPlaceResource(stopPlacesDatasetLoader);
  }

  @Bean
  @Profile("!test")
  StopPlaceRepository stopPlaceRepository(
    @Qualifier(
      STOP_PLACE_AND_QUAY_CACHE
    ) Map<String, Set<String>> stopPlaceCache,
    StopPlaceFetcher stopPlaceFetcher,
    QuayFetcher quayFetcher,
    StopPlaceForQuayIdFetcher stopPlaceForQuayIdFetcher,
    @Qualifier(
      TRANSPORT_MODES_FOR_QUAY_ID_CACHE
    ) Map<QuayId, TransportModes> transportModesForQuayIdCache,
    @Qualifier(QUAY_ID_NOT_FOUND_CACHE) Set<QuayId> quayIdNotFoundCache,
    @Qualifier(
      COORDINATES_PER_QUAY_ID_CACHE
    ) Map<QuayId, QuayCoordinates> coordinatesPerQuayIdCache,
    @Qualifier(
      STOP_PLACE_NAME_PER_QUAY_ID_CACHE
    ) Map<QuayId, String> stopPlaceNamePerQuayIdCache,
    @Qualifier(
      "stopPlaceResource"
    ) DefaultStopPlaceResource defaultStopPlaceResource
  ) {
    return new DefaultStopPlaceRepository(
      defaultStopPlaceResource,
      stopPlaceCache,
      quayIdNotFoundCache,
      transportModesForQuayIdCache,
      coordinatesPerQuayIdCache,
      stopPlaceNamePerQuayIdCache,
      quayFetcher,
      stopPlaceFetcher,
      stopPlaceForQuayIdFetcher
    );
  }

  @Bean("stopPlaceWebClient")
  WebClient stopPlaceWebClient(
    @Value(
      "${stopplace.registry.url:https://api.dev.entur.io/stop-places/v1/read}"
    ) String stopPlaceRegistryUrl,
    @Value("${http.client.name:antu}") String clientName,
    @Value("${http.client.id:antu}") String clientId,
    ClientHttpConnector clientHttpConnector
  ) {
    return WebClient
      .builder()
      .baseUrl(stopPlaceRegistryUrl)
      .clientConnector(clientHttpConnector)
      .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML_VALUE)
      .defaultHeader(ET_CLIENT_NAME_HEADER, clientName)
      .defaultHeader(ET_CLIENT_ID_HEADER, clientId)
      .build();
  }
}
