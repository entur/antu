package no.entur.antu.validation.validator.xpath.rules;

import java.util.List;
import org.entur.netex.validation.test.xpath.support.TestValidationContextBuilder;
import org.entur.netex.validation.validator.ValidationIssue;
import org.entur.netex.validation.validator.xpath.XPathRuleValidationContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ValidateNSRCodespaceTest {

  public static final String TEST_CODESPACE = "FLB";

  private static final String NETEX_FRAGMENT =
    """
  <CompositeFrame  xmlns="http://www.netex.org.uk/netex">
    <codespaces>
      <Codespace id="nsr">
        <Xmlns>NSR</Xmlns>
        <XmlnsUrl>${XML_NAMESPACE_URL}</XmlnsUrl>
      </Codespace>
    </codespaces>
  </CompositeFrame>
  """;

  @Test
  void testMissingNSRCodeSpace() {
    ValidateNSRCodespace validateNSRCodespace = new ValidateNSRCodespace();
    String fragmentWithInvalidCodespace = NETEX_FRAGMENT.replace(
      "${XML_NAMESPACE_URL}",
      "http://www.rutebanken.org/ns/nsr-invalid"
    );

    XPathRuleValidationContext xpathValidationContext =
      TestValidationContextBuilder
        .ofNetexFragment(fragmentWithInvalidCodespace)
        .withCodespace(TEST_CODESPACE)
        .build();

    List<ValidationIssue> xPathValidationReportEntries =
      validateNSRCodespace.validate(xpathValidationContext);
    Assertions.assertFalse(xPathValidationReportEntries.isEmpty());
  }

  @Test
  void testValidNSRCodeSpace() {
    ValidateNSRCodespace validateNSRCodespace = new ValidateNSRCodespace();
    String fragmentWithValidCodespace = NETEX_FRAGMENT.replace(
      "${XML_NAMESPACE_URL}",
      "http://www.rutebanken.org/ns/nsr"
    );
    XPathRuleValidationContext xpathValidationContext =
      TestValidationContextBuilder
        .ofNetexFragment(fragmentWithValidCodespace)
        .withCodespace(TEST_CODESPACE)
        .build();
    List<ValidationIssue> xPathValidationReportEntries =
      validateNSRCodespace.validate(xpathValidationContext);
    Assertions.assertTrue(xPathValidationReportEntries.isEmpty());
  }
}
