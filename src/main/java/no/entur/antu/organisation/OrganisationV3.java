package no.entur.antu.organisation;

import java.util.List;

public class OrganisationV3 {
    public String masterId;
    public Integer internalId;
    public Integer organisationId;
    public List<String> aliases;

    public OrganisationV3(String masterId, Integer internalId, Integer organisationId, List<String> aliases) {
        this.masterId = masterId;
        this.internalId = internalId;
        this.organisationId = organisationId;
        this.aliases = aliases;
    }
}
