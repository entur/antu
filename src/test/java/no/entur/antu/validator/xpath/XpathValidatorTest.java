package no.entur.antu.validator.xpath;

import net.sf.saxon.s9api.XdmNode;
import no.entur.antu.organisation.OrganisationRepository;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.entur.netex.validation.validator.xpath.ValidationTreeFactory;
import org.entur.netex.validation.validator.xpath.XPathValidationContext;
import org.entur.netex.validation.validator.xpath.XPathValidator;
import org.entur.netex.validation.xml.XMLParserUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

class XpathValidatorTest {

    private static final String TEST_DATASET_AUTHORITY_VALIDATION_FILE_NAME = "rb_flb-aggregated-netex.zip";

    @Test
    void testValidator() throws IOException {

        OrganisationRepository stubOrganisationRepository = new OrganisationRepository() {
            @Override
            public void refreshCache() {
            }

            @Override
            public Set<String> getWhitelistedAuthorityIds(String codespace) {
                return Set.of("FLB:Authority:XXX", "FLB:Authority:YYY");
            }
        };
        ValidationTreeFactory validationTreeFactory = new EnturValidationTreeFactory(stubOrganisationRepository);
        XPathValidator xPathValidator = new XPathValidator(validationTreeFactory);

        InputStream testDatasetAsStream = getClass().getResourceAsStream('/' + TEST_DATASET_AUTHORITY_VALIDATION_FILE_NAME);
        assert testDatasetAsStream != null;

        List<ValidationReportEntry> validationReportEntries = new ArrayList<>();

        try (ZipInputStream zipInputStream = new ZipInputStream(testDatasetAsStream)) {

            ZipEntry zipEntry = zipInputStream.getNextEntry();
            while (zipEntry != null) {
                byte[] content = zipInputStream.readAllBytes();
                XdmNode document = XMLParserUtil.parseFileToXdmNode(content);
                XPathValidationContext xPathValidationContext = new XPathValidationContext(document, XMLParserUtil.getXPathCompiler(), "FLB", zipEntry.getName());
                validationReportEntries.addAll(xPathValidator.validate(xPathValidationContext));
                zipEntry = zipInputStream.getNextEntry();
            }
            Assertions.assertFalse(validationReportEntries.isEmpty());
        }
    }

}