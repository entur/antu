package no.entur.antu.services;

import org.entur.netex.NetexParser;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
public class NetexEntitiesIndexLoader {
    private static final Logger log = LoggerFactory.getLogger(NetexEntitiesIndexLoader.class);
    private final NetexEntitiesIndex netexEntitiesIndex;
    private final NetexParser parser = new NetexParser();

    @Autowired
    public NetexEntitiesIndexLoader(@Value("${no.entur.mummu.data-file}") String dataFile) throws IOException {
        long now = System.currentTimeMillis();
        netexEntitiesIndex = parser.parse(dataFile);
        log.info("Parsed NeTEx file in {} ms", System.currentTimeMillis() - now);
    }

    public NetexEntitiesIndex getNetexEntitiesIndex() {
        return netexEntitiesIndex;
    }

    public void updateWithPublicationDeliveryStream(InputStream publicationDeliveryStream) {
        long now = System.currentTimeMillis();
        parser.parse(publicationDeliveryStream, netexEntitiesIndex);
        log.info("Parsed NeTEx stream in {} ms", System.currentTimeMillis() - now);
    }
}
