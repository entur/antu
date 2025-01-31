package no.entur.antu.organisation;

/**
 * Simple hashmap-based implementation of the organisation repository.
 */
public class SimpleOrganisationV3Repository
  implements OrganisationV3Repository {

  public SimpleOrganisationV3Repository(
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
