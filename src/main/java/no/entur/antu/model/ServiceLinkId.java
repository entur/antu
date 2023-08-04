package no.entur.antu.model;

import java.util.Optional;
import no.entur.antu.exception.AntuException;
import org.rutebanken.netex.model.LinkInJourneyPattern;
import org.rutebanken.netex.model.ServiceLink;
import org.rutebanken.netex.model.VersionOfObjectRefStructure;

public record ServiceLinkId(String id) {
  public ServiceLinkId {
    if (!isValid(id)) {
      throw new AntuException("Invalid scheduled stop point id: " + id);
    }
  }

  public static ServiceLinkId of(ServiceLink serviceLink) {
    return new ServiceLinkId(serviceLink.getId());
  }

  public static ServiceLinkId of(LinkInJourneyPattern linkInJourneyPattern) {
    return Optional
      .ofNullable(linkInJourneyPattern)
      .map(LinkInJourneyPattern::getServiceLinkRef)
      .map(VersionOfObjectRefStructure::getRef)
      .map(ServiceLinkId::new)
      .orElse(null);
  }

  public static boolean isValid(String id) {
    return id != null && id.contains(":ServiceLink:");
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
