package no.entur.antu.organisation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class DefaultOrganisationRepository implements OrganisationRepository {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultOrganisationRepository.class);

  private final OrganisationResource organisationResource;
  private final Set<String> organisationIdCache;

  public DefaultOrganisationRepository(OrganisationResource organisationResource, Set<String> organisationIdCache) {
    this.organisationResource = organisationResource;
    this.organisationIdCache = organisationIdCache;
  }

  @Override
  public Boolean organisationExists(String organisationId) {
    return organisationIdCache.contains(organisationId);
  }

  @Override
  public void refreshCache() {
    HashSet<String> organisationIdsForCache = new HashSet<>();
    organisationResource.getOrganisations().stream().forEach(
    organisation ->
            organisationIdsForCache.addAll(organisation.aliases)
    );
    organisationIdCache.retainAll(organisationIdsForCache);
    organisationIdCache.addAll(organisationIdsForCache);
    LOGGER.debug("Updated organisation cache. Cache now has {} elements", organisationIdCache.size());
  }

  @Override
  public boolean isEmpty() {
    return organisationIdCache.isEmpty();
  }
}
