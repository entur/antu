package no.entur.antu.config;

import java.time.Instant;
import java.util.Set;
import no.entur.antu.common.repository.TestNetexDataRepository;
import no.entur.antu.netexdata.NetexDataRepositoryLoader;
import no.entur.antu.stop.StopPlaceRepositoryLoader;
import no.entur.antu.validation.NetexCodespace;
import no.entur.antu.validation.validator.organisation.OrganisationAliasRepository;
import no.entur.antu.validation.validator.vehicletype.VehicleRefRepository;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.validation.validator.jaxb.*;
import org.entur.netex.validation.validator.model.*;
import org.rutebanken.helper.organisation.authorization.AuthorizationService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Stubs for the repositories that back the validators, so the tests do not need the real registries.
 *
 * <p>Import this explicitly: TestApp filters test configurations out of component scanning, so a test
 * that forgets the import silently gets the real stop place and organisation repositories instead.
 *
 * <p>The bean methods declare the loader interfaces the validator configuration asks for. Declaring only
 * the read-only supertype makes the match depend on bean instantiation order.
 */
@TestConfiguration
public class TestConfig {

  @Bean
  @Primary
  public CommonDataRepositoryLoader commonDataRepository() {
    return new TestCommonDataRepository();
  }

  @Bean
  @Primary
  public NetexDataRepositoryLoader netexDataRepository() {
    return new TestNetexDataRepository();
  }

  @Bean
  @Primary
  public OrganisationAliasRepository organisationAliasRepository() {
    return new TestOrganisationAliasRepository();
  }

  @Bean
  @Primary
  public VehicleRefRepository vehicleReferenceRepository() {
    return new TestVehicleRefRepository();
  }

  @Bean
  @Primary
  public ValidationParametersConfig validationParametersConfig() {
    ValidationParametersConfig validationParametersConfig =
      new ValidationParametersConfig();
    validationParametersConfig.setAdditionalAllowedCodespaces(
      Set.of(NetexCodespace.rutebanken("nsr"), NetexCodespace.rutebanken("pen"))
    );
    validationParametersConfig.setAdditionalAllowedOrganisations(Set.of());
    return validationParametersConfig;
  }

  @Bean
  @Primary
  public StopPlaceRepositoryLoader stopPlaceRepository() {
    return new TestStopPlaceRepository();
  }

  /**
   * Grants everything to an authenticated caller. In a deployed environment this bean comes from
   * antu.security.authorization-service, which the tests do not set.
   */
  @Bean
  public AuthorizationService<String> testAuthorizationService() {
    return new TestRutebankenAuthorizationService();
  }

  private static class TestRutebankenAuthorizationService
    implements AuthorizationService<String> {

    @Override
    public boolean isRouteDataAdmin() {
      return authenticated();
    }

    @Override
    public boolean isOrganisationAdmin() {
      return authenticated();
    }

    @Override
    public boolean canViewAllOrganisationData() {
      return authenticated();
    }

    @Override
    public boolean canViewRouteData(String providerId) {
      return authenticated();
    }

    @Override
    public boolean canEditRouteData(String providerId) {
      return authenticated();
    }

    @Override
    public boolean canViewBlockData(String providerId) {
      return authenticated();
    }

    @Override
    public boolean canViewRoleAssignments() {
      return authenticated();
    }

    private static boolean authenticated() {
      if (SecurityContextHolder.getContext().getAuthentication() == null) {
        throw new IllegalStateException("No security context");
      }
      return true;
    }
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

  private static class TestVehicleRefRepository
    implements VehicleRefRepository {

    @Override
    public boolean hasVehicleTypeRef(String vehicleTypeRef) {
      return true;
    }

    @Override
    public boolean hasVehicleRef(String vehicleRef) {
      return true;
    }

    @Override
    public void refreshCache() {
      // No-op: no cache to refresh in this test implementation
    }

    @Override
    public boolean isEmpty() {
      return false;
    }
  }
}
