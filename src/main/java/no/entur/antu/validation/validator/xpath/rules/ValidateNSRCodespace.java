package no.entur.antu.validation.validator.xpath.rules;

import no.entur.antu.Constants;
import org.entur.netex.validation.validator.Severity;
import org.entur.netex.validation.validator.xpath.rules.ValidateExactlyOne;

/**
 * Validate that the PublicationDelivery refers to the NSR codespace.
 */
public class ValidateNSRCodespace extends ValidateExactlyOne {

  public static final String CODE_NSR_CODESPACE = "NSR_CODESPACE";

  public ValidateNSRCodespace() {
    super(
      "codespaces/Codespace[Xmlns = '" +
      Constants.NSR_XMLNS +
      "' and XmlnsUrl = '" +
      Constants.NSR_XMLNSURL +
      "']",
      CODE_NSR_CODESPACE,
      "NSR Codespace not declared",
      "NSR Codespace must be declared (Xmlns=NSR, XmlnsUrl=http://www.rutebanken.org/ns/nsr). Any references to StopPlaces must point to data in the NSR namespace",
      Severity.ERROR
    );
  }
}
