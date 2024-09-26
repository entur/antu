package no.entur.antu.validation;

import java.util.List;
import java.util.Set;
import net.sf.saxon.s9api.XdmNode;
import org.entur.netex.validation.validator.id.IdVersion;
import org.entur.netex.validation.validator.xpath.XPathValidationContext;
import org.entur.netex.validation.xml.NetexXMLParser;

/**
 * Extends the XPathValidationContext with NetexEntitiesIndex, which is the in memory index of the Netex dataset.
 */
public class ValidationContextWithNetexEntitiesIndex
  extends XPathValidationContext {

  /**
   * The supplier of the NetexEntitiesIndex.
   */
  private final AntuNetexData antuNetexData;

  public ValidationContextWithNetexEntitiesIndex(
    XdmNode document,
    NetexXMLParser netexXMLParser,
    AntuNetexData antuNetexData,
    String codespace,
    String fileName,
    Set<IdVersion> localIds,
    List<IdVersion> localRefs
  ) {
    super(document, netexXMLParser, codespace, fileName, localIds, localRefs);
    this.antuNetexData = antuNetexData;
  }

  /**
   * Gets the AntuNetexData.
   * @return the AntuNetexData
   */
  public AntuNetexData getAntuNetexData() {
    return antuNetexData;
  }
}
