package no.entur.antu.validation.validator.organisation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;
import no.entur.antu.exception.AntuException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    when(agreementResource.getOrganisationAliases())
      .thenReturn(refreshedOrganisationIds);

    repository.refreshCache();
    assertFalse(repository.hasOrganisationWithAlias("TestOrg1"));
    assertTrue(repository.hasOrganisationWithAlias("TestOrg2"));
    assertTrue(repository.hasOrganisationWithAlias("TestOrg3"));
  }

  /**
   * The agreement registry is never legitimately empty, so an empty response is a failed or truncated
   * one. Refusing it rather than applying it keeps the caller from believing the cache was refreshed.
   */
  @Test
  void testRefreshCacheRefusesAnEmptyResponse() {
    DefaultOrganisationAliasRepository repository =
      new DefaultOrganisationAliasRepository(
        this.agreementResource,
        new HashSet<>(Set.of("TestOrg1"))
      );
    when(agreementResource.getOrganisationAliases()).thenReturn(Set.of());

    assertThrows(AntuException.class, repository::refreshCache);
  }

  /**
   * Applying an empty response would clear the cache, and every AuthorityRef in every dataset would
   * then be reported as invalid until the next refresh.
   */
  @Test
  void testRefreshCacheKeepsThePreviousAliasesWhenTheResponseIsEmpty() {
    HashSet<String> organisationIds = new HashSet<>(
      Set.of("TestOrg1", "TestOrg2")
    );
    DefaultOrganisationAliasRepository repository =
      new DefaultOrganisationAliasRepository(
        this.agreementResource,
        organisationIds
      );
    when(agreementResource.getOrganisationAliases()).thenReturn(Set.of());

    assertThrows(AntuException.class, repository::refreshCache);

    assertTrue(repository.hasOrganisationWithAlias("TestOrg1"));
    assertTrue(repository.hasOrganisationWithAlias("TestOrg2"));
  }
}
