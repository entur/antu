/*
 *
 *  * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 *  * the European Commission - subsequent versions of the EUPL (the "Licence");
 *  * You may not use this work except in compliance with the Licence.
 *  * You may obtain a copy of the Licence at:
 *  *
 *  *   https://joinup.ec.europa.eu/software/page/eupl
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the Licence is distributed on an "AS IS" basis,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the Licence for the specific language governing permissions and
 *  * limitations under the Licence.
 *  *
 *
 */

package no.entur.antu;/*
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

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import jakarta.annotation.PostConstruct;
import org.apache.camel.EndpointInject;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.camel.test.spring.junit5.UseAdviceWith;
import org.entur.pubsub.base.EnturGooglePubSubAdmin;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.rutebanken.helper.storage.repository.InMemoryBlobStoreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PubSubEmulatorContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@CamelSpringBootTest
@UseAdviceWith
@ActiveProfiles(
  { "test", "default", "in-memory-blobstore", "google-pubsub-autocreate" }
)
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public abstract class AntuRouteBuilderIntegrationTestBase {

  private static PubSubEmulatorContainer pubsubEmulator;

  @Autowired
  private EnturGooglePubSubAdmin enturGooglePubSubAdmin;

  @Value("${blobstore.gcs.antu.exchange.container.name}")
  private String antuExchangeContainerName;

  @Value("${blobstore.gcs.antu.container.name}")
  private String antuContainerName;

  @Autowired
  protected ModelCamelContext context;

  @Autowired
  protected PubSubTemplate pubSubTemplate;

  @Autowired
  protected InMemoryBlobStoreRepository antuExchangeInMemoryBlobStoreRepository;

  @Autowired
  protected InMemoryBlobStoreRepository antuInMemoryBlobStoreRepository;

  @EndpointInject("mock:sink")
  protected MockEndpoint sink;

  @PostConstruct
  void initInMemoryBlobStoreRepositories() {
    antuExchangeInMemoryBlobStoreRepository.setContainerName(
      antuExchangeContainerName
    );
    antuInMemoryBlobStoreRepository.setContainerName(antuContainerName);
  }

  @BeforeAll
  public static void init() {
    pubsubEmulator =
            new PubSubEmulatorContainer(
                    DockerImageName.parse(
                            "gcr.io/google.com/cloudsdktool/cloud-sdk:emulators"
                    )
            );
    pubsubEmulator.start();
  }

  @AfterAll
  public static void tearDown() {
    pubsubEmulator.stop();
  }

  @AfterEach
  public void teardown() {
    enturGooglePubSubAdmin.deleteAllSubscriptions();
  }

  @AfterEach
  void stopContext() {
    context.stop();
  }

  @DynamicPropertySource
  static void emulatorProperties(DynamicPropertyRegistry registry) {
    registry.add(
      "spring.cloud.gcp.pubsub.emulator-host",
      pubsubEmulator::getEmulatorEndpoint
    );
    registry.add(
      "camel.component.google-pubsub.endpoint",
      pubsubEmulator::getEmulatorEndpoint
    );
  }
}
