package no.entur.antu.validation.validator.xpath;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import net.sf.saxon.s9api.XdmNode;
import no.entur.antu.organisation.OrganisationRepository;
import no.entur.antu.organisation.SimpleOrganisationAliasRepository;
import no.entur.antu.organisation.SimpleOrganisationRepository;
import no.entur.antu.validation.validator.organisation.OrganisationAliasRepository;
import org.entur.netex.validation.validator.ValidationIssue;
import org.entur.netex.validation.validator.xpath.ValidationTreeFactory;
import org.entur.netex.validation.validator.xpath.XPathRuleValidationContext;
import org.entur.netex.validation.validator.xpath.XPathRuleValidator;
import org.entur.netex.validation.xml.NetexXMLParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class XpathValidatorIntegrationTest {

  private static final String TEST_DATASET_AUTHORITY_VALIDATION_FILE_NAME =
    "rb_flb-aggregated-netex.zip";
  private static final String TEST_CODESPACE = "FLB";

  @Test
  void testValidator() throws IOException {
    OrganisationRepository stubOrganisationRepository =
      new SimpleOrganisationRepository(
        Map.of(TEST_CODESPACE, Set.of("FLB:Authority:XXX", "FLB:Authority:YYY"))
      );
    OrganisationAliasRepository stubOrganisationAliasRepository =
      new SimpleOrganisationAliasRepository(new HashSet<>());
    ValidationTreeFactory validationTreeFactory =
      new EnturTimetableDataValidationTreeFactory(
        stubOrganisationRepository,
        stubOrganisationAliasRepository
      );
    NetexXMLParser netexXMLParser = new NetexXMLParser(Set.of("SiteFrame"));
    XPathRuleValidator xPathValidator = new XPathRuleValidator(
      validationTreeFactory
    );

    InputStream testDatasetAsStream = getClass()
      .getResourceAsStream('/' + TEST_DATASET_AUTHORITY_VALIDATION_FILE_NAME);
    assert testDatasetAsStream != null;

    List<ValidationIssue> validationReportEntries = new ArrayList<>();

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
