package no.entur.antu.validator.nonincreasingpassingtime;

import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XdmNode;
import org.entur.netex.NetexParser;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.NetexValidator;
import org.entur.netex.validation.validator.NetexValidatorsRunner;
import org.entur.netex.validation.validator.id.IdVersion;
import org.entur.netex.validation.validator.id.NetexIdExtractorHelper;
import org.entur.netex.validation.validator.schema.NetexSchemaValidator;
import org.entur.netex.validation.validator.xpath.ValidationContext;
import org.entur.netex.validation.xml.NetexXMLParser;

import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class NetexValidatorRunnerWithNetexEntitiesIndex extends NetexValidatorsRunner {

    private final NetexXMLParser netexXMLParser;

    public NetexValidatorRunnerWithNetexEntitiesIndex(NetexXMLParser netexXMLParser, List<NetexValidator> netexValidators) {
        super(netexXMLParser, netexValidators);
        this.netexXMLParser = netexXMLParser;
    }

    public NetexValidatorRunnerWithNetexEntitiesIndex(NetexXMLParser netexXMLParser, NetexSchemaValidator netexSchemaValidator, List<NetexValidator> netexValidators) {
        super(netexXMLParser, netexSchemaValidator, netexValidators);
        this.netexXMLParser = netexXMLParser;
    }

    @Override
    protected ValidationContext prepareValidationContext(String codespace, String filename, byte[] fileContent) {
        XdmNode document = netexXMLParser.parseByteArrayToXdmNode(fileContent);
        XPathCompiler xPathCompiler = netexXMLParser.getXPathCompiler();
        Set<IdVersion> localIds = new HashSet<>(NetexIdExtractorHelper.collectEntityIdentifiers(document, xPathCompiler, filename, Set.of("Codespace")));
        List<IdVersion> localRefs = NetexIdExtractorHelper.collectEntityReferences(document, xPathCompiler, filename, null);

        Supplier<NetexEntitiesIndex> getNetexEntitiesIndex = () -> {
            NetexParser netexParser = new NetexParser();
            return netexParser.parse(new ByteArrayInputStream(fileContent));
        };

        return new ValidationContextWithNetexEntitesIndex(
                document, netexXMLParser, getNetexEntitiesIndex, codespace, filename, localIds, localRefs
        );
    }
}
