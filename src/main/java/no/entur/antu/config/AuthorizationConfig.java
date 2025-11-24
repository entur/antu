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

import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import no.entur.antu.security.AntuAuthorizationService;
import no.entur.antu.security.DefaultAntuAuthorizationService;
import org.entur.oauth2.AuthorizedWebClientBuilder;
import org.entur.oauth2.JwtRoleAssignmentExtractor;
import org.entur.ror.permission.RemoteBabaRoleAssignmentExtractor;
import org.rutebanken.helper.organisation.RoleAssignmentExtractor;
import org.rutebanken.helper.organisation.authorization.AuthorizationService;
import org.rutebanken.helper.organisation.authorization.DefaultAuthorizationService;
import org.rutebanken.helper.organisation.authorization.FullAccessAuthorizationService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configure authorization.
 */
@Configuration
public class AuthorizationConfig {

  @ConditionalOnProperty(
    value = "antu.security.role.assignment.extractor",
    havingValue = "jwt",
    matchIfMissing = true
  )
  @Bean
  public RoleAssignmentExtractor jwtRoleAssignmentExtractor() {
    return new JwtRoleAssignmentExtractor();
  }

  @ConditionalOnProperty(
    value = "antu.security.role.assignment.extractor",
    havingValue = "baba"
  )
  @Bean
  public RoleAssignmentExtractor babaRoleAssignmentExtractor(
    @Qualifier("internalWebClient") WebClient webClient,
    @Value("${user.permission.rest.service.url}") String url
  ) {
    return new RemoteBabaRoleAssignmentExtractor(webClient, url);
  }

  @ConditionalOnProperty(
    value = "antu.security.role.assignment.extractor",
    havingValue = "baba"
  )
  @Bean("internalWebClient")
  WebClient internalWebClient(
    WebClient.Builder webClientBuilder,
    OAuth2ClientProperties properties,
    @Value("${ror.oauth2.client.audience}") String audience
  ) {
    return new AuthorizedWebClientBuilder(webClientBuilder)
      .withOAuth2ClientProperties(properties)
      .withAudience(audience)
      .withClientRegistrationId("internal")
      .build();
  }

  @ConditionalOnProperty(
    value = "antu.security.authorization-service",
    havingValue = "token-based"
  )
  @Bean("authorizationService")
  public AuthorizationService<String> tokenBasedAuthorizationService(
    RoleAssignmentExtractor roleAssignmentExtractor
  ) {
    return new DefaultAuthorizationService<>(
      providerCodespace ->
        providerCodespace == null
          ? null
          : providerCodespace.toUpperCase(Locale.ROOT),
      roleAssignmentExtractor
    );
  }

  @ConditionalOnProperty(
    value = "antu.security.authorization-service",
    havingValue = "full-access"
  )
  @Bean("authorizationService")
  public AuthorizationService<Long> fullAccessAuthorizationService() {
    return new FullAccessAuthorizationService();
  }

  @Bean
  public AntuAuthorizationService antuAuthorizationService(
    AuthorizationService<String> authorizationService,
    AuthenticationManagerResolver<HttpServletRequest> resolver
  ) {
    return new DefaultAntuAuthorizationService(authorizationService, resolver);
  }
}
