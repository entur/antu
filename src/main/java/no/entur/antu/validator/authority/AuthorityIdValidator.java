package no.entur.antu.validator.authority;

import no.entur.antu.netex.loader.DefaultNetexDatasetLoader;
import no.entur.antu.netex.loader.NetexDatasetLoader;
import no.entur.antu.organisation.OrganisationRepository;
import no.entur.antu.validator.ValidationReportEntry;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.index.impl.NetexEntitiesIndexImpl;
import org.rutebanken.netex.model.EntityStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validate that NeTEx Authority identifier are valid according to the Organisation register.
 */
public class AuthorityIdValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorityIdValidator.class);

    private final OrganisationRepository organisationRepository;

    public AuthorityIdValidator(OrganisationRepository organisationRepository) {
        this.organisationRepository = organisationRepository;
    }

    public List<ValidationReportEntry> validateAuthorityId(InputStream timetableDataset, String codespace) {
        Set<String> whitelistedAuthorityIds = organisationRepository.getWhitelistedAuthorityIds(codespace);
        NetexEntitiesIndex netexEntitiesIndex = new NetexEntitiesIndexImpl();
        NetexDatasetLoader netexDatasetLoader = new DefaultNetexDatasetLoader();
        netexDatasetLoader.load(timetableDataset, netexEntitiesIndex);
        return netexEntitiesIndex.getAuthorityIndex()
                .getAll()
                .stream()
                .map(EntityStructure::getId)
                .map(authorityId -> checkAuthorityId(whitelistedAuthorityIds, authorityId, codespace))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private ValidationReportEntry checkAuthorityId(Set<String> whitelistedAuthorityIds, String authorityId, String codespace) {
        if (!whitelistedAuthorityIds.contains(authorityId)) {
            LOGGER.warn("Invalid Authority Id {} for codespace {}", authorityId, codespace);
            return new ValidationReportEntry(String.format("Invalid Authority Id %s", authorityId), "Invalid authority ID", "ERROR");
        }
        return null;
    }

}
