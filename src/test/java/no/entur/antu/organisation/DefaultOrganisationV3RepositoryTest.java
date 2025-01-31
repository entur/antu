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

public class DefaultOrganisationV3RepositoryTest {
    private DefaultOrganisationV3Repository organisationV3Repository;
    private HashSet<String> organisationCache;
    private OrganisationV3Resource organisationV3Resource;

    @BeforeEach
    void setUp() {
        organisationCache = new HashSet<>();
        organisationV3Resource = mock(OrganisationV3Resource.class);
        organisationV3Repository = new DefaultOrganisationV3Repository(organisationV3Resource, organisationCache);
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

    private List<OrganisationV3> mockOrganisations(String... aliases) {
        List<OrganisationV3> organisations = new ArrayList<>();
        organisations.add(new OrganisationV3("", 0, 0, Arrays.stream(aliases).toList()));
        return organisations;
    }

    @Test
    void refreshCacheShouldRetainExistingOrganisations() {
        List<OrganisationV3> initialOrganisations = mockOrganisations("TestOrg 1", "TestOrg 2");
        when(organisationV3Resource.getOrganisations()).thenReturn(initialOrganisations);
        organisationV3Repository.refreshCache();
        Assertions.assertTrue(organisationCache.contains("TestOrg 1"));
        Assertions.assertTrue(organisationCache.contains("TestOrg 2"));

        List<OrganisationV3> updatedOrganisations = mockOrganisations("TestOrg 2", "TestOrg 3");
        when(organisationV3Resource.getOrganisations()).thenReturn(updatedOrganisations);
        organisationV3Repository.refreshCache();
        Assertions.assertTrue(organisationCache.contains("TestOrg 2"));
        Assertions.assertTrue(organisationCache.contains("TestOrg 3"));

        Assertions.assertFalse(organisationCache.contains("TestOrg 1"));
    }
}
