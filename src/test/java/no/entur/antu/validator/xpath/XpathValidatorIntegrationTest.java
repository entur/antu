package no.entur.antu.validator.xpath;

import net.sf.saxon.s9api.XdmNode;
import no.entur.antu.commondata.CommonDataRepository;
import no.entur.antu.organisation.OrganisationRepository;
import no.entur.antu.stop.model.QuayId;
import org.entur.netex.validation.configuration.DefaultValidationConfigLoader;
import org.entur.netex.validation.validator.DefaultValidationEntryFactory;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.entur.netex.validation.validator.xpath.ValidationTreeFactory;
import org.entur.netex.validation.validator.xpath.XPathValidationContext;
import org.entur.netex.validation.validator.xpath.XPathValidator;
import org.entur.netex.validation.xml.NetexXMLParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

class XpathValidatorIntegrationTest {

    private static final String TEST_DATASET_AUTHORITY_VALIDATION_FILE_NAME = "rb_flb-aggregated-netex.zip";
    private static final String CONFIGURATION_ANTU_YAML = "configuration.antu.yaml";

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
        ValidationTreeFactory validationTreeFactory = new EnturTimetableDataValidationTreeFactory(stubOrganisationRepository, null, null);
        NetexXMLParser netexXMLParser = new NetexXMLParser(Set.of("SiteFrame"));
        XPathValidator xPathValidator = new XPathValidator(validationTreeFactory, new DefaultValidationEntryFactory(new DefaultValidationConfigLoader(CONFIGURATION_ANTU_YAML)));

        InputStream testDatasetAsStream = getClass().getResourceAsStream('/' + TEST_DATASET_AUTHORITY_VALIDATION_FILE_NAME);
        assert testDatasetAsStream != null;

        List<ValidationReportEntry> validationReportEntries = new ArrayList<>();

        try (ZipInputStream zipInputStream = new ZipInputStream(testDatasetAsStream)) {

            ZipEntry zipEntry = zipInputStream.getNextEntry();
            while (zipEntry != null) {
                byte[] content = zipInputStream.readAllBytes();
                XdmNode document = netexXMLParser.parseByteArrayToXdmNode(content);
                XPathValidationContext xPathValidationContext = new XPathValidationContext(document, netexXMLParser, "FLB", zipEntry.getName());
                validationReportEntries.addAll(xPathValidator.validate(xPathValidationContext));
                zipEntry = zipInputStream.getNextEntry();
            }
            Assertions.assertFalse(validationReportEntries.isEmpty());
        }
    }

}