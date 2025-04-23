package no.entur.antu.validation.validator.organisation;

import java.util.List;

public class Agreement {
    private List<String> roleIds;
    private List<String> aliases;

    public Agreement(List<String> roleIds, List<String> aliases) {
        this.roleIds = roleIds;
        this.aliases = aliases;
    }

    public List<String> getRoleIds() {
        return roleIds;
    }

    public List<String> getAliases() {
        return aliases;
    }
}
