package no.entur.antu.organisation;

/**
 * Simple hashmap-based implementation of the organisation repository.
 */
public class SimpleOrganisationRepository
  implements OrganisationRepository {

  public SimpleOrganisationRepository(
  ) {
  }

  @Override
  public void refreshCache() {

  }

  @Override
  public boolean isEmpty() {
    return false;
  }

  @Override
  public Boolean organisationExists(String organisationId) {
    return false;
  }
}
