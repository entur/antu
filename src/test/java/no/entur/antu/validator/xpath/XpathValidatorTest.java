package no.entur.antu.validator.xpath;

import net.sf.saxon.s9api.SaxonApiException;
import no.entur.antu.organisation.OrganisationRepository;
import no.entur.antu.validator.ValidationReportEntry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.xml.stream.XMLStreamException;
import javax.xml.xpath.XPathExpressionException;
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
    void testValidator() throws IOException, XMLStreamException, SaxonApiException, XPathExpressionException {

        XPathValidator xPathValidator = new XPathValidator(new OrganisationRepository() {
            @Override
            public void refreshCache() {
            }

            @Override
            public Set<String> getWhitelistedAuthorityIds(String codespace) {
                return Set.of("FLB:Authority:XXX", "FLB:Authority:YYY");
            }
        });

        InputStream testDatasetAsStream = getClass().getResourceAsStream('/' + TEST_DATASET_AUTHORITY_VALIDATION_FILE_NAME);
        assert testDatasetAsStream != null;

        List<ValidationReportEntry> validationReportEntries = new ArrayList<>();

        try (ZipInputStream zipInputStream = new ZipInputStream(testDatasetAsStream)) {

            ZipEntry zipEntry = zipInputStream.getNextEntry();
            while (zipEntry != null) {
                byte[] content = zipInputStream.readAllBytes();
                validationReportEntries.addAll(xPathValidator.validate("FLB", zipEntry.getName(), content));
                zipEntry = zipInputStream.getNextEntry();
            }
            Assertions.assertFalse(validationReportEntries.isEmpty());
        }
    }


}