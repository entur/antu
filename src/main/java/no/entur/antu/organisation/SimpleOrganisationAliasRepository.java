package no.entur.antu.organisation;

import java.util.Set;
import no.entur.antu.validation.validator.organisation.OrganisationAliasRepository;

/**
 * Simple in-memory based implementation of the organisation alias repository.
 */
public class SimpleOrganisationAliasRepository
  implements OrganisationAliasRepository {

  private final Set<String> organisationAliases;

  public SimpleOrganisationAliasRepository(Set<String> organisationAliases) {
    this.organisationAliases = organisationAliases;
  }

  @Override
  public boolean hasOrganisationWithAlias(String organisationId) {
    return organisationAliases.contains(organisationId);
  }

  @Override
  public void refreshCache() {
    //NOOP
  }

  @Override
  public boolean isEmpty() {
    return this.organisationAliases.isEmpty();
  }
}
