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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class DefaultOrganisationRegistry implements OrganisationRegistry {

    private static final String REFERENCE_CODESPACE = "codeSpace";
    private static final String REFERENCE_NETEX_OPERATOR_IDS_WHITELIST = "netexOperatorIdsWhitelist";

    private final OrganisationResource organisationResource;

    private volatile Map<String, Set<String>> authorityIdWhitelistByCodespace;

    public DefaultOrganisationRegistry(OrganisationResource organisationResource) {
        this.organisationResource = organisationResource;
        this.authorityIdWhitelistByCodespace = new HashMap<>();
    }

    @Override
    public void refreshCache() {
        Collection<Organisation> organisations = organisationResource.getOrganisations();
        authorityIdWhitelistByCodespace = organisations.stream()
                .filter(organisation -> organisation.references.containsKey(REFERENCE_CODESPACE))
                .filter(organisation -> organisation.references.containsKey(REFERENCE_NETEX_OPERATOR_IDS_WHITELIST))
                .collect(Collectors.toMap(
                        organisation -> organisation.references.get(REFERENCE_CODESPACE),
                        organisation -> Arrays.stream(organisation.references.get(REFERENCE_NETEX_OPERATOR_IDS_WHITELIST).split(",")).collect(Collectors.toSet())));
    }

    @Override
    public Set<String> getWhitelistedAuthorityIds(String codespace) {
        Set<String> whitelistedIds = authorityIdWhitelistByCodespace.get(codespace);
        if (whitelistedIds == null) {
            return Collections.emptySet();
        }
        return Set.copyOf(whitelistedIds);
    }

}
