package no.entur.antu.organisation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultOrganisationRepositoryTest {
    private DefaultOrganisationRepository organisationV3Repository;
    private HashSet<String> organisationCache;
    private OrganisationResource organisationResource;

    @BeforeEach
    void setUp() {
        organisationCache = new HashSet<>();
        organisationResource = mock(OrganisationResource.class);
        organisationV3Repository = new DefaultOrganisationRepository(organisationResource, organisationCache);
        organisationCache.clear();
    }

    @Test
    void organisationExistsShouldReturnTrueIfOrganisationExists() {
        organisationCache.add("TestOrg");
        Assertions.assertTrue(organisationV3Repository.organisationExists("TestOrg"));
    }

    @Test
    void organisationExistsShouldReturnFalseIfOrganisationDoesNotExists() {
        Assertions.assertFalse(organisationV3Repository.organisationExists("TestOrg"));
    }

    @Test
    void isEmptyShouldReturnTrueIfCacheIsEmpty() {
        Assertions.assertTrue(organisationCache.isEmpty());
    }

    @Test
    void isEmptyShouldReturnFalseIfCacheIsNotEmpty() {
        organisationCache.add("TestOrg");
        Assertions.assertFalse(organisationCache.isEmpty());
    }

    private List<Organisation> mockOrganisations(String... aliases) {
        List<Organisation> organisations = new ArrayList<>();
        organisations.add(new Organisation("", 0, 0, Arrays.stream(aliases).toList()));
        return organisations;
    }

    @Test
    void refreshCacheShouldRetainExistingOrganisations() {
        List<Organisation> initialOrganisations = mockOrganisations("TestOrg 1", "TestOrg 2");
        when(organisationResource.getOrganisations()).thenReturn(initialOrganisations);
        organisationV3Repository.refreshCache();
        Assertions.assertTrue(organisationCache.contains("TestOrg 1"));
        Assertions.assertTrue(organisationCache.contains("TestOrg 2"));

        List<Organisation> updatedOrganisations = mockOrganisations("TestOrg 2", "TestOrg 3");
        when(organisationResource.getOrganisations()).thenReturn(updatedOrganisations);
        organisationV3Repository.refreshCache();
        Assertions.assertTrue(organisationCache.contains("TestOrg 2"));
        Assertions.assertTrue(organisationCache.contains("TestOrg 3"));

        Assertions.assertFalse(organisationCache.contains("TestOrg 1"));
    }
}
