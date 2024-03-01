package no.entur.antu.validator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import net.sf.saxon.s9api.XdmNode;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.id.IdVersion;
import org.entur.netex.validation.validator.xpath.ValidationContext;
import org.entur.netex.validation.xml.NetexXMLParser;

/**
 * Extends the ValidationContext with NetexEntitiesIndex, which is the in memory index of the Netex dataset.
 */
public class ValidationContextWithNetexEntitiesIndex extends ValidationContext {

  /**
   * The supplier of the NetexEntitiesIndex.
   */
  private final Supplier<NetexEntitiesIndex> getNetexEntitiesIndex;
  private NetexEntitiesIndex netexEntitiesIndex;

  private Map<String, IdVersion> localIdsMap = new HashMap<>();

  public ValidationContextWithNetexEntitiesIndex(
    XdmNode document,
    NetexXMLParser netexXMLParser,
    Supplier<NetexEntitiesIndex> getNetexEntitiesIndex,
    String codespace,
    String fileName,
    Set<IdVersion> localIds,
    List<IdVersion> localRefs
  ) {
    super(document, netexXMLParser, codespace, fileName, localIds, localRefs);
    this.getNetexEntitiesIndex = getNetexEntitiesIndex;
    localIds.forEach(idVersion -> localIdsMap.put(idVersion.getId(), idVersion)
    );
  }

  public Map<String, IdVersion> getLocalIdsMap() {
    return localIdsMap;
  }

  /**
   * Gets the NetexEntitiesIndex.
   * @return the NetexEntitiesIndex
   */
  public NetexEntitiesIndex getNetexEntitiesIndex() {
    if (netexEntitiesIndex == null) {
      netexEntitiesIndex = this.getNetexEntitiesIndex.get();
    }
    return netexEntitiesIndex;
  }
}
