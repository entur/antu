package no.entur.antu.validation.validator.journeypattern.stoppoint.stoppointscount;

import no.entur.antu.validation.validator.support.NetexUtils;
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

    // If there are no service links in the journey pattern, then
    // the validation is not applicable.
    if (linksInSequence == null) {
      return null;
    }

    return new StopPointsCountContext(
      journeyPattern.getId(),
      NetexUtils
        .stopPointsInJourneyPattern(journeyPattern)
        .stream()
        .toList()
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
