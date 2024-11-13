package no.entur.antu.validation.validator.servicelink.support;

import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.LinkInJourneyPattern;
import org.rutebanken.netex.model.LinkInLinkSequence_VersionedChildStructure;
import org.rutebanken.netex.model.LinksInJourneyPattern_RelStructure;

public class ServiceLinkUtils {

  private ServiceLinkUtils() {}

  /**
   * Find the links in journey pattern for the given journey pattern, sorted by order.
   */
  public static Stream<LinkInJourneyPattern> linksInJourneyPattern(
    JourneyPattern journeyPattern
  ) {
    return Optional
      .ofNullable(journeyPattern.getLinksInSequence())
      .map(
        LinksInJourneyPattern_RelStructure::getServiceLinkInJourneyPatternOrTimingLinkInJourneyPattern
      )
      .map(serviceLinksInJourneyPattern ->
        serviceLinksInJourneyPattern
          .stream()
          .filter(LinkInJourneyPattern.class::isInstance)
          .map(LinkInJourneyPattern.class::cast)
          .sorted(
            Comparator.comparing(
              LinkInLinkSequence_VersionedChildStructure::getOrder
            )
          )
      )
      .orElse(Stream.empty());
  }
}
