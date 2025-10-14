package no.entur.antu.validation.validator.xpath;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import no.entur.antu.validation.NetexCodespace;
import no.entur.antu.validation.validator.xpath.rules.ValidateAllowedCodespaces;
import org.entur.netex.validation.test.xpath.support.TestValidationContextBuilder;
import org.entur.netex.validation.validator.ValidationIssue;
import org.entur.netex.validation.validator.xpath.XPathRuleValidationContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ValidateAllowedCodespacesTest {

  public static final String TEST_CODESPACE = "FLB";

  private static final String NETEX_FRAGMENT =
    """
                <PublicationDelivery xmlns="http://www.netex.org.uk/netex" xmlns:gis="http://www.opengis.net/gml/3.2" xmlns:siri="http://www.siri.org.uk/siri" version="1.13:NO-NeTEx-networktimetable:1.3">
                                <dataObjects>
                                  <CompositeFrame created="2021-09-09T14:20:20.905" version="1" id="FLB:CompositeFrame:188">
                                    <codespaces>
                                      <Codespace id="nsr">
                                        <Xmlns>NSR</Xmlns>
                                        <XmlnsUrl>${XML_NAMESPACE_URL}</XmlnsUrl>
                                      </Codespace>
                                    </codespaces>
                                  </CompositeFrame>
                                </dataObjects>
                </PublicationDelivery>
            """;

  @Test
  void testInvalidCodeSpace() {
    String publicationDeliveryWithInvalidCodespace = NETEX_FRAGMENT.replace(
      "${XML_NAMESPACE_URL}",
      "http://www.rutebanken.org/ns/nsr-invalid"
    );
    ValidateAllowedCodespaces validateAllowedCodespaces =
      new ValidateAllowedCodespaces(new HashSet<>());

    XPathRuleValidationContext xpathValidationContext =
      TestValidationContextBuilder
        .ofNetexFragment(publicationDeliveryWithInvalidCodespace)
        .withCodespace(TEST_CODESPACE)
        .build();

    List<ValidationIssue> xPathValidationReportEntries =
      validateAllowedCodespaces.validate(xpathValidationContext);
    Assertions.assertFalse(xPathValidationReportEntries.isEmpty());
  }

  @Test
  void testValidCodeSpace() {
    NetexCodespace nsrNetexCodespace = NetexCodespace.rutebanken("nsr");
    Set<NetexCodespace> additionalAllowedCodespaces = new HashSet<>();
    additionalAllowedCodespaces.add(nsrNetexCodespace);
    String publicationDeliveryWithValidCodespace = NETEX_FRAGMENT.replace(
      "${XML_NAMESPACE_URL}",
            nsrNetexCodespace.xmlnsUrl()
    );
    ValidateAllowedCodespaces validateAllowedCodespaces =
      new ValidateAllowedCodespaces(additionalAllowedCodespaces);
    XPathRuleValidationContext xpathValidationContext =
      TestValidationContextBuilder
        .ofNetexFragment(publicationDeliveryWithValidCodespace)
        .withCodespace(TEST_CODESPACE)
        .build();

    List<ValidationIssue> xPathValidationReportEntries =
      validateAllowedCodespaces.validate(xpathValidationContext);
    Assertions.assertTrue(xPathValidationReportEntries.isEmpty());
  }
}
