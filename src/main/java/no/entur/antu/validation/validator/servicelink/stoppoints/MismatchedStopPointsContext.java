package no.entur.antu.validation.validator.servicelink.stoppoints;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import no.entur.antu.model.ScheduledStopPointId;
import no.entur.antu.model.ScheduledStopPointIds;
import no.entur.antu.model.ServiceLinkId;
import no.entur.antu.validation.AntuNetexData;
import org.rutebanken.netex.model.JourneyPattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record MismatchedStopPointsContext(
  String journeyPatternId,
  List<ScheduledStopPointId> stopPointsInJourneyPattern,
  List<ServiceLinkId> linksInJourneyPattern,
  Map<ServiceLinkId, ScheduledStopPointIds> stopPointsForServiceLinksInJourneyPattern,
  Map<ServiceLinkId, ScheduledStopPointIds> stopPointsInServiceLink
) {
  public static final class Builder {

    private static final Logger LOGGER = LoggerFactory.getLogger(Builder.class);

    private final AntuNetexData antuNetexData;

    public Builder(AntuNetexData antuNetexData) {
      this.antuNetexData = antuNetexData;
    }

    public MismatchedStopPointsContext build(JourneyPattern journeyPattern) {
      List<ServiceLinkId> serviceLinkIdsInJourneyPattern = AntuNetexData
        .linksInJourneyPattern(journeyPattern)
        .map(ServiceLinkId::of)
        .toList();
      List<ScheduledStopPointId> stopPointsInJourneyPattern = AntuNetexData
        .stopPointsInJourneyPattern(journeyPattern)
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
                new ScheduledStopPointIds(
                  stopPointsInJourneyPattern.get(index),
                  stopPointsInJourneyPattern.get(index + 1)
                )
            )
          ),
        serviceLinkIdsInJourneyPattern
          .stream()
          .map(serviceLinkId -> new AbstractMap.SimpleEntry<>(serviceLinkId,
                                                              antuNetexData.findScheduledStopPointsForServiceLinkId(
                                                                serviceLinkId)
          ))
          .filter(entry -> entry.getValue() != null)
          .collect(
            Collectors.toMap(
              Map.Entry::getKey,
              Map.Entry::getValue
            )
          )
      );
    }
  }
}
