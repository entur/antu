package no.entur.antu.validator.journeypattern.stoppoint.stoppointscount;

import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.LinksInJourneyPattern_RelStructure;

record StopPointsCountContext(
  String journeyPatternId,
  int stopPointsCount,
  int serviceLinksCount
) {
  public static StopPointsCountContext of(JourneyPattern journeyPattern) {
    LinksInJourneyPattern_RelStructure linksInSequence =
      journeyPattern.getLinksInSequence();

    if (linksInSequence == null) {
      return null;
    }

    return new StopPointsCountContext(
      journeyPattern.getId(),
      journeyPattern
        .getPointsInSequence()
        .getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern()
        .size(),
      linksInSequence
        .getServiceLinkInJourneyPatternOrTimingLinkInJourneyPattern()
        .size()
    );
  }

  public boolean isValid() {
    return stopPointsCount == serviceLinksCount + 1;
  }
}
