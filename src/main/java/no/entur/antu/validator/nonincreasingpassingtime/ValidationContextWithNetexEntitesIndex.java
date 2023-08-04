package no.entur.antu.validator.nonincreasingpassingtime;

import net.sf.saxon.s9api.XdmNode;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.id.IdVersion;
import org.entur.netex.validation.validator.xpath.ValidationContext;
import org.entur.netex.validation.xml.NetexXMLParser;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class ValidationContextWithNetexEntitesIndex extends ValidationContext {

    private final Supplier<NetexEntitiesIndex> getNetexEntitiesIndex;
    private NetexEntitiesIndex netexEntitiesIndex;

    public ValidationContextWithNetexEntitesIndex(XdmNode document,
                                                  NetexXMLParser netexXMLParser,
                                                  Supplier<NetexEntitiesIndex> getNetexEntitiesIndex,
                                                  String codespace,
                                                  String fileName,
                                                  Set<IdVersion> localIds,
                                                  List<IdVersion> localRefs) {
        super(document, netexXMLParser, codespace, fileName, localIds, localRefs);
        this.getNetexEntitiesIndex = getNetexEntitiesIndex;
    }

    public NetexEntitiesIndex getNetexEntitiesIndex() {
        if (netexEntitiesIndex == null) {
            netexEntitiesIndex = this.getNetexEntitiesIndex.get();
        }
        return netexEntitiesIndex;
    }
}
