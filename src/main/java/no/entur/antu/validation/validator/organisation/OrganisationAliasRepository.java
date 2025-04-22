package no.entur.antu.validation.validator.organisation;

public interface OrganisationAliasRepository {
    boolean hasOrganisationWithAlias(String organisationId);
    /**
     * Retrieve data from the Organisation Register and update the cache accordingly.
     */
    void refreshCache();

    /**
     * Return true if the repository is not primed.
     */
    boolean isEmpty();
}
