package no.entur.antu.validation.validator.xpath;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import net.sf.saxon.s9api.XdmNode;
import no.entur.antu.organisation.OrganisationRepository;
import org.entur.netex.validation.configuration.DefaultValidationConfigLoader;
import org.entur.netex.validation.validator.DefaultValidationEntryFactory;
import org.entur.netex.validation.validator.ValidationReportEntry;
import org.entur.netex.validation.validator.xpath.ValidationTreeFactory;
import org.entur.netex.validation.validator.xpath.XPathRuleValidationContext;
import org.entur.netex.validation.validator.xpath.XPathRuleValidator;
import org.entur.netex.validation.xml.NetexXMLParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class XpathValidatorIntegrationTest {

  private static final String TEST_DATASET_AUTHORITY_VALIDATION_FILE_NAME =
    "rb_flb-aggregated-netex.zip";
  private static final String CONFIGURATION_ANTU_YAML =
    "configuration.antu.yaml";

  @Test
  void testValidator() throws IOException {
    OrganisationRepository stubOrganisationRepository =
      new OrganisationRepository() {
        @Override
        public void refreshCache() {}

        @Override
        public boolean isEmpty() {
          return false;
        }

        @Override
        public Set<String> getWhitelistedAuthorityIds(String codespace) {
          return Set.of("FLB:Authority:XXX", "FLB:Authority:YYY");
        }
      };
    ValidationTreeFactory validationTreeFactory =
      new EnturTimetableDataValidationTreeFactory(stubOrganisationRepository);
    NetexXMLParser netexXMLParser = new NetexXMLParser(Set.of("SiteFrame"));
    XPathRuleValidator xPathValidator = new XPathRuleValidator(
      validationTreeFactory,
      new DefaultValidationEntryFactory(
        new DefaultValidationConfigLoader(CONFIGURATION_ANTU_YAML)
      )
    );

    InputStream testDatasetAsStream = getClass()
      .getResourceAsStream('/' + TEST_DATASET_AUTHORITY_VALIDATION_FILE_NAME);
    assert testDatasetAsStream != null;

    List<ValidationReportEntry> validationReportEntries = new ArrayList<>();

    try (
      ZipInputStream zipInputStream = new ZipInputStream(testDatasetAsStream)
    ) {
      ZipEntry zipEntry = zipInputStream.getNextEntry();
      while (zipEntry != null) {
        byte[] content = zipInputStream.readAllBytes();
        XdmNode document = netexXMLParser.parseByteArrayToXdmNode(content);
        XPathRuleValidationContext xPathValidationContext =
          new XPathRuleValidationContext(
            document,
            netexXMLParser,
            "FLB",
            zipEntry.getName()
          );
        validationReportEntries.addAll(
          xPathValidator.validate(xPathValidationContext)
        );
        zipEntry = zipInputStream.getNextEntry();
      }
      Assertions.assertFalse(validationReportEntries.isEmpty());
    }
  }
}
