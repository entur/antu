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

import java.util.function.Function;
import no.entur.antu.security.AuthorizationService;
import no.entur.antu.security.DefaultAuthorizationService;
import org.entur.oauth2.authorization.FullAccessUserContextService;
import org.entur.oauth2.authorization.OAuth2TokenUserContextService;
import org.entur.oauth2.authorization.UserContextService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configure authorization.
 */
@Configuration
public class AuthorizationConfig {

  @ConditionalOnProperty(
    value = "antu.security.user-context-service",
    havingValue = "token-based"
  )
  @Bean("userContextService")
  public UserContextService<String> tokenBasedUserContextService() {
    return new OAuth2TokenUserContextService<>(Function.identity());
  }

  @ConditionalOnProperty(
    value = "antu.security.user-context-service",
    havingValue = "full-access"
  )
  @Bean("userContextService")
  public UserContextService<Long> fullAccessUserContextService() {
    return new FullAccessUserContextService();
  }

  @Bean
  public AuthorizationService authorizationService(
    UserContextService<String> userContextService
  ) {
    return new DefaultAuthorizationService(userContextService);
  }
}
