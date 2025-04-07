package no.entur.antu.common.netex;

import static org.mockito.Mockito.mock;

import java.util.Map;
import no.entur.antu.common.repository.TestCommonDataRepository;
import org.entur.netex.index.api.NetexEntitiesIndex;
import org.entur.netex.index.impl.NetexEntitiesIndexImpl;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
import org.entur.netex.validation.validator.jaxb.StopPlaceRepository;
import org.rutebanken.netex.model.FlexibleStopPlace;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.ServiceJourney;

public class NetexTestEnvironment {

  public JAXBValidationContext jaxbValidationContext;
  private StopPlaceRepository stopPlaceRepositoryMock;

  public NetexTestEnvironment() {
    this.stopPlaceRepositoryMock = mock(StopPlaceRepository.class);
    this.jaxbValidationContext =
      new JAXBValidationContext(
        "TEST",
        new NetexEntitiesIndexImpl(),
        new TestCommonDataRepository(Map.of(), Map.of()),
        v -> stopPlaceRepositoryMock,
        "ENT",
        "_common.xml",
        Map.of()
      );
  }

  public void updateTestCommonDataRepository() {
    NetexEntitiesIndex netexEntitiesIndex =
      this.jaxbValidationContext.getNetexEntitiesIndex();
    this.jaxbValidationContext =
      new JAXBValidationContext(
        this.jaxbValidationContext.getValidationReportId(),
        netexEntitiesIndex,
        new TestCommonDataRepository(
          Map.of(),
          netexEntitiesIndex.getFlexibleStopPlaceIdByStopPointRefIndex()
        ),
        v -> stopPlaceRepositoryMock,
        "ENT",
        "_common.xml",
        Map.of()
      );
  }

  public void addServiceJourney(ServiceJourney serviceJourney) {
    this.jaxbValidationContext.getNetexEntitiesIndex()
      .getServiceJourneyIndex()
      .put(serviceJourney.getId(), serviceJourney);
    this.updateTestCommonDataRepository();
  }

  public void addJourneyPattern(JourneyPattern journeyPattern) {
    this.jaxbValidationContext.getNetexEntitiesIndex()
      .getJourneyPatternIndex()
      .put(journeyPattern.getId(), journeyPattern);
    this.updateTestCommonDataRepository();
  }

  public void addFlexibleStopPlace(
    String scheduledStopPointId,
    FlexibleStopPlace flexibleStopPlace
  ) {
    this.jaxbValidationContext.getNetexEntitiesIndex()
      .getFlexibleStopPlaceIndex()
      .put(flexibleStopPlace.getId(), flexibleStopPlace);
    this.jaxbValidationContext.getNetexEntitiesIndex()
      .getFlexibleStopPlaceIdByStopPointRefIndex()
      .put(scheduledStopPointId, flexibleStopPlace.getId());
    this.updateTestCommonDataRepository();
  }

  public NetexTestEnvironmentBuilder toBuilder() {
    return new NetexTestEnvironmentBuilder(this);
  }
}
