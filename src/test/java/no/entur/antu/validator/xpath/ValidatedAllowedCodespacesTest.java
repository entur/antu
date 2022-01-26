package no.entur.antu.validator.xpath;

import net.sf.saxon.s9api.XdmNode;
import no.entur.antu.validator.xpath.rules.ValidateAllowedCodespaces;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.entur.netex.validation.validator.xpath.XPathValidationContext;
import org.entur.netex.validation.xml.XMLParserUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

class ValidatedAllowedCodespacesTest {

    public static final String TEST_CODESPACE = "FLB";
    public static final String TEST_FILE_VALID_CODESPACE = "FLB_FLB-Line-42_42_Flamsbana.xml";
    public static final String TEST_FILE_VALID_INVALID_CODESPACE = "FLB_FLB-Line-42_42_Flamsbana_invalid_codespace.xml";

    @Test
    void testValidCodeSpace() throws IOException {
        ValidateAllowedCodespaces validateAllowedCodespaces = new ValidateAllowedCodespaces();
        InputStream testDatasetAsStream = getClass().getResourceAsStream('/' + TEST_FILE_VALID_CODESPACE);
        XdmNode document = XMLParserUtil.parseFileToXdmNode(testDatasetAsStream.readAllBytes());
        XPathValidationContext validationContext = new XPathValidationContext(document, XMLParserUtil.getXPathCompiler(), TEST_CODESPACE, TEST_FILE_VALID_CODESPACE);
        List<ValidationReportEntry> validationReportEntries = validateAllowedCodespaces.validate(validationContext);
        Assertions.assertTrue(validationReportEntries.isEmpty());
    }

    @Test
    void testInValidCodeSpace() throws IOException {
        ValidateAllowedCodespaces validateAllowedCodespaces = new ValidateAllowedCodespaces();
        InputStream testDatasetAsStream = getClass().getResourceAsStream('/' + TEST_FILE_VALID_INVALID_CODESPACE);
        XdmNode document = XMLParserUtil.parseFileToXdmNode(testDatasetAsStream.readAllBytes());
        XPathValidationContext validationContext = new XPathValidationContext(document, XMLParserUtil.getXPathCompiler(), TEST_CODESPACE, TEST_FILE_VALID_INVALID_CODESPACE);
        List<ValidationReportEntry> validationReportEntries = validateAllowedCodespaces.validate(validationContext);
        Assertions.assertFalse(validationReportEntries.isEmpty());
    }

}
