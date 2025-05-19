package no.entur.antu.config;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import no.entur.antu.netexdata.NetexDataRepositoryLoader;
import no.entur.antu.organisation.SimpleOrganisationAliasRepository;
import no.entur.antu.services.AntuBlobStoreService;
import no.entur.antu.services.AntuExchangeBlobStoreService;
import no.entur.antu.stop.StopPlaceRepositoryLoader;
import no.entur.antu.validation.validator.organisation.OrganisationAliasRepository;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.jaxb.*;
import org.entur.netex.validation.validator.model.*;
import org.rutebanken.helper.storage.repository.BlobStoreRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestConfig {

  @Bean
  @Primary
  public CommonDataRepository commonDataRepository() {
    return new TestCommonDataRepository();
  }

  @Bean
  @Primary
  public TestNetexDataRepository netexDataRepository() {
    return new TestNetexDataRepository();
  }

  @Bean
  @Primary
  public OrganisationAliasRepository organisationAliasRepository() {
    return new SimpleOrganisationAliasRepository(new HashSet<>());
  }

  @Bean(name = "stopPlaceRepository")
  @Primary
  public StopPlaceRepository stopPlaceRepository() {
    return new TestStopPlaceRepository();
  }

  private static class TestCommonDataRepository
    implements CommonDataRepositoryLoader {

    @Override
    public boolean hasSharedScheduledStopPoints(String validationReportId) {
      return true;
    }

    @Override
    public QuayId quayIdForScheduledStopPoint(
      ScheduledStopPointId scheduledStopPointId,
      String validationReportId
    ) {
      return null;
    }

    @Override
    public FromToScheduledStopPointId fromToScheduledStopPointIdForServiceLink(
      ServiceLinkId serviceLinkId,
      String validationReportId
    ) {
      return null;
    }

    @Override
    public String getFlexibleStopPlaceRefByStopPointRef(
      String validationReportId,
      String stopPointRef
    ) {
      return "";
    }

    @Override
    public void collect(
      String validationReportId,
      NetexEntitiesIndex netexEntitiesIndex
    ) {}

    @Override
    public void cleanUp(String validationReportId) {}
  }

  public class TestNetexDataRepository
    implements NetexDataRepositoryLoader {

    private Map<String, Map<ServiceJourneyId, List<LocalDateTime>>> serviceJourneyIdToActiveDatesByValidationReportId;
    private List<ServiceJourneyInterchangeInfo> serviceJourneyInterchangeInfos;
    private Map<String, Map<ServiceJourneyId, List<ServiceJourneyStop>>> serviceJourneyStopsMap;

    @Override
    public List<SimpleLine> lineNames(String validationReportId) {
      return List.of();
    }

    @Override
    public Map<ServiceJourneyId, List<ServiceJourneyStop>> serviceJourneyStops(
      String validationReportId
    ) {
      return this.serviceJourneyStopsMap.get(validationReportId);
    }

    @Override
    public List<ServiceJourneyInterchangeInfo> serviceJourneyInterchangeInfos(
      String validationReportId
    ) {
      return this.serviceJourneyInterchangeInfos;
    }

    public void addServiceJourneyInterchangeInfo(ServiceJourneyInterchangeInfo serviceJourneyInterchangeInfo) {
      if (this.serviceJourneyInterchangeInfos == null) {
        this.serviceJourneyInterchangeInfos = new ArrayList<>();
        this.serviceJourneyInterchangeInfos.add(serviceJourneyInterchangeInfo);
      }
      this.serviceJourneyInterchangeInfos.add(serviceJourneyInterchangeInfo);
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
    public void cleanUp(String validationReportId) {}

    @Override
    public Map<ServiceJourneyId, List<LocalDateTime>> serviceJourneyIdToActiveDates(String validationReportId) {
      return serviceJourneyIdToActiveDatesByValidationReportId.get(validationReportId);
    }

    public void putServiceJourneyIdToActiveDates(String validationReportId, Map<ServiceJourneyId, List<LocalDateTime>> serviceJourneyIdToActiveDates) {
      Map.Entry<String, Map<ServiceJourneyId, List<LocalDateTime>>> entry = Map.entry(validationReportId, serviceJourneyIdToActiveDates);
      if (this.serviceJourneyIdToActiveDatesByValidationReportId == null) {
        this.serviceJourneyIdToActiveDatesByValidationReportId = Map.ofEntries(entry);
        return;
      }
      this.serviceJourneyIdToActiveDatesByValidationReportId.put(validationReportId, entry.getValue());
    }

    public void putServiceJourneyStop(String validationReportId, Map<ServiceJourneyId, List<ServiceJourneyStop>> serviceJourneyStops) {
      Map.Entry<String, Map<ServiceJourneyId, List<ServiceJourneyStop>>> entry = Map.entry(validationReportId, serviceJourneyStops);
      if (this.serviceJourneyStopsMap == null) {
        this.serviceJourneyStopsMap = Map.ofEntries(entry);
        return;
      }
      this.serviceJourneyStopsMap.put(validationReportId, entry.getValue());
    }
  }

  private static class TestStopPlaceRepository
    implements StopPlaceRepositoryLoader {

    @Override
    public boolean hasStopPlaceId(StopPlaceId stopPlaceId) {
      return false;
    }

    @Override
    public boolean hasQuayId(QuayId quayId) {
      return false;
    }

    @Override
    public TransportModeAndSubMode getTransportModesForQuayId(QuayId quayId) {
      return null;
    }

    @Override
    public QuayCoordinates getCoordinatesForQuayId(QuayId quayId) {
      return new QuayCoordinates(0, 0);
    }

    @Override
    public String getStopPlaceNameForQuayId(QuayId quayId) {
      return null;
    }

    @Override
    public boolean isEmpty() {
      return false;
    }

    @Override
    public void refreshCache() {}
  }
}
