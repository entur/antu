package no.entur.antu.validation.validator.organisation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashSet;

import static org.mockito.Mockito.mock;

class DefaultOrganisationAliasRepositoryTest {

    private AgreementResource agreementResource;

    @BeforeEach
    void setUp() {
        this.agreementResource = mock(AgreementResource.class);
    }


    @Test
    void testIsEmpty() {
        HashSet<String> organisationIds = new HashSet<>();
        DefaultOrganisationAliasRepository repository = new DefaultOrganisationAliasRepository(this.agreementResource, organisationIds);
        Assertions.assertTrue(repository.isEmpty());
    }

    @Test
    void testHasOrganisationCheck() {
        HashSet<String> organisationIds = new HashSet<>();
        DefaultOrganisationAliasRepository repository = new DefaultOrganisationAliasRepository(this.agreementResource, organisationIds);
        Assertions.assertFalse(repository.hasOrganisationWithAlias("TestOrg"));
        organisationIds.add("TestOrg");
        Assertions.assertTrue(repository.hasOrganisationWithAlias("TestOrg"));
    }

    @Test
    void testRefreshCache() {
        HashSet<String> initialOrganisationIds = new HashSet<>();
        initialOrganisationIds.add("TestOrg1");
        initialOrganisationIds.add("TestOrg2");
        DefaultOrganisationAliasRepository repository = new DefaultOrganisationAliasRepository(this.agreementResource, initialOrganisationIds);

        HashSet<String> refreshedOrganisationIds = new HashSet<>();
        refreshedOrganisationIds.add("TestOrg2");
        refreshedOrganisationIds.add("TestOrg3");
        Mockito.when(agreementResource.getOrganisationAliases()).thenReturn(refreshedOrganisationIds.stream().toList());

        repository.refreshCache();
        Assertions.assertFalse(repository.hasOrganisationWithAlias("TestOrg1"));
        Assertions.assertTrue(repository.hasOrganisationWithAlias("TestOrg2"));
        Assertions.assertTrue(repository.hasOrganisationWithAlias("TestOrg3"));
    }
}