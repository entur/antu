package no.entur.antu.config;

import java.util.List;
import java.util.Map;
import java.util.Set;
import no.entur.antu.netexdata.NetexDataRepositoryLoader;
import no.entur.antu.organisation.OrganisationRepository;
import no.entur.antu.organisation.SimpleOrganisationRepository;
import no.entur.antu.stop.StopPlaceRepositoryLoader;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.jaxb.*;
import org.entur.netex.validation.validator.model.*;
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
  public NetexDataRepository netexDataRepository() {
    return new TestNetexDataRepository();
  }

  @Bean
  @Primary
  public OrganisationRepository organisationRepository() {
    return new SimpleOrganisationRepository(
      Map.of(
        "avi",
        Set.of("AVI:Authority:Avinor"),
        "flb",
        Set.of("FLB:Authority:XXX", "FLB:Authority:YYY")
      )
    );
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
    public void collect(
      String validationReportId,
      NetexEntitiesIndex netexEntitiesIndex
    ) {}

    @Override
    public void cleanUp(String validationReportId) {}
  }

  private static class TestNetexDataRepository
    implements NetexDataRepositoryLoader {

    @Override
    public List<SimpleLine> lineNames(String validationReportId) {
      return List.of();
    }

    @Override
    public Map<ServiceJourneyId, List<ServiceJourneyStop>> serviceJourneyStops(
      String validationReportId
    ) {
      return Map.of();
    }

    @Override
    public List<ServiceJourneyInterchangeInfo> serviceJourneyInterchangeInfos(
      String validationReportId
    ) {
      return List.of();
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
