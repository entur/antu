package no.entur.antu.model;

import java.util.Objects;
import java.util.Optional;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.streams.XdmStream;
import no.entur.antu.exception.AntuException;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.VehicleJourneyRefStructure;

public record ServiceJourneyId(String id) {
  public ServiceJourneyId {
    Objects.requireNonNull(id, "Service journey id should not be null");
    if (!isValid(id)) {
      throw new AntuException("Invalid service journey id: " + id);
    }
  }

  public static ServiceJourneyId ofValidId(ServiceJourney serviceJourney) {
    return Optional
      .of(serviceJourney)
      .map(ServiceJourney::getId)
      .map(ServiceJourneyId::ofValidId)
      .orElse(null);
  }

  public static ServiceJourneyId ofValidId(
    VehicleJourneyRefStructure vehicleJourneyRefStructure
  ) {
    return Optional
      .of(vehicleJourneyRefStructure)
      .map(VehicleJourneyRefStructure::getRef)
      .map(ServiceJourneyId::ofValidId)
      .orElse(null);
  }

  public static ServiceJourneyId ofValidId(String id) {
    return Optional
      .ofNullable(id)
      .filter(ServiceJourneyId::isValid)
      .map(ServiceJourneyId::new)
      .orElse(null);
  }

  public static ServiceJourneyId ofNullable(String id) {
    return id == null ? null : new ServiceJourneyId(id);
  }

  public static boolean isValid(String serviceJourneyId) {
    return serviceJourneyId.contains(":ServiceJourney:");
  }

  public static ServiceJourneyId ofValidId(XdmItem serviceJourneyItem) {
    return Optional
      .ofNullable(serviceJourneyItem)
      .map(XdmItem::stream)
      .map(XdmStream::asNode)
      .map(node -> node.attribute("id"))
      .map(ServiceJourneyId::ofValidId)
      .orElse(null);
  }

  /*
   * Used to encode data to store in redis.
   * Caution: Changes in this method can effect data stored in redis.
   */
  @Override
  public String toString() {
    return id();
  }
}
