package no.entur.antu.validator;

import no.entur.antu.netex.loader.DefaultNetexDatasetLoader;
import no.entur.antu.netex.loader.NetexDatasetLoader;
import no.entur.antu.organisation.OrganisationRegistry;
import org.entur.netex.NetexParser;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.index.impl.NetexEntitiesIndexImpl;
import org.rutebanken.netex.model.EntityStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Set;

public class AuthorityIdValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorityIdValidator.class);

    private final OrganisationRegistry organisationRegistry;

    public AuthorityIdValidator(OrganisationRegistry organisationRegistry) {
        this.organisationRegistry = organisationRegistry;
    }

    public void validateAuthorityId(InputStream timetableDataset, String codespace) {
        Set<String> whitelistedAuthorityIds = organisationRegistry.getWhitelistedAuthorityIds(codespace);
        NetexEntitiesIndex netexEntitiesIndex = new NetexEntitiesIndexImpl();
        NetexDatasetLoader netexDatasetLoader = new DefaultNetexDatasetLoader();
        netexDatasetLoader.load(timetableDataset, netexEntitiesIndex);
        netexEntitiesIndex.getAuthorityIndex()
                .getAll()
                .stream()
                .map(EntityStructure::getId)
                .forEach(authorityId -> checkAuthorityId(whitelistedAuthorityIds, authorityId, codespace));
    }

    private void checkAuthorityId(Set<String> whitelistedAuthorityIds, String authorityId, String codespace) {
        if (!whitelistedAuthorityIds.contains(authorityId)) {
            LOGGER.warn("Invalid Authority Id {} for codespace {}", authorityId, codespace);
        }
    }

}
