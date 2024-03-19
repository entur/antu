package no.entur.antu.validation;

import static no.entur.antu.Constants.NSR_XMLNS;
import static no.entur.antu.Constants.NSR_XMLNSURL;
import static no.entur.antu.Constants.PEN_XMLNS;
import static no.entur.antu.Constants.PEN_XMLNSURL;

import java.util.Locale;
import java.util.Set;

/**
 * A NeTEx codespace, identified by its namespace and its URL.
 */
public record NetexCodespace(String xmlns, String xmlnsUrl) {
  public static final NetexCodespace NSR_NETEX_CODESPACE = new NetexCodespace(
    NSR_XMLNS,
    NSR_XMLNSURL
  );
  public static final NetexCodespace PEN_NETEX_CODESPACE = new NetexCodespace(
    PEN_XMLNS,
    PEN_XMLNSURL
  );

  public static Set<NetexCodespace> getValidNetexCodespacesFor(
    String codespace
  ) {
    NetexCodespace netexCodespace = NetexCodespace.getNetexCodespaceFor(
      codespace
    );
    if (NSR_NETEX_CODESPACE.equals(netexCodespace)) {
      return Set.of(NSR_NETEX_CODESPACE, PEN_NETEX_CODESPACE);
    } else {
      return Set.of(NSR_NETEX_CODESPACE, PEN_NETEX_CODESPACE, netexCodespace);
    }
  }

  private static NetexCodespace getNetexCodespaceFor(String codespace) {
    return new NetexCodespace(
      codespace.toUpperCase(Locale.ROOT),
      "http://www.rutebanken.org/ns/" + codespace.toLowerCase(Locale.ROOT)
    );
  }
}
