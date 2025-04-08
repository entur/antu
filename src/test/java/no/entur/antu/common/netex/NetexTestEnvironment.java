package no.entur.antu.common.netex;

import static org.mockito.Mockito.mock;

import java.util.HashMap;
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

  private JAXBValidationContext jaxbValidationContext;
  private StopPlaceRepository stopPlaceRepositoryMock;
  private TestCommonDataRepository commonDataRepositoryMock;

  public NetexTestEnvironment() {
    this.commonDataRepositoryMock =
      new TestCommonDataRepository(Map.of(), new HashMap<>());
    this.stopPlaceRepositoryMock = mock(StopPlaceRepository.class);
    this.jaxbValidationContext =
      new JAXBValidationContext(
        "VALIDATION_REPORT_KEY",
        new NetexEntitiesIndexImpl(),
        commonDataRepositoryMock,
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
        new TestCommonDataRepository(Map.of(), new HashMap<>()),
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

  public void addFlexibleStopPlaceToNetexEntitiesIndex(
    String scheduledStopPointId,
    FlexibleStopPlace flexibleStopPlace
  ) {
    this.jaxbValidationContext.getNetexEntitiesIndex()
      .getFlexibleStopPlaceIndex()
      .put(flexibleStopPlace.getId(), flexibleStopPlace);
    this.jaxbValidationContext.getNetexEntitiesIndex()
      .getFlexibleStopPlaceIdByStopPointRefIndex()
      .put(scheduledStopPointId, flexibleStopPlace.getId());
  }

  public void addFlexibleStopPlaceToCommonDataRepository(
    String scheduledStopPointId,
    FlexibleStopPlace flexibleStopPlace
  ) {
    HashMap<String, String> currentFlexibleStopPlaceRefByStopPointRef =
      this.commonDataRepositoryMock.getFlexibleStopPlaceRefByStopPointRef();
    currentFlexibleStopPlaceRefByStopPointRef.put(
      scheduledStopPointId,
      flexibleStopPlace.getId()
    );
    this.jaxbValidationContext =
      new JAXBValidationContext(
        this.jaxbValidationContext.getValidationReportId(),
        this.jaxbValidationContext.getNetexEntitiesIndex(),
        new TestCommonDataRepository(
          Map.ofEntries(),
          currentFlexibleStopPlaceRefByStopPointRef
        ),
        v -> stopPlaceRepositoryMock,
        "ENT",
        "_common.xml",
        Map.of()
      );
  }

  public NetexTestEnvironmentBuilder toBuilder() {
    return new NetexTestEnvironmentBuilder(this);
  }

  public JAXBValidationContext getJaxbValidationContext() {
    return jaxbValidationContext;
  }
}
