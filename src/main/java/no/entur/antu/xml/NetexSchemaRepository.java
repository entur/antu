package no.entur.antu.xml;

import no.entur.antu.exception.AntuException;
import org.rutebanken.netex.validation.NeTExValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.validation.Schema;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class NetexSchemaRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetexSchemaRepository.class);

    private final Map<NeTExValidator.NetexVersion, Schema> netexSchema;

    public NetexSchemaRepository() {
        this.netexSchema = new ConcurrentHashMap<>();
    }

    public Schema getNetexSchema(NeTExValidator.NetexVersion version) {
        return netexSchema.computeIfAbsent(version, netexVersion -> createNetexSchema(version));
    }

    private static Schema createNetexSchema(NeTExValidator.NetexVersion version) {
        LOGGER.info("Initializing Netex schema version {}, this may take a few seconds", version);
        try {
            return new NeTExValidator(version).getSchema();
        } catch (IOException | SAXException e) {
            throw new AntuException("Could not create NeTEx schema", e);
        }
    }

    public static NeTExValidator.NetexVersion detectNetexSchemaVersion(byte[] content) {
        String profileVersion = PublicationDeliveryVersionAttributeReader.findPublicationDeliveryVersion(content);
        String netexSchemaVersion = getSchemaVersion(profileVersion);

        if (netexSchemaVersion != null) {
            switch (netexSchemaVersion) {
                case "1.04":
                    return NeTExValidator.NetexVersion.V1_0_4beta;
                case "1.07":
                    return NeTExValidator.NetexVersion.V1_0_7;
                case "1.08":
                    return NeTExValidator.NetexVersion.v1_0_8;
                case "1.09":
                    return NeTExValidator.NetexVersion.v1_0_9;
                case "1.10":
                    return NeTExValidator.NetexVersion.v1_10;
                case "1.11":
                    return NeTExValidator.NetexVersion.v1_11;
                case "1.12":
                    return NeTExValidator.NetexVersion.v1_12;
                case "1.13":
                    return NeTExValidator.NetexVersion.v1_13;
                default:
            }
        }
        return null;
    }

    //1.04:NO-NeTEx-networktimetable:1.0
    private static String getSchemaVersion(String fullProfileString) {
        if (fullProfileString != null) {
            String[] split = fullProfileString.split(":");

            if (split.length == 3) {
                // Valid
                return split[0];
            }
        }
        return null;
    }
}
