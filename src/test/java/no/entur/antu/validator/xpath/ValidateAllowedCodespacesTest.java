package no.entur.antu.validator.xpath;

import net.sf.saxon.s9api.XdmNode;
import no.entur.antu.validator.xpath.rules.ValidateAllowedCodespaces;
import org.entur.netex.validation.validator.xpath.XPathValidationContext;
import org.entur.netex.validation.validator.xpath.XPathValidationReportEntry;
import org.entur.netex.validation.xml.NetexXMLParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static no.entur.antu.validator.codespace.NetexCodespace.NSR_NETEX_CODESPACE;

class ValidateAllowedCodespacesTest {

    public static final String TEST_CODESPACE = "FLB";
    private static final NetexXMLParser NETEX_XML_PARSER = new NetexXMLParser(Set.of("SiteFrame"));

    private static final String NETEX_FRAGMENT = """
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
        String publicationDeliveryWithInvalidCodespace = NETEX_FRAGMENT.replace("${XML_NAMESPACE_URL}", "http://www.rutebanken.org/ns/nsr-invalid");
        ValidateAllowedCodespaces validateAllowedCodespaces = new ValidateAllowedCodespaces();
        XdmNode document = NETEX_XML_PARSER.parseStringToXdmNode(publicationDeliveryWithInvalidCodespace);
        XPathValidationContext xpathValidationContext = new XPathValidationContext(document, NETEX_XML_PARSER, TEST_CODESPACE, null);
        List<XPathValidationReportEntry> xPathValidationReportEntries = validateAllowedCodespaces.validate(xpathValidationContext);
        Assertions.assertNotNull(xPathValidationReportEntries);
        Assertions.assertFalse(xPathValidationReportEntries.isEmpty());
    }

    @Test
    void testValidCodeSpace() {
        String publicationDeliveryWithValidCodespace = NETEX_FRAGMENT.replace("${XML_NAMESPACE_URL}", NSR_NETEX_CODESPACE.xmlnsUrl());
        ValidateAllowedCodespaces validateAllowedCodespaces = new ValidateAllowedCodespaces();
        XdmNode document = NETEX_XML_PARSER.parseStringToXdmNode(publicationDeliveryWithValidCodespace);
        XPathValidationContext xpathValidationContext = new XPathValidationContext(document, NETEX_XML_PARSER, TEST_CODESPACE, null);
        List<XPathValidationReportEntry> xPathValidationReportEntries = validateAllowedCodespaces.validate(xpathValidationContext);
        Assertions.assertNotNull(xPathValidationReportEntries);
        Assertions.assertTrue(xPathValidationReportEntries.isEmpty());
    }

}
