package no.entur.antu.netexdata;

import jakarta.xml.bind.JAXBElement;
import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import no.entur.antu.exception.AntuException;
import no.entur.antu.validation.AntuNetexData;
import org.entur.netex.NetexParser;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.model.FromToScheduledStopPointId;
import org.entur.netex.validation.validator.model.ServiceLinkId;
import org.rutebanken.netex.model.PassengerStopAssignment;
import org.rutebanken.netex.model.ServiceFrame;

/**
 * Represents a resource for common data.
 * This resource is used to load and retrieve common data from the Netex Common file,
 * and from the line files, which is used and to be validated across the line files.
 *  For example: For validating the duplicate line names and interchanges,
 *  need to be validated across the line files.
 */
public class NetexDataResource {

  private NetexEntitiesIndex netexEntitiesIndex;

  public void loadCommonData(byte[] fileContent) {
    NetexParser netexParser = new NetexParser();
    netexEntitiesIndex =
      netexParser.parse(new ByteArrayInputStream(fileContent));
  }

  protected NetexEntitiesIndex getCommonDataIndex() {
    if (netexEntitiesIndex == null) {
      throw new AntuException("Common dataset not loaded");
    }
    return netexEntitiesIndex;
  }

  public Map<String, String> getQuayIdsPerScheduledStopPoints() {
    return getCommonDataIndex()
      .getServiceFrames()
      .stream()
      .map(ServiceFrame::getStopAssignments)
      .filter(Objects::nonNull)
      .flatMap(stopAssignments -> stopAssignments.getStopAssignment().stream())
      .map(JAXBElement::getValue)
      .filter(PassengerStopAssignment.class::isInstance)
      .map(PassengerStopAssignment.class::cast)
      .collect(
        Collectors.toMap(
          passengerStopAssignment ->
            passengerStopAssignment
              .getScheduledStopPointRef()
              .getValue()
              .getRef(),
          passengerStopAssignment ->
            passengerStopAssignment.getQuayRef().getValue().getRef(),
          (v1, v2) -> v2
        )
      );
  }

  public Map<String, String> getFromToScheduledStopPointIdPerServiceLinkId() {
    return AntuNetexData
      .serviceLinks(getCommonDataIndex())
      .collect(
        Collectors.toMap(
          serviceLink -> ServiceLinkId.of(serviceLink).toString(),
          serviceLink -> FromToScheduledStopPointId.of(serviceLink).toString(),
          (v1, v2) -> v2
        )
      );
  }
}
