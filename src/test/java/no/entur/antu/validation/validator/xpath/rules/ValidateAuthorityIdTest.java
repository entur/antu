package no.entur.antu.validation.validator.xpath.rules;

import java.util.List;
import java.util.Map;
import java.util.Set;
import no.entur.antu.organisation.OrganisationRepository;
import no.entur.antu.organisation.SimpleOrganisationRepository;
import org.entur.netex.validation.test.xpath.support.TestValidationContextBuilder;
import org.entur.netex.validation.validator.ValidationIssue;
import org.entur.netex.validation.validator.xpath.XPathRuleValidationContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ValidateAuthorityIdTest {

  public static final String TEST_CODESPACE = "AVI";
  public static final String TEST_VALID_AUTHORITY_ID = "AVI:Authority:Avinor";
  public static final String TEST_INVALID_AUTHORITY_ID = "XXX:Authority:1";

  private static final String NETEX_FRAGMENT =
    """
                                    <ResourceFrame xmlns="http://www.netex.org.uk/netex" version="1" id="AVI:ResourceFrame:17709027">
                                              <organisations>
                                                <Authority version="1" id="${AUTHORITY_ID}">
                                                  <CompanyNumber>985198292</CompanyNumber>
                                                  <Name>Avinor</Name>
                                                  <LegalName>AVINOR AS</LegalName>
                                                  <ContactDetails>
                                                    <Phone>0047 815 30 550</Phone>
                                                    <Url>https://www.avinor.no</Url>
                                                  </ContactDetails>
                                                  <OrganisationType>authority</OrganisationType>
                                                </Authority>
                                              </organisations>
                                            </ResourceFrame>
            """;
  private ValidateAuthorityId validateAuthorityId;

  @BeforeEach
  void setUpTest() {
    OrganisationRepository organisationRepository =
      new SimpleOrganisationRepository(
        Map.of(TEST_CODESPACE, Set.of(TEST_VALID_AUTHORITY_ID))
      );

    validateAuthorityId = new ValidateAuthorityId(organisationRepository);
  }

  @Test
  void testInvalidAuthority() {
    String netexFragment = NETEX_FRAGMENT.replace(
      "${AUTHORITY_ID}",
      TEST_INVALID_AUTHORITY_ID
    );
    XPathRuleValidationContext xpathValidationContext =
      TestValidationContextBuilder
        .ofNetexFragment(netexFragment)
        .withCodespace(TEST_CODESPACE)
        .build();
    List<ValidationIssue> xPathValidationReportEntries =
      validateAuthorityId.validate(xpathValidationContext);
    Assertions.assertFalse(xPathValidationReportEntries.isEmpty());
  }

  @Test
  void testValidAuthority() {
    String netexFragment = NETEX_FRAGMENT.replace(
      "${AUTHORITY_ID}",
      TEST_VALID_AUTHORITY_ID
    );
    XPathRuleValidationContext xpathValidationContext =
      TestValidationContextBuilder
        .ofNetexFragment(netexFragment)
        .withCodespace(TEST_CODESPACE)
        .build();
    List<ValidationIssue> xPathValidationReportEntries =
      validateAuthorityId.validate(xpathValidationContext);
    Assertions.assertTrue(xPathValidationReportEntries.isEmpty());
  }
}
