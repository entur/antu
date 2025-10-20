package no.entur.antu.config;

import java.time.Instant;
import java.util.Set;
import no.entur.antu.common.repository.TestNetexDataRepository;
import no.entur.antu.stop.StopPlaceRepositoryLoader;
import no.entur.antu.validation.validator.organisation.OrganisationAliasRepository;
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
  public OrganisationAliasRepository organisationAliasRepository() {
    return new TestOrganisationAliasRepository();
  }

  @Bean
  @Primary
  public StopPlaceRepositoryLoader stopPlaceRepository() {
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
      return true;
    }

    @Override
    public boolean hasQuayId(QuayId quayId) {
      return true;
    }

    @Override
    public boolean isParentStop(StopPlaceId stopPlaceId) {
      return false;
    }

    @Override
    public Set<String> getQuaysForStopPlaceId(StopPlaceId stopPlaceId) {
      return Set.of();
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
    public Instant refreshCache() {
      return null;
    }

    @Override
    public void createOrUpdateQuay(QuayId quayId, SimpleQuay quay) {}

    @Override
    public void createOrUpdateStopPlace(
      StopPlaceId id,
      SimpleStopPlace stopPlace
    ) {}

    @Override
    public void deleteStopPlace(StopPlaceId stopPlaceId) {}

    @Override
    public void deleteQuay(QuayId quayId) {}
  }

  private static class TestOrganisationAliasRepository
    implements OrganisationAliasRepository {

    @Override
    public boolean hasOrganisationWithAlias(String organisationId) {
      return true;
    }

    @Override
    public void refreshCache() {}

    @Override
    public boolean isEmpty() {
      return false;
    }
  }
}
