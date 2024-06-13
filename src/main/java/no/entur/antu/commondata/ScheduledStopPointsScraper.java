package no.entur.antu.commondata;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import no.entur.antu.model.ScheduledStopPointId;
import no.entur.antu.validation.AntuNetexData;
import no.entur.antu.validation.ValidationContextWithNetexEntitiesIndex;
import org.entur.netex.validation.validator.xpath.ValidationContext;

public class ScheduledStopPointsScraper implements CommonDataScraper {

  private final Map<String, Map<String, List<String>>> serviceJourneyToScheduledStopPointsCache;

  public ScheduledStopPointsScraper(
    Map<String, Map<String, List<String>>> serviceJourneyToScheduledStopPointsCache
  ) {
    this.serviceJourneyToScheduledStopPointsCache =
      serviceJourneyToScheduledStopPointsCache;
  }

  @Override
  public void scrapeData(ValidationContext validationContext) {
    if (
      validationContext instanceof ValidationContextWithNetexEntitiesIndex validationContextWithNetexEntitiesIndex
    ) {
      AntuNetexData antuNetexData =
        validationContextWithNetexEntitiesIndex.getAntuNetexData();

      if (antuNetexData.serviceJourneyInterchanges().findAny().isPresent()) {
        addScheduledStopPointsForServiceJourney(
          validationContextWithNetexEntitiesIndex
            .getAntuNetexData()
            .validationReportId(),
          antuNetexData
            .validServiceJourneys()
            .map(antuNetexData::scheduledStopPointIdByServiceJourneyId)
            .collect(
              Collectors.toMap(
                Map.Entry::getKey,
                entry ->
                  entry
                    .getValue()
                    .stream()
                    .map(ScheduledStopPointId::toString)
                    .collect(Collectors.toList())
              )
            )
        );
      }
    } else {
      throw new IllegalArgumentException(
        "ValidationContext must be of type ValidationContextWithNetexEntitiesIndex"
      );
    }
  }

  private void addScheduledStopPointsForServiceJourney(
    String validationReportId,
    Map<String, List<String>> scheduledStopPointsForServiceJourney
  ) {
    serviceJourneyToScheduledStopPointsCache.merge(
      validationReportId,
      scheduledStopPointsForServiceJourney,
      (existingMap, newMap) -> {
        existingMap.putAll(newMap);
        return existingMap;
      }
    );
  }
}
