package no.entur.antu.validation;

import java.util.Locale;

/**
 * A Netex codespace, identified by its namespace and its URL.
 */
public record NetexCodespace(String xmlns, String xmlnsUrl) {
  public static NetexCodespace rutebanken(String namespace) {
    return new NetexCodespace(
      namespace.toUpperCase(Locale.ROOT),
      "http://www.rutebanken.org/ns/" + namespace.toLowerCase(Locale.ROOT)
    );
  }
}
