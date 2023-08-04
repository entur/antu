package no.entur.antu.validator.stoppointinjourneypatternvalidator;

import java.util.List;
import no.entur.antu.commondata.CommonDataRepository;
import no.entur.antu.model.QuayId;
import org.rutebanken.netex.model.*;

public class StopPointInJourneyPatternContextBuilder {

  private final String validationReportId;
  private final CommonDataRepository commonDataRepository;

  public StopPointInJourneyPatternContextBuilder(
    String validationReportId,
    CommonDataRepository commonDataRepository
  ) {
    this.validationReportId = validationReportId;
    this.commonDataRepository = commonDataRepository;
  }

  public record StopPointInJourneyPatternContext(
    JourneyPattern journeyPattern, // trenger bare ID?
    StopPointInJourneyPattern stopPointInJourneyPattern,
    QuayId quayId
  ) {}

  public List<StopPointInJourneyPatternContext> build(
    JourneyPattern journeyPattern
  ) {
    return journeyPattern
      .getPointsInSequence()
      .getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern()
      .stream()
      .filter(StopPointInJourneyPattern.class::isInstance)
      .map(StopPointInJourneyPattern.class::cast)
      .map(stopPointInJourneyPattern ->
        new StopPointInJourneyPatternContext(
          journeyPattern,
          stopPointInJourneyPattern,
          commonDataRepository.findQuayIdForScheduledStopPoint(
            stopPointInJourneyPattern
              .getScheduledStopPointRef()
              .getValue()
              .getRef(),
            validationReportId
          )
        )
      )
      .toList();
  }
}
