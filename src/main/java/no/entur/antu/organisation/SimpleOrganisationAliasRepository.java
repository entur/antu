package no.entur.antu.organisation;

import no.entur.antu.validation.validator.organisation.OrganisationAliasRepository;

public class SimpleOrganisationAliasRepository
  implements OrganisationAliasRepository {

  @Override
  public boolean hasOrganisationWithAlias(String organisationId) {
    return false;
  }

  @Override
  public void refreshCache() {}

  @Override
  public boolean isEmpty() {
    return false;
  }
}
