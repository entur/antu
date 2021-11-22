package no.entur.antu.validator;

import no.entur.antu.netex.loader.DefaultNetexDatasetLoader;
import no.entur.antu.netex.loader.NetexDatasetLoader;
import no.entur.antu.organisation.OrganisationRepository;
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

public class AuthorityIdValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorityIdValidator.class);

    private final OrganisationRepository organisationRepository;

    public AuthorityIdValidator(OrganisationRepository organisationRepository) {
        this.organisationRepository = organisationRepository;
    }

    public ValidationReport validateAuthorityId(InputStream timetableDataset, String codespace, String validationReportId) {
        Set<String> whitelistedAuthorityIds = organisationRepository.getWhitelistedAuthorityIds(codespace);
        NetexEntitiesIndex netexEntitiesIndex = new NetexEntitiesIndexImpl();
        NetexDatasetLoader netexDatasetLoader = new DefaultNetexDatasetLoader();
        netexDatasetLoader.load(timetableDataset, netexEntitiesIndex);
        List<ValidationReportEntry> validationReportEntries = netexEntitiesIndex.getAuthorityIndex()
                .getAll()
                .stream()
                .map(EntityStructure::getId)
                .map(authorityId -> checkAuthorityId(whitelistedAuthorityIds, authorityId, codespace))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return new ValidationReport(codespace, validationReportId, validationReportEntries);
    }

    private ValidationReportEntry checkAuthorityId(Set<String> whitelistedAuthorityIds, String authorityId, String codespace) {
        if (!whitelistedAuthorityIds.contains(authorityId)) {
            LOGGER.warn("Invalid Authority Id {} for codespace {}", authorityId, codespace);
            return new ValidationReportEntry(String.format("Invalid Authority Id %s", authorityId), "Invalid authority ID", "ERROR");
        }
        return null;
    }

}
