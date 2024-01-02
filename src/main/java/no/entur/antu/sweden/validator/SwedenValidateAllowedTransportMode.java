package no.entur.antu.sweden.validator;

import org.entur.netex.validation.validator.xpath.rules.ValidatedAllowedTransportMode;

public class SwedenValidateAllowedTransportMode
  extends ValidatedAllowedTransportMode {

  private static final String SWEDEN_VALID_TRANSPORT_MODES =
    "'" +
    String.join(
      "','",
      "coach",
      "bus",
      "tram",
      "rail",
      "metro",
      "air",
      "water",
      "cableway",
      "funicular",
      "taxi",
      "unknown"
    ) +
    "'";

  public SwedenValidateAllowedTransportMode() {
    super(SWEDEN_VALID_TRANSPORT_MODES);
  }
}
