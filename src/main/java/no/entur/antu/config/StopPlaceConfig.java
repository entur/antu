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

import static no.entur.antu.stop.DefaultStopPlaceRepository.QUAY_CACHE;
import static no.entur.antu.stop.DefaultStopPlaceRepository.STOP_PLACE_CACHE;

import java.util.Map;
import no.entur.antu.stop.DefaultStopPlaceRepository;
import no.entur.antu.stop.DefaultStopPlaceResource;
import no.entur.antu.stop.StopPlaceDatasetVersionRepository;
import no.entur.antu.stop.StopPlaceRepositoryLoader;
import no.entur.antu.stop.changelog.DefaultStopPlaceRepositoryUpdater;
import no.entur.antu.stop.changelog.StopPlaceRepositoryUpdater;
import no.entur.antu.stop.loader.StopPlacesDatasetLoader;
import no.entur.antu.stop.registry.NotFoundIdCache;
import no.entur.antu.stop.registry.StopPlaceRegistry;
import no.entur.antu.stop.registry.TiamatStopPlaceRegistry;
import org.entur.netex.validation.validator.model.QuayId;
import org.entur.netex.validation.validator.model.SimpleQuay;
import org.entur.netex.validation.validator.model.SimpleStopPlace;
import org.entur.netex.validation.validator.model.StopPlaceId;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class StopPlaceConfig {

  private static final Logger LOGGER = LoggerFactory.getLogger(
    StopPlaceConfig.class
  );

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
  StopPlaceRepositoryLoader stopPlaceRepository(
    @Qualifier(
      STOP_PLACE_CACHE
    ) Map<StopPlaceId, SimpleStopPlace> stopPlaceCache,
    @Qualifier(QUAY_CACHE) Map<QuayId, SimpleQuay> quayCache,
    @Qualifier(
      "stopPlaceResource"
    ) DefaultStopPlaceResource defaultStopPlaceResource,
    StopPlaceRegistry stopPlaceRegistry
  ) {
    return new DefaultStopPlaceRepository(
      defaultStopPlaceResource,
      stopPlaceCache,
      quayCache,
      stopPlaceRegistry
    );
  }

  @Bean
  StopPlaceDatasetVersionRepository stopPlaceDatasetVersionRepository(
    RedissonClient redissonClient
  ) {
    return new StopPlaceDatasetVersionRepository(redissonClient);
  }

  /**
   * Single id lookups for what the dataset import does not cover yet. Without a registry URL the
   * lookup is off and an id missing from the cache is reported as unresolved, as it was before.
   */
  @Bean
  @Profile("!test")
  StopPlaceRegistry stopPlaceRegistry(
    @Value("${antu.stop.registry.url:}") String registryUrl,
    @Value("${antu.stop.registry.client.name:entur-antu}") String clientName,
    NotFoundIdCache notFoundIdCache
  ) {
    if (registryUrl.isBlank()) {
      LOGGER.warn(
        "No stop place registry URL configured: references to stop places missing from the cache " +
        "will be reported as unresolved"
      );
      return StopPlaceRegistry.disabled();
    }
    WebClient webClient = WebClient
      .builder()
      .baseUrl(registryUrl)
      .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML_VALUE)
      .defaultHeader(ET_CLIENT_NAME_HEADER, clientName)
      .build();
    return new TiamatStopPlaceRegistry(webClient, notFoundIdCache);
  }

  @Profile("!stop-place-changelog")
  @Bean
  StopPlaceRepositoryUpdater stopPlaceRepositoryUpdater(
    StopPlaceRepositoryLoader stopPlaceRepositoryLoader
  ) {
    return new DefaultStopPlaceRepositoryUpdater(stopPlaceRepositoryLoader);
  }
}
