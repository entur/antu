package no.entur.antu.netex.test.repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.entur.netex.validation.validator.jaxb.NetexDataRepository;
import org.entur.netex.validation.validator.model.ActiveDates;
import org.entur.netex.validation.validator.model.ActiveDatesId;
import org.entur.netex.validation.validator.model.DayTypeId;
import org.entur.netex.validation.validator.model.OperatingDayId;
import org.entur.netex.validation.validator.model.ServiceJourneyId;
import org.entur.netex.validation.validator.model.ServiceJourneyInterchangeInfo;
import org.entur.netex.validation.validator.model.ServiceJourneyStop;
import org.entur.netex.validation.validator.model.SimpleLine;

/**
 * A NetexDataRepository backed by plain maps, mutable after construction.
 *
 * <p>Everything is keyed by validation report id, and stays that way: the interchange tests use a
 * different report id per scenario as a scenario key.
 *
 * <p>Reads of an unprimed key return empty rather than null. The alternative is a NullPointerException
 * from a validator that asks a question this test never set up an answer for.
 */
public class TestNetexDataRepository implements NetexDataRepository {

  private final Map<String, Map<ServiceJourneyId, List<LocalDateTime>>> activeDatesByServiceJourney =
    new HashMap<>();
  private final Map<String, List<ServiceJourneyInterchangeInfo>> interchangeInfos =
    new HashMap<>();
  private final Map<String, Map<ServiceJourneyId, List<ServiceJourneyStop>>> serviceJourneyStops =
    new HashMap<>();

  @Override
  public List<SimpleLine> lineNames(String validationReportId) {
    return List.of();
  }

  @Override
  public Map<ServiceJourneyId, List<ServiceJourneyStop>> serviceJourneyStops(
    String validationReportId
  ) {
    return serviceJourneyStops.getOrDefault(validationReportId, Map.of());
  }

  @Override
  public List<ServiceJourneyInterchangeInfo> serviceJourneyInterchangeInfos(
    String validationReportId
  ) {
    return interchangeInfos.getOrDefault(validationReportId, List.of());
  }

  @Override
  public Map<ServiceJourneyId, List<DayTypeId>> serviceJourneyDayTypes(
    String validationReportId
  ) {
    return Map.of();
  }

  @Override
  public Map<ActiveDatesId, ActiveDates> activeDates(
    String validationReportId
  ) {
    return Map.of();
  }

  @Override
  public Map<ServiceJourneyId, List<OperatingDayId>> serviceJourneyOperatingDays(
    String validationReportId
  ) {
    return Map.of();
  }

  @Override
  public Map<ServiceJourneyId, List<LocalDateTime>> serviceJourneyIdToActiveDates(
    String validationReportId
  ) {
    return activeDatesByServiceJourney.getOrDefault(
      validationReportId,
      Map.of()
    );
  }

  @Override
  public Set<String> scheduledStopPointIds(String validationReportId) {
    return Set.of();
  }

  public TestNetexDataRepository addServiceJourneyInterchangeInfo(
    String validationReportId,
    ServiceJourneyInterchangeInfo serviceJourneyInterchangeInfo
  ) {
    interchangeInfos
      .computeIfAbsent(validationReportId, reportId -> new ArrayList<>())
      .add(serviceJourneyInterchangeInfo);
    return this;
  }

  public TestNetexDataRepository putServiceJourneyIdToActiveDates(
    String validationReportId,
    Map<ServiceJourneyId, List<LocalDateTime>> serviceJourneyIdToActiveDates
  ) {
    activeDatesByServiceJourney.put(
      validationReportId,
      serviceJourneyIdToActiveDates
    );
    return this;
  }

  public TestNetexDataRepository putServiceJourneyStop(
    String validationReportId,
    Map<ServiceJourneyId, List<ServiceJourneyStop>> serviceJourneyStops
  ) {
    this.serviceJourneyStops.put(validationReportId, serviceJourneyStops);
    return this;
  }
}
