package no.entur.antu.netexdata;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.entur.netex.validation.validator.model.ServiceJourneyId;
import org.entur.netex.validation.validator.model.ServiceJourneyStop;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultNetexDataRepositoryTest {

  private final String REPORT_ID = "report1";

  private DefaultNetexDataRepository repository;

  private Map<String, List<String>> lineInfoCache;
  private Map<String, Map<String, List<ServiceJourneyStop>>> serviceJourneyStopsCache;
  private Map<String, List<String>> serviceJourneyInterchangeInfoCache;
  private Map<String, Map<ServiceJourneyId, List<LocalDateTime>>> activeDatesByServiceJourneyId;
  private Map<String, Map<String, List<LocalDateTime>>> dayTypeActiveDatesCache;
  private Map<String, Map<String, LocalDateTime>> operatingDayActiveDateCache;

  @BeforeEach
  public void setUp() {
    lineInfoCache = new HashMap<>();
    lineInfoCache.put(REPORT_ID, List.of("Line1", "Line2"));

    serviceJourneyStopsCache = new HashMap<>();
    serviceJourneyStopsCache.put(
      REPORT_ID,
      Map.of(
        "TST:ServiceJourney:1",
        List.of(new ServiceJourneyStop(null, null, null, 0, 0, true, true))
      )
    );

    serviceJourneyInterchangeInfoCache = new HashMap<>();
    serviceJourneyInterchangeInfoCache.put(
      REPORT_ID,
      List.of("InterchangeInfo1")
    );

    activeDatesByServiceJourneyId = new HashMap<>();
    activeDatesByServiceJourneyId.put(
      REPORT_ID,
      Map.of(
        new ServiceJourneyId("TST:ServiceJourney:1"),
        List.of(LocalDateTime.now())
      )
    );

    dayTypeActiveDatesCache = new HashMap<>();
    dayTypeActiveDatesCache.put(
      REPORT_ID,
      Map.of("DayType1", List.of(LocalDateTime.now()))
    );

    operatingDayActiveDateCache = new HashMap<>();
    operatingDayActiveDateCache.put(
      REPORT_ID,
      Map.of("OperatingDay1", LocalDateTime.now())
    );

    this.repository =
      new DefaultNetexDataRepository(
        lineInfoCache,
        serviceJourneyStopsCache,
        serviceJourneyInterchangeInfoCache,
        activeDatesByServiceJourneyId,
        dayTypeActiveDatesCache,
        operatingDayActiveDateCache
      );
  }

  @Test
  void testCleanUp() {
    repository.cleanUp(REPORT_ID);
    assertTrue(lineInfoCache.isEmpty());
    assertTrue(serviceJourneyStopsCache.isEmpty());
    assertTrue(serviceJourneyInterchangeInfoCache.isEmpty());
    assertTrue(activeDatesByServiceJourneyId.isEmpty());
    assertTrue(dayTypeActiveDatesCache.isEmpty());
    assertTrue(operatingDayActiveDateCache.isEmpty());
  }
}
