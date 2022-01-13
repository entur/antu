package no.entur.antu.validator;

import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XdmNode;
import no.entur.antu.validator.id.IdVersion;
import no.entur.antu.validator.id.NetexIdExtractorHelper;
import no.entur.antu.validator.schema.NetexSchemaValidator;
import no.entur.antu.validator.xpath.ValidationContext;
import no.entur.antu.xml.XMLParserUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NetexValidatorsRunner {

    private final NetexSchemaValidator netexSchemaValidator;
    private final List<NetexValidator> netexValidators;

    public NetexValidatorsRunner(NetexSchemaValidator netexSchemaValidator, List<NetexValidator> netexValidators) {
        this.netexSchemaValidator = netexSchemaValidator;
        this.netexValidators = netexValidators;
    }

    public ValidationReport validate(String codespace, String validationReportId, String filename, byte[] fileContent) throws SaxonApiException {
        ValidationReport validationReport = new ValidationReport(codespace, validationReportId);
        validationReport.addAllValidationReportEntries(netexSchemaValidator.validateSchema(filename, fileContent));
        if (validationReport.hasError()) {
            // do not run subsequent validators if the XML Schema validation fails
            return validationReport;
        }

        XdmNode document = XMLParserUtil.parseFileToXdmNode(fileContent);
        XPathCompiler xPathCompiler = XMLParserUtil.getXPathCompiler();
        Set<IdVersion> localIds = new HashSet<>(NetexIdExtractorHelper.collectEntityIdentificators(document, xPathCompiler, filename, Set.of("Codespace")));
        List<IdVersion> localRefs = NetexIdExtractorHelper.collectEntityReferences(document, xPathCompiler, filename, null);

        ValidationContext validationContext = new ValidationContext(document, xPathCompiler, codespace, filename, localIds, localRefs);

        netexValidators.forEach(netexValidator -> netexValidator.validate(validationReport, validationContext));

        return validationReport;

    }

}
