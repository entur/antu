package no.entur.antu.validation;

import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XdmNode;
import no.entur.antu.commondata.CommonDataRepository;
import no.entur.antu.commondata.CommonDataScraper;
import no.entur.antu.stop.StopPlaceRepository;
import org.entur.netex.NetexParser;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.NetexDatasetValidator;
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
  private final CommonDataRepository commonDataRepository;
  private final StopPlaceRepository stopPlaceRepository;
  private final List<CommonDataScraper> commonDataScrapers;

  public NetexValidatorsRunnerWithNetexEntitiesIndex(
    NetexXMLParser netexXMLParser,
    NetexSchemaValidator netexSchemaValidator,
    List<NetexValidator> netexValidators,
    CommonDataRepository commonDataRepository,
    StopPlaceRepository stopPlaceRepository
  ) {
    super(netexXMLParser, netexSchemaValidator, netexValidators, List.of());
    this.netexXMLParser = netexXMLParser;
    this.commonDataRepository = commonDataRepository;
    this.stopPlaceRepository = stopPlaceRepository;
    commonDataScrapers = List.of();
  }

  public NetexValidatorsRunnerWithNetexEntitiesIndex(
    NetexXMLParser netexXMLParser,
    NetexSchemaValidator netexSchemaValidator,
    List<NetexValidator> netexValidators,
    List<NetexDatasetValidator> aggregatedNetexValidators,
    List<CommonDataScraper> commonDataScrapers,
    CommonDataRepository commonDataRepository,
    StopPlaceRepository stopPlaceRepository
  ) {
    super(
      netexXMLParser,
      netexSchemaValidator,
      netexValidators,
      aggregatedNetexValidators
    );
    this.netexXMLParser = netexXMLParser;
    this.commonDataScrapers = commonDataScrapers;
    this.commonDataRepository = commonDataRepository;
    this.stopPlaceRepository = stopPlaceRepository;
  }

  @Override
  protected ValidationContext prepareValidationContext(
    String validationReportId,
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

    NetexParser netexParser = new NetexParser();
    NetexEntitiesIndex netexEntitiesIndex = netexParser.parse(new ByteArrayInputStream(fileContent));

    AntuNetexData antuNetexData = new AntuNetexData(
      validationReportId,
      netexEntitiesIndex,
      commonDataRepository,
      stopPlaceRepository
    );

    return new ValidationContextWithNetexEntitiesIndex(
      document,
      netexXMLParser,
      antuNetexData,
      codespace,
      filename,
      localIds,
      localRefs
    );
  }

  @Override
  protected void postPreparedValidationContext(ValidationContext validationContext) {
    if (!validationContext.isCommonFile()) {
      commonDataScrapers.forEach(
        commonDataScraper -> commonDataScraper.scrapeCommonData(
          validationContext
        )
      );
    }
  }
}
