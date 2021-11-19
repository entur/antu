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

package no.entur.antu.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;


public class DefaultProviderRepository implements ProviderRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultProviderRepository.class);

    private final ProviderResource providerResource;
    // volatile read-only access to the unmodifiable map is thread-safe as long as the values are not modified after the map creation
    private volatile Map<Long, Provider> providersById;

    public DefaultProviderRepository(ProviderResource providerResource) {
        this.providerResource = providerResource;
        this.providersById = Collections.emptyMap();
    }

    @Override
    public void refreshCache() {
        Collection<Provider> newProviders = providerResource.getProviders();
        providersById = newProviders.stream().collect(Collectors.toUnmodifiableMap(Provider::getId, p -> p));
        LOGGER.debug("Updated provider cache. Cache now has {} elements", providersById.size());
    }

    @Override
    public Collection<Provider> getProviders() {
        return providersById.values();
    }

    @Override
    public Provider getProvider(Long id) {
        return providersById.get(id);
    }

    @Override
    public String getReferential(Long id) {
        return getProvider(id).chouetteInfo.referential;
    }

    @Override
    public Long getProviderId(String referential) {
        Provider provider = providersById.values().stream().filter(p -> referential.equals(p.chouetteInfo.referential)).findFirst().orElse(null);
        if (provider != null) {
            return provider.getId();
        }
        return null;
    }
}
