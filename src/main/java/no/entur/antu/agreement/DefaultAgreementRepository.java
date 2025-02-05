package no.entur.antu.agreement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Set;

public class DefaultAgreementRepository implements AgreementRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultAgreementRepository.class);

    private final AgreementResource agreementResource;
    private final Set<String> organisationIdCache;

    public DefaultAgreementRepository(AgreementResource agreementResource, Set<String> organisationIdCache) {
        this.agreementResource = agreementResource;
        this.organisationIdCache = organisationIdCache;
    }

    @Override
    public void refreshCache() {
        ArrayList<String> authorityIds = new ArrayList<>(agreementResource.getAuthorityIds());
        ArrayList<String> operatorIds = new ArrayList<>(agreementResource.getOperatorIds());
        organisationIdCache.addAll(authorityIds);
        organisationIdCache.addAll(operatorIds);
    }

    @Override
    public boolean isEmpty() {
        return this.organisationIdCache.isEmpty();
    }

    @Override
    public Boolean organisationExists(String masterIdOrAlias) {
        return this.organisationIdCache.contains(masterIdOrAlias);
    }
}
