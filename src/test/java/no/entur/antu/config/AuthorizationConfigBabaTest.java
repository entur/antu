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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import no.entur.antu.TestApp;
import org.entur.oauth2.JwtRoleAssignmentExtractor;
import org.entur.ror.permission.RemoteBabaRoleAssignmentExtractor;
import org.junit.jupiter.api.Test;
import org.rutebanken.helper.organisation.RoleAssignmentExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Verify the authorization wiring used in all deployed environments, where
 * antu.security.role.assignment.extractor is set to baba.
 */
@ActiveProfiles({ "test", "default", "in-memory-blobstore" })
@Import(TestConfig.class)
@DirtiesContext
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.NONE,
  classes = TestApp.class,
  properties = {
    "antu.security.role.assignment.extractor=baba",
    "user.permission.rest.service.url=https://notInUse/services/organisations/users",
    "ror.oauth2.client.audience=https://notInUse",
    "spring.cloud.gcp.pubsub.emulator-host=localhost:8085",
  }
)
class AuthorizationConfigBabaTest {

  @Autowired
  private ApplicationContext applicationContext;

  @Autowired
  private RoleAssignmentExtractor roleAssignmentExtractor;

  @Test
  void babaRoleAssignmentExtractorIsTheActiveExtractor() {
    assertInstanceOf(
      RemoteBabaRoleAssignmentExtractor.class,
      roleAssignmentExtractor
    );
    assertEquals(
      0,
      applicationContext.getBeanNamesForType(JwtRoleAssignmentExtractor.class)
        .length
    );
  }

  @Test
  void internalWebClientIsCreated() {
    assertNotNull(
      applicationContext.getBean("internalWebClient", WebClient.class)
    );
  }
}
