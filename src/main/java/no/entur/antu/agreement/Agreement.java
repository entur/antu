package no.entur.antu.agreement;

public class Agreement {
    public String role;
    public String roleId;
    public Integer organisationId;

    public Agreement(String role, String roleId, Integer organisationId) {
        this.role = role;
        this.roleId = roleId;
        this.organisationId = organisationId;
    }
}
