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

import no.entur.antu.organisation.DefaultOrganisationRepository;
import no.entur.antu.organisation.OrganisationRepository;
import no.entur.antu.organisation.OrganisationResource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.Set;

@Configuration
public class OrganisationConfig {


    @Bean
    @Profile("!test")
    OrganisationResource organisationResource(@Value("${antu.organisation.registry.url}") String organisationRegistryUrl, WebClient orgRegisterClient) {
        return new OrganisationResource(organisationRegistryUrl, orgRegisterClient);
    }

    @Bean
    @Profile("!test")
    OrganisationRepository organisationRepository(OrganisationResource organisationResource, @Qualifier("organisationCache") Map<String, Set<String>> organisationCache) {
        return new DefaultOrganisationRepository(organisationResource, organisationCache);
    }
}
