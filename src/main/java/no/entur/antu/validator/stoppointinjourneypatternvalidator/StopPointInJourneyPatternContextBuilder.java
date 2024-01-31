package no.entur.antu.validator.stoppointinjourneypatternvalidator;

import java.util.List;
import java.util.function.Function;
import no.entur.antu.commondata.CommonDataRepository;
import no.entur.antu.model.QuayId;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.rutebanken.netex.model.*;

public class StopPointInJourneyPatternContextBuilder {

  private final CommonDataRepository commonDataRepository;
  private final NetexEntitiesIndex netexEntitiesIndex;

  public StopPointInJourneyPatternContextBuilder(
    CommonDataRepository commonDataRepository,
    NetexEntitiesIndex netexEntitiesIndex
  ) {
    this.commonDataRepository = commonDataRepository;
    this.netexEntitiesIndex = netexEntitiesIndex;
  }

  public record StopPointInJourneyPatternContext(
    JourneyPattern journeyPattern, // trenger bare ID?
    StopPointInJourneyPattern stopPointInJourneyPattern,
    QuayId quayId
  ) {}

  public List<StopPointInJourneyPatternContext> build(
    JourneyPattern journeyPattern,
    String validationReportId
  ) {
    Function<String, QuayId> quayIdForScheduleStopPoint =
      scheduledStopPointRef ->
        commonDataRepository.hasQuayIds(validationReportId)
          ? commonDataRepository.findQuayIdForScheduledStopPoint(
            scheduledStopPointRef,
            validationReportId
          )
          : new QuayId(
            netexEntitiesIndex
              .getQuayIdByStopPointRefIndex()
              .get(scheduledStopPointRef)
          );

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
          quayIdForScheduleStopPoint.apply(
            stopPointInJourneyPattern
              .getScheduledStopPointRef()
              .getValue()
              .getRef()
          )
        )
      )
      .toList();
  }
}
