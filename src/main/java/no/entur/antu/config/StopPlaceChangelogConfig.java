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

import no.entur.antu.stop.StopPlaceRepositoryLoader;
import no.entur.antu.stop.changelog.AntuPublicationTimeRecordFilterStrategy;
import no.entur.antu.stop.changelog.AntuStopPlaceChangeLogListener;
import no.entur.antu.stop.changelog.ChangelogStopPlaceRepositoryUpdater;
import no.entur.antu.stop.changelog.RedisChangelogUpdateTimestampRepository;
import no.entur.antu.stop.changelog.StopPlaceRepositoryUpdater;
import org.redisson.api.RedissonClient;
import org.rutebanken.helper.stopplace.changelog.StopPlaceChangelog;
import org.rutebanken.helper.stopplace.changelog.kafka.ChangelogConsumerController;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("stop-place-changelog")
@Configuration
public class StopPlaceChangelogConfig {

  @Bean("publicationTimeRecordFilterStrategy")
  public AntuPublicationTimeRecordFilterStrategy customFilter() {
    return new AntuPublicationTimeRecordFilterStrategy();
  }

  @Bean
  public RedisChangelogUpdateTimestampRepository changelogUpdateTimestampRepository(
    RedissonClient redissonClient
  ) {
    return new RedisChangelogUpdateTimestampRepository(redissonClient);
  }

  @Bean
  public AntuStopPlaceChangeLogListener antuStopPlaceChangeLogListener(
    StopPlaceRepositoryLoader stopPlaceRepositoryLoader,
    RedisChangelogUpdateTimestampRepository changelogUpdateTimestampRepository
  ) {
    return new AntuStopPlaceChangeLogListener(
      stopPlaceRepositoryLoader,
      changelogUpdateTimestampRepository
    );
  }

  @Bean
  StopPlaceRepositoryUpdater stopPlaceRepositoryUpdater(
    StopPlaceRepositoryLoader stopPlaceRepositoryLoader,
    AntuPublicationTimeRecordFilterStrategy antuPublicationTimeRecordFilterStrategy,
    RedisChangelogUpdateTimestampRepository changelogUpdateTimestampRepository,
    ChangelogConsumerController changelogConsumerController,
    StopPlaceChangelog stopPlaceChangelog,
    AntuStopPlaceChangeLogListener antuStopPlaceChangeLogListener
  ) {
    return new ChangelogStopPlaceRepositoryUpdater(
      stopPlaceRepositoryLoader,
      antuPublicationTimeRecordFilterStrategy,
      changelogUpdateTimestampRepository,
      changelogConsumerController,
      stopPlaceChangelog,
      antuStopPlaceChangeLogListener
    );
  }
}
