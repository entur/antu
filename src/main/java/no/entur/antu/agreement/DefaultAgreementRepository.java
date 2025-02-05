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
        ArrayList<Agreement> authorities = new ArrayList<>(agreementResource.getAuthorityIds());
        ArrayList<Agreement> operators = new ArrayList<>(agreementResource.getOperatorIds());

        ArrayList<String> ids = new ArrayList<>();
        for (Agreement agreement : authorities) {
            ids.add(agreement.roleId);
        }
        for (Agreement operator : operators) {
            ids.add(operator.roleId);
        }
        organisationIdCache.addAll(ids);
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
