package no.entur.antu.commondata;

import no.entur.antu.exception.AntuException;
import org.entur.netex.NetexParser;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.rutebanken.netex.model.PassengerStopAssignment;

import javax.xml.bind.JAXBElement;
import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.stream.Collectors;

public class CommonDataResource {

    private NetexEntitiesIndex netexEntitiesIndex;

    public void loadCommonData(byte[] fileContent) {
        NetexParser netexParser = new NetexParser();
        netexEntitiesIndex = netexParser.parse(new ByteArrayInputStream(fileContent));
    }

    protected NetexEntitiesIndex getCommonDataIndex() {
        if (netexEntitiesIndex == null) {
            throw new AntuException("Common dataset not loaded");
        }
        return netexEntitiesIndex;
    }

    public Map<String, String> getStopPlaceIdsPerScheduledStopPoints() {
        return getCommonDataIndex().getServiceFrames().stream()
                .flatMap(serviceFrame -> serviceFrame.getStopAssignments().getStopAssignment().stream())
                .map(JAXBElement::getValue)
                .filter(PassengerStopAssignment.class::isInstance)
                .map(PassengerStopAssignment.class::cast)
                .collect(Collectors.toMap(
                        passengerStopAssignment -> passengerStopAssignment.getScheduledStopPointRef().getValue().getRef(),
                        passengerStopAssignment -> passengerStopAssignment.getStopPlaceRef().getRef(),
                        (v1, v2) -> v2
                ));
    }
}