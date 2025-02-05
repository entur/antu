package no.entur.antu.agreement;

public interface AgreementRepository {
    /**
     * Retrieve data from the Agreement Register and update the cache accordingly.
     */
    void refreshCache();

    /**
     * Return true if the repository is not primed.
     */
    boolean isEmpty();

    /**
     * Return true if the organisation exists in the registry.
     */
    Boolean organisationExists(String masterIdOrAlias);
}
