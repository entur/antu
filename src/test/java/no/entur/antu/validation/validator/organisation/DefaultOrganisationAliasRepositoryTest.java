package no.entur.antu.validation.validator.organisation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.HashSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DefaultOrganisationAliasRepositoryTest {

  private AgreementResource agreementResource;

  @BeforeEach
  void setUp() {
    this.agreementResource = mock(AgreementResource.class);
  }

  @Test
  void testIsEmpty() {
    HashSet<String> organisationIds = new HashSet<>();
    DefaultOrganisationAliasRepository repository =
      new DefaultOrganisationAliasRepository(
        this.agreementResource,
        organisationIds
      );
    assertTrue(repository.isEmpty());
  }

  @Test
  void testHasOrganisationCheck() {
    HashSet<String> organisationIds = new HashSet<>();
    DefaultOrganisationAliasRepository repository =
      new DefaultOrganisationAliasRepository(
        this.agreementResource,
        organisationIds
      );
    assertFalse(repository.hasOrganisationWithAlias("TestOrg"));
    organisationIds.add("TestOrg");
    assertTrue(repository.hasOrganisationWithAlias("TestOrg"));
  }

  @Test
  void testRefreshCache() {
    HashSet<String> initialOrganisationIds = new HashSet<>();
    initialOrganisationIds.add("TestOrg1");
    initialOrganisationIds.add("TestOrg2");
    DefaultOrganisationAliasRepository repository =
      new DefaultOrganisationAliasRepository(
        this.agreementResource,
        initialOrganisationIds
      );

    HashSet<String> refreshedOrganisationIds = new HashSet<>();
    refreshedOrganisationIds.add("TestOrg2");
    refreshedOrganisationIds.add("TestOrg3");
    Mockito
      .when(agreementResource.getOrganisationAliases())
      .thenReturn(refreshedOrganisationIds);

    repository.refreshCache();
    assertFalse(repository.hasOrganisationWithAlias("TestOrg1"));
    assertTrue(repository.hasOrganisationWithAlias("TestOrg2"));
    assertTrue(repository.hasOrganisationWithAlias("TestOrg3"));
  }
}
