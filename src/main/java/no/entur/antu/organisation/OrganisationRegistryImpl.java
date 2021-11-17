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

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class OrganisationRegistryImpl implements OrganisationRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrganisationRegistryImpl.class);
    private static final String REFERENCE_CODESPACE = "codeSpace";
    private static final String REFERENCE_NETEX_OPERATOR_IDS_WHITELIST = "netexOperatorIdsWhitelist";

    private final String organisationRegistryUrl;
    private final RestTemplate restTemplate;

    private volatile Map<String, Set<String>> authorityIdWhitelistByCodespace;


    public OrganisationRegistryImpl(String organisationRegistryUrl) {
        this.organisationRegistryUrl = organisationRegistryUrl;
        this.restTemplate = createRestTemplate();
        this.authorityIdWhitelistByCodespace = new HashMap<>();
    }

    @Override
    public void refreshCache() {
        try {
            ResponseEntity<Organisation[]> organisationsResponseEntity = restTemplate.getForEntity(organisationRegistryUrl,
                    Organisation[].class);

            if(organisationsResponseEntity.getBody() == null) {
                return;
            }
            authorityIdWhitelistByCodespace = Arrays.stream(organisationsResponseEntity.getBody())
                    .filter(organisation -> organisation.references.containsKey(REFERENCE_CODESPACE))
                    .filter(organisation -> organisation.references.containsKey(REFERENCE_NETEX_OPERATOR_IDS_WHITELIST))
                    .collect(Collectors.toMap(
                            organisation -> organisation.references.get(REFERENCE_CODESPACE),
                            organisation -> Arrays.stream(organisation.references.get(REFERENCE_NETEX_OPERATOR_IDS_WHITELIST).split(",")).collect(Collectors.toSet())));

        } catch (HttpClientErrorException ex) {
            LOGGER.warn("Exception while trying to fetch organisations: " + ex.getMessage(), ex);
        }
    }

    @Override
    public Set<String> getWhitelistedAuthorityIds(String codespace) {
        Set<String> whitelistedIds = authorityIdWhitelistByCodespace.get(codespace);
        if(whitelistedIds == null) {
            return Collections.emptySet();
        }
        return Set.copyOf(whitelistedIds);
    }

    private RestTemplate createRestTemplate() {
        CloseableHttpClient clientBuilder = HttpClientBuilder.create().build();
        return new RestTemplate(new HttpComponentsClientHttpRequestFactory(clientBuilder));
    }

}
