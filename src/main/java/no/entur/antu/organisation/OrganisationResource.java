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
 */

package no.entur.antu.organisation;


import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collection;

/**
 * REST-client to access the Organisation Register REST-service.
 */
public class OrganisationResource {

    private final WebClient webClient;

    public OrganisationResource(String organisationRegistryUrl, WebClient orgRegisterClient) {
        this.webClient = orgRegisterClient.mutate()
                .defaultHeader("Et-Client-Name", "entur-antu")
                .baseUrl(organisationRegistryUrl)
                .build();
    }

    public Collection<Organisation> getOrganisations() {

        return webClient.get()
                .retrieve()
                .bodyToFlux(Organisation.class)
                .collectList().block();

    }

}
