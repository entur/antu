package no.entur.antu.validator.xpath.rules;

import net.sf.saxon.s9api.XdmNode;
import org.entur.netex.validation.validator.xpath.XPathValidationContext;
import org.entur.netex.validation.validator.xpath.XPathValidationReportEntry;
import org.entur.netex.validation.validator.xpath.rules.ValidateNotExist;
import org.entur.netex.validation.xml.NetexXMLParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

class ValidateNoLineInImportedFlexDatasetTest {

    public static final String TEST_CODESPACE = "ATB";
    private static final NetexXMLParser NETEX_XML_PARSER = new NetexXMLParser(Set.of("SiteFrame"));

    private static final String NETEX_FRAGMENT = """
            <ServiceFrame xmlns="http://www.netex.org.uk/netex" id="ATB:ServiceFrame:1" version="2223">
              <lines>
                <Line id="ATB:Line:2_1" version="2223">
                  <Name>Ranheim - Strindheim - sentrum - Tiller - Heimdal - Kattem</Name>
                  <TransportMode>bus</TransportMode>
                  <TransportSubmode>
                    <BusSubmode>localBus</BusSubmode>
                  </TransportSubmode>
                  <PublicCode>1</PublicCode>
                  <PrivateCode>1</PrivateCode>
                  <OperatorRef ref="ATB:Operator:170" />
                  <RepresentedByGroupRef ref="ATB:GroupOfLines:bus_1" />
                </Line>
              </lines>
            </ServiceFrame>
                """;

    @Test
    void testMissingNSRCodeSpace() {
        ValidateNotExist validateNotExist = new ValidateNotExist("ServiceFrame/lines/Line", "Line not allowed in imported flexible line files", "LINE_10");
        XdmNode document = NETEX_XML_PARSER.parseStringToXdmNode(NETEX_FRAGMENT);
        XPathValidationContext xpathValidationContext = new XPathValidationContext(document, NETEX_XML_PARSER, TEST_CODESPACE, null);
        List<XPathValidationReportEntry> xPathValidationReportEntries = validateNotExist.validate(xpathValidationContext);
        Assertions.assertNotNull(xPathValidationReportEntries);
        Assertions.assertFalse(xPathValidationReportEntries.isEmpty());
    }
}
