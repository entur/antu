package no.entur.antu.organisation;

import java.util.Map;
import java.util.Set;

/**
 * Simple hashmap-based implementation of the organisation repository.
 */
public class SimpleOrganisationRepository implements OrganisationRepository {

  private final Map<String, Set<String>> whiteListByCodespace;

  public SimpleOrganisationRepository(
    Map<String, Set<String>> whiteListByCodespace
  ) {
    this.whiteListByCodespace = whiteListByCodespace;
  }

  @Override
  public void refreshCache() {
    //NOOP
  }

  @Override
  public boolean isEmpty() {
    return whiteListByCodespace.isEmpty();
  }

  @Override
  public Set<String> getWhitelistedAuthorityIds(String codespace) {
    Set<String> whitelist = whiteListByCodespace.get(codespace);
    return whitelist == null ? Set.of() : whitelist;
  }
}
