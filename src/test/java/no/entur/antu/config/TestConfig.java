package no.entur.antu.config;

import java.util.Collections;
import java.util.Set;
import no.entur.antu.commondata.CommonDataRepository;
import no.entur.antu.model.QuayId;
import no.entur.antu.model.StopPlaceId;
import no.entur.antu.model.TransportModes;
import no.entur.antu.organisation.OrganisationRepository;
import no.entur.antu.stop.StopPlaceRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestConfig {

  @Bean
  @Primary
  public CommonDataRepository commonDataRepository() {
    return new CommonDataRepository() {
      @Override
      public boolean hasQuayIds(String validationReportId) {
        return true;
      }

      @Override
      public QuayId findQuayIdForScheduledStopPoint(
        String scheduledStopPoint,
        String validationReportId
      ) {
        return null;
      }

      @Override
      public void loadCommonDataCache(
        byte[] fileContent,
        String validationReportId
      ) {}

      @Override
      public void cleanUp(String validationReportId) {}
    };
  }

  @Bean
  @Primary
  public OrganisationRepository organisationRepository() {
    return new OrganisationRepository() {
      @Override
      public void refreshCache() {}

      @Override
      public Set<String> getWhitelistedAuthorityIds(String codespace) {
        if ("avi".equals(codespace)) {
          return Set.of("AVI:Authority:Avinor");
        }
        if ("flb".equals(codespace)) {
          return Set.of("FLB:Authority:XXX", "FLB:Authority:YYY");
        }
        return Collections.emptySet();
      }
    };
  }

  @Bean(name = "stopPlaceRepository")
  @Primary
  public StopPlaceRepository stopPlaceRepository() {
    return new StopPlaceRepository() {
      @Override
      public boolean hasStopPlaceId(StopPlaceId stopPlaceId) {
        return false;
      }

      @Override
      public boolean hasQuayId(QuayId quayId) {
        return false;
      }

      @Override
      public TransportModes getTransportModesForQuayId(QuayId quayId) {
        return null;
      }

      @Override
      public void refreshCache() {}
    };
  }
}
