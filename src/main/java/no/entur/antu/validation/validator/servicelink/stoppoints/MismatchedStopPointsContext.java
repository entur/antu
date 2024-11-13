package no.entur.antu.validation.validator.servicelink.stoppoints;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import no.entur.antu.validation.validator.servicelink.support.ServiceLinkUtils;
import no.entur.antu.validation.validator.support.NetexUtils;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
import org.entur.netex.validation.validator.model.FromToScheduledStopPointId;
import org.entur.netex.validation.validator.model.ScheduledStopPointId;
import org.entur.netex.validation.validator.model.ServiceLinkId;
import org.rutebanken.netex.model.JourneyPattern;

public record MismatchedStopPointsContext(
  String journeyPatternId,
  List<ScheduledStopPointId> stopPointsInJourneyPattern,
  List<ServiceLinkId> linksInJourneyPattern,
  Map<ServiceLinkId, FromToScheduledStopPointId> stopPointsForServiceLinksInJourneyPattern,
  Map<ServiceLinkId, FromToScheduledStopPointId> stopPointsInServiceLink
) {
  public static final class Builder {

    private final JAXBValidationContext validationContext;

    public Builder(JAXBValidationContext validationContext) {
      this.validationContext = validationContext;
    }

    public MismatchedStopPointsContext build(JourneyPattern journeyPattern) {
      List<ServiceLinkId> serviceLinkIdsInJourneyPattern = ServiceLinkUtils
        .linksInJourneyPattern(journeyPattern)
        .map(ServiceLinkId::of)
        .toList();
      List<ScheduledStopPointId> stopPointsInJourneyPattern = NetexUtils
        .stopPointsInJourneyPattern(journeyPattern)
        .stream()
        .map(ScheduledStopPointId::of)
        .toList();

      return new MismatchedStopPointsContext(
        journeyPattern.getId(),
        stopPointsInJourneyPattern,
        serviceLinkIdsInJourneyPattern,
        IntStream
          .range(0, serviceLinkIdsInJourneyPattern.size())
          .boxed()
          .collect(
            Collectors.toMap(
              serviceLinkIdsInJourneyPattern::get,
              index ->
                new FromToScheduledStopPointId(
                  stopPointsInJourneyPattern.get(index),
                  stopPointsInJourneyPattern.get(index + 1)
                )
            )
          ),
        serviceLinkIdsInJourneyPattern
          .stream()
          .map(serviceLinkId ->
            new AbstractMap.SimpleEntry<>(
              serviceLinkId,
              scheduledStopPointsForServiceLinkId(serviceLinkId)
            )
          )
          .filter(entry -> entry.getValue() != null)
          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
      );
    }

    public FromToScheduledStopPointId scheduledStopPointsForServiceLinkId(
      ServiceLinkId serviceLinkId
    ) {
      // Should extend this function to check line file if we don't find the
      // service links in common file. Same as findQuayIdForScheduledStopPoint.
      return validationContext
        .getNetexDataRepository()
        .fromToScheduledStopPointIdForServiceLink(
          serviceLinkId,
          validationContext.getValidationReportId()
        );
    }
  }
}
