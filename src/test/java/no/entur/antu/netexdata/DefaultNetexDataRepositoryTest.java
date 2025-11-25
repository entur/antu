package no.entur.antu.netexdata;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import no.entur.antu.memorystore.LineInfoMemStoreRepository;
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
  private Map<String, Set<String>> scheduledStopPointIdsCache;

  /**
   * Simple in-memory implementation of LineInfoMemStoreRepository for testing.
   */
  private static class InMemoryLineInfoRepository
    implements LineInfoMemStoreRepository {

    private final Map<String, List<String>> cache = new HashMap<>();

    @Override
    public void addLineInfo(String validationReportId, String lineInfoString) {
      cache
        .computeIfAbsent(validationReportId, k -> new java.util.ArrayList<>())
        .add(lineInfoString);
    }

    @Override
    public List<String> getLineInfo(String validationReportId) {
      return cache.get(validationReportId);
    }

    @Override
    public void removeLineInfo(String validationReportId) {
      cache.remove(validationReportId);
    }

    public Map<String, List<String>> getCache() {
      return cache;
    }
  }

  @BeforeEach
  void setUp() {
    InMemoryLineInfoRepository lineInfoRepository =
      new InMemoryLineInfoRepository();
    lineInfoCache = lineInfoRepository.getCache();
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

    scheduledStopPointIdsCache = new HashMap<>();

    this.repository =
      new DefaultNetexDataRepository(
        lineInfoRepository,
        serviceJourneyStopsCache,
        serviceJourneyInterchangeInfoCache,
        activeDatesByServiceJourneyId,
        dayTypeActiveDatesCache,
        operatingDayActiveDateCache,
        scheduledStopPointIdsCache
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
    assertTrue(scheduledStopPointIdsCache.isEmpty());
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
