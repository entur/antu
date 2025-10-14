package no.entur.antu.validation.utilities;

import no.entur.antu.validation.NetexCodespace;

import java.util.HashSet;
import java.util.Set;

public class CodespaceUtils {
    public static Set<NetexCodespace> getValidCodespacesFor(String validatingCodespace, Set<NetexCodespace> additionalCodespaces) {
        Set<NetexCodespace> validCodespaces = new HashSet<>();
        if (additionalCodespaces != null && !additionalCodespaces.isEmpty()) {
            validCodespaces.addAll(additionalCodespaces);
        }
        validCodespaces.add(NetexCodespace.rutebanken(validatingCodespace));
        return validCodespaces;
    }
}
