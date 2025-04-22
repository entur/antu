package no.entur.antu.validation.validator.organisation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Set;

public class DefaultOrganisationAliasRepository implements OrganisationAliasRepository {
    private final AgreementResource agreementResource;
    private final Set<String> organisationAliasCache;

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultOrganisationAliasRepository.class);

    public DefaultOrganisationAliasRepository(
        AgreementResource agreementResource,
        Set<String> organisationAliasCache
    ) {
        this.agreementResource = agreementResource;
        this.organisationAliasCache = organisationAliasCache;
    }

    @Override
    public boolean hasOrganisationWithAlias(String alias) {
        return organisationAliasCache.contains(alias);
    }

    @Override
    public void refreshCache() {
        Collection<String> organisationAliases = agreementResource.getOrganisationAliases();
        organisationAliasCache.retainAll(organisationAliases);
        organisationAliasCache.addAll(organisationAliases);
        LOGGER.info("Organisation Alias cache was refreshed with {} elements", organisationAliasCache.size());
    }

    @Override
    public boolean isEmpty() {
        return organisationAliasCache.isEmpty();
    }
}
