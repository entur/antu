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

import org.entur.oauth2.AuthorizedWebClientBuilder;
import org.entur.oauth2.JwtRoleAssignmentExtractor;
import org.entur.oauth2.RoRJwtDecoderBuilder;
import org.rutebanken.helper.organisation.RoleAssignmentExtractor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class OAuth2Config {

    @Bean
    public RoleAssignmentExtractor roleAssignmentExtractor() {
        return new JwtRoleAssignmentExtractor();
    }

    /**
     * Build a @{@link JwtDecoder} for RoR Auth0 domain.
     *
     * @return a @{@link JwtDecoder} for Auth0.
     */
    @Bean
    @Profile("!test")
    public JwtDecoder rorAuth0JwtDecoder(OAuth2ResourceServerProperties properties,
                                         @Value("${antu.oauth2.resourceserver.auth0.ror.jwt.audience}") String rorAuth0Audience,
                                         @Value("${antu.oauth2.resourceserver.auth0.ror.claim.namespace}") String rorAuth0ClaimNamespace) {

        String rorAuth0Issuer = properties.getJwt().getIssuerUri();
        return new RoRJwtDecoderBuilder().withIssuer(rorAuth0Issuer)
                .withAudience(rorAuth0Audience)
                .withAuth0ClaimNamespace(rorAuth0ClaimNamespace)
                .build();
    }


    @Bean
    @Profile("!test")
    WebClient webClient(WebClient.Builder webClientBuilder, OAuth2ClientProperties properties, @Value("${orgRegister.oauth2.client.audience}") String audience) {
        return new AuthorizedWebClientBuilder(webClientBuilder)
                .withOAuth2ClientProperties(properties)
                .withAudience(audience)
                .withClientRegistrationId("orgRegister")
                .build();
    }

}

