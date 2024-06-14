package no.entur.antu.config;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import no.entur.antu.commondata.CommonDataRepository;
import no.entur.antu.model.LineInfo;
import no.entur.antu.model.QuayCoordinates;
import no.entur.antu.model.QuayId;
import no.entur.antu.model.ScheduledStopPointId;
import no.entur.antu.model.ScheduledStopPointIds;
import no.entur.antu.model.ServiceLinkId;
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
      public QuayId quayIdForScheduledStopPoint(
        ScheduledStopPointId scheduledStopPointId,
        String validationReportId
      ) {
        return null;
      }

      @Override
      public ScheduledStopPointIds scheduledStopPointIdsForServiceLink(
        ServiceLinkId serviceLinkId,
        String validationReportId
      ) {
        return null;
      }

      @Override
      public List<LineInfo> lineNames(String validationReportId) {
        return List.of();
      }

      @Override
      public List<ScheduledStopPointId> scheduledStopPointsForServiceJourney(
        String validationReportId,
        String serviceJourneyId
      ) {
        return List.of();
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
      public QuayCoordinates getCoordinatesForQuayId(QuayId quayId) {
        return new QuayCoordinates(0, 0);
      }

      @Override
      public String getStopPlaceNameForQuayId(QuayId quayId) {
        return null;
      }

      @Override
      public void refreshCache() {}
    };
  }
}
