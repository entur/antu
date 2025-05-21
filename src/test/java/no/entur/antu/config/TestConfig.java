package no.entur.antu.config;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import no.entur.antu.common.repository.TestNetexDataRepository;
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
  public OrganisationAliasRepository organisationAliasRepository() {
    return new SimpleOrganisationAliasRepository(new HashSet<>());
  }

  @Bean
  @Primary
  public TestNetexDataRepository netexDataRepository() {
    return new TestNetexDataRepository();
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
