package no.entur.antu.validation.utilities;

import java.util.HashSet;
import java.util.Set;
import no.entur.antu.validation.NetexCodespace;

public class CodespaceUtils {

  private CodespaceUtils() {}

  public static Set<NetexCodespace> getValidCodespacesFor(
    String validatingCodespace,
    Set<NetexCodespace> additionalCodespaces
  ) {
    Set<NetexCodespace> validCodespaces = new HashSet<>();
    if (additionalCodespaces != null && !additionalCodespaces.isEmpty()) {
      validCodespaces.addAll(additionalCodespaces);
    }
    validCodespaces.add(NetexCodespace.rutebanken(validatingCodespace));
    return validCodespaces;
  }
}
