package no.entur.antu.validation;

import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
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

/**
 * Represents a Netex validator runner with NetexEntitiesIndex.
 */
public class NetexValidatorsRunnerWithNetexEntitiesIndex
  extends NetexValidatorsRunner {

  private final NetexXMLParser netexXMLParser;

  public NetexValidatorsRunnerWithNetexEntitiesIndex(
    NetexXMLParser netexXMLParser,
    NetexSchemaValidator netexSchemaValidator,
    List<NetexValidator> netexValidators
  ) {
    super(netexXMLParser, netexSchemaValidator, netexValidators);
    this.netexXMLParser = netexXMLParser;
  }

  @Override
  protected ValidationContext prepareValidationContext(
    String codespace,
    String filename,
    byte[] fileContent
  ) {
    XdmNode document = netexXMLParser.parseByteArrayToXdmNode(fileContent);
    XPathCompiler xPathCompiler = netexXMLParser.getXPathCompiler();
    Set<IdVersion> localIds = new HashSet<>(
      NetexIdExtractorHelper.collectEntityIdentifiers(
        document,
        xPathCompiler,
        filename,
        Set.of("Codespace")
      )
    );
    List<IdVersion> localRefs = NetexIdExtractorHelper.collectEntityReferences(
      document,
      xPathCompiler,
      filename,
      null
    );

    /*
     * Supplier of the NetexEntitiesIndex.
     */
    Supplier<NetexEntitiesIndex> getNetexEntitiesIndex = () -> {
      NetexParser netexParser = new NetexParser();
      return netexParser.parse(new ByteArrayInputStream(fileContent));
    };

    return new ValidationContextWithNetexEntitiesIndex(
      document,
      netexXMLParser,
      getNetexEntitiesIndex,
      codespace,
      filename,
      localIds,
      localRefs
    );
  }
}
