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

  private final String validationReportId = "report1";

  private DefaultNetexDataRepository repository;

  private Map<String, List<String>> lineInfoCache;
  private Map<String, Map<String, List<ServiceJourneyStop>>> serviceJourneyStopsCache;
  private Map<String, List<String>> serviceJourneyInterchangeInfoCache;
  private Map<String, Map<ServiceJourneyId, List<LocalDateTime>>> activeDatesByServiceJourneyId;
  private Map<String, Map<String, List<LocalDateTime>>> dayTypeActiveDatesCache;
  private Map<String, Map<String, LocalDateTime>> operatingDayActiveDateCache;

  @BeforeEach
  void setUp() {
    lineInfoCache = new HashMap<>();
    lineInfoCache.put(validationReportId, List.of("TST:Line:1", "TST:Line:2"));

    serviceJourneyStopsCache = new HashMap<>();
    serviceJourneyStopsCache.put(
      validationReportId,
      Map.of(
        "TST:ServiceJourney:1",
        List.of(new ServiceJourneyStop(null, null, null, 0, 0, true, true))
      )
    );

    serviceJourneyInterchangeInfoCache = new HashMap<>();
    serviceJourneyInterchangeInfoCache.put(
      validationReportId,
      List.of("TST:ServiceJourneyInterchange:1")
    );

    activeDatesByServiceJourneyId = new HashMap<>();
    activeDatesByServiceJourneyId.put(
      validationReportId,
      Map.of(
        new ServiceJourneyId("TST:ServiceJourney:1"),
        List.of(LocalDateTime.now())
      )
    );

    dayTypeActiveDatesCache = new HashMap<>();
    dayTypeActiveDatesCache.put(
      validationReportId,
      Map.of("DayType1", List.of(LocalDateTime.now()))
    );

    operatingDayActiveDateCache = new HashMap<>();
    operatingDayActiveDateCache.put(
      validationReportId,
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
    repository.cleanUp(validationReportId);
    assertTrue(lineInfoCache.isEmpty());
    assertTrue(serviceJourneyStopsCache.isEmpty());
    assertTrue(serviceJourneyInterchangeInfoCache.isEmpty());
    assertTrue(activeDatesByServiceJourneyId.isEmpty());
    assertTrue(dayTypeActiveDatesCache.isEmpty());
    assertTrue(operatingDayActiveDateCache.isEmpty());
  }

  @Test
  void testServiceJourneyStops() {
    Map<ServiceJourneyId, List<ServiceJourneyStop>> stops =
      repository.serviceJourneyStops(validationReportId);
    assertNotNull(stops);
    assertFalse(stops.isEmpty());
    assertTrue(stops.containsKey(new ServiceJourneyId("TST:ServiceJourney:1")));
    assertEquals(
      1,
      stops.get(new ServiceJourneyId("TST:ServiceJourney:1")).size()
    );
  }

  @Test
  void testActiveDatesByServiceJourneyId() {
    Map<ServiceJourneyId, List<LocalDateTime>> activeDates =
      repository.serviceJourneyIdToActiveDates(validationReportId);
    assertNotNull(activeDates);
    assertFalse(activeDates.isEmpty());
    assertTrue(
      activeDates.containsKey(new ServiceJourneyId("TST:ServiceJourney:1"))
    );
    assertEquals(
      1,
      activeDates.get(new ServiceJourneyId("TST:ServiceJourney:1")).size()
    );
  }
}
