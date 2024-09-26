package no.entur.antu.validation.validator.xpath.rules;

import java.util.List;
import java.util.Set;
import net.sf.saxon.s9api.XdmNode;
import no.entur.antu.organisation.OrganisationRepository;
import org.entur.netex.validation.validator.xpath.XPathRuleValidationContext;
import org.entur.netex.validation.validator.xpath.XPathValidationReportEntry;
import org.entur.netex.validation.xml.NetexXMLParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ValidateAuthorityIdTest {

  public static final String TEST_CODESPACE = "AVI";
  public static final String TEST_VALID_AUTHORITY_ID = "AVI:Authority:Avinor";
  public static final String TEST_INVALID_AUTHORITY_ID = "XXX:Authority:1";
  private static final NetexXMLParser NETEX_XML_PARSER = new NetexXMLParser(
    Set.of("SiteFrame")
  );

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
      new OrganisationRepository() {
        @Override
        public void refreshCache() {}

        @Override
        public boolean isEmpty() {
          return false;
        }

        @Override
        public Set<String> getWhitelistedAuthorityIds(String codespace) {
          return Set.of(TEST_VALID_AUTHORITY_ID);
        }
      };

    validateAuthorityId = new ValidateAuthorityId(organisationRepository);
  }

  @Test
  void testInvalidAuthority() {
    String fragmentWithInvalidCodespace = NETEX_FRAGMENT.replace(
      "${AUTHORITY_ID}",
      TEST_INVALID_AUTHORITY_ID
    );
    XdmNode document = NETEX_XML_PARSER.parseStringToXdmNode(
      fragmentWithInvalidCodespace
    );
    XPathRuleValidationContext xpathValidationContext =
      new XPathRuleValidationContext(
        document,
        NETEX_XML_PARSER,
        TEST_CODESPACE,
        null
      );
    List<XPathValidationReportEntry> xPathValidationReportEntries =
      validateAuthorityId.validate(xpathValidationContext);
    Assertions.assertNotNull(xPathValidationReportEntries);
    Assertions.assertFalse(xPathValidationReportEntries.isEmpty());
  }

  @Test
  void testValidAuthority() {
    String fragmentWithInvalidCodespace = NETEX_FRAGMENT.replace(
      "${AUTHORITY_ID}",
      TEST_VALID_AUTHORITY_ID
    );
    XdmNode document = NETEX_XML_PARSER.parseStringToXdmNode(
      fragmentWithInvalidCodespace
    );
    XPathRuleValidationContext xpathValidationContext =
      new XPathRuleValidationContext(
        document,
        NETEX_XML_PARSER,
        TEST_CODESPACE,
        null
      );
    List<XPathValidationReportEntry> xPathValidationReportEntries =
      validateAuthorityId.validate(xpathValidationContext);
    Assertions.assertNotNull(xPathValidationReportEntries);
    Assertions.assertTrue(xPathValidationReportEntries.isEmpty());
  }
}
