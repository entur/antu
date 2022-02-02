package no.entur.antu.validator.xpath;

import net.sf.saxon.s9api.XdmNode;
import no.entur.antu.validator.xpath.rules.ValidateAllowedCodespaces;
import org.entur.netex.validation.validator.xpath.XPathValidationContext;
import org.entur.netex.validation.validator.xpath.XPathValidationReportEntry;
import org.entur.netex.validation.xml.NetexXMLParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

class ValidatedAllowedCodespacesTest {

    public static final String TEST_CODESPACE = "FLB";
    public static final String TEST_FILE_VALID_CODESPACE = "FLB_FLB-Line-42_42_Flamsbana.xml";
    public static final String TEST_FILE_VALID_INVALID_CODESPACE = "FLB_FLB-Line-42_42_Flamsbana_invalid_codespace.xml";

    private static final NetexXMLParser NETEX_XML_PARSER = new NetexXMLParser(Set.of("SiteFrame"));

    @Test
    void testValidCodeSpace() throws IOException {
        ValidateAllowedCodespaces validateAllowedCodespaces = new ValidateAllowedCodespaces();
        List<XPathValidationReportEntry> validationReportEntries = getValidationReportEntries(TEST_FILE_VALID_CODESPACE, TEST_CODESPACE, validateAllowedCodespaces);
        Assertions.assertTrue(validationReportEntries.isEmpty());
    }

    @Test
    void testInValidCodeSpace() throws IOException {
        ValidateAllowedCodespaces validateAllowedCodespaces = new ValidateAllowedCodespaces();
        List<XPathValidationReportEntry> validationReportEntries = getValidationReportEntries(TEST_FILE_VALID_INVALID_CODESPACE, TEST_CODESPACE, validateAllowedCodespaces);
        Assertions.assertFalse(validationReportEntries.isEmpty());
    }

    private List<XPathValidationReportEntry> getValidationReportEntries(String testFileValidCodespace, String testCodespace, ValidateAllowedCodespaces validateAllowedCodespaces) throws IOException {
        InputStream testDatasetAsStream = getClass().getResourceAsStream('/' + testFileValidCodespace);
        assert testDatasetAsStream != null;
        XdmNode document = NETEX_XML_PARSER.parseFileToXdmNode(testDatasetAsStream.readAllBytes());
        XPathValidationContext validationContext = new XPathValidationContext(document, NETEX_XML_PARSER, testCodespace, testFileValidCodespace);
        return validateAllowedCodespaces.validate(validationContext);
    }

}
