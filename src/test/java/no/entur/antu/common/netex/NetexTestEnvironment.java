package no.entur.antu.common.netex;

import java.util.HashMap;
import java.util.Map;
import no.entur.antu.common.repository.TestCommonDataRepository;
import no.entur.antu.common.repository.TestStopPlaceRepository;
import org.entur.netex.index.impl.NetexEntitiesIndexImpl;
import org.entur.netex.validation.validator.jaxb.JAXBValidationContext;
import org.entur.netex.validation.validator.jaxb.StopPlaceRepository;
import org.rutebanken.netex.model.FlexibleStopPlace;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.ServiceJourney;

public class NetexTestEnvironment {

  private JAXBValidationContext jaxbValidationContext;
  private StopPlaceRepository testStopPlaceRepository;
  private TestCommonDataRepository testCommonDataRepository;

  public NetexTestEnvironment() {
    this.testCommonDataRepository =
      new TestCommonDataRepository(Map.of(), new HashMap<>());
    this.testStopPlaceRepository = TestStopPlaceRepository.ofLocalBusStops(1);
    this.jaxbValidationContext =
      new JAXBValidationContext(
        "VALIDATION_REPORT_KEY",
        new NetexEntitiesIndexImpl(),
        testCommonDataRepository,
        v -> testStopPlaceRepository,
        "ENT",
        "_common.xml",
        Map.of()
      );
  }

  public void addServiceJourney(ServiceJourney serviceJourney) {
    this.jaxbValidationContext.getNetexEntitiesIndex()
      .getServiceJourneyIndex()
      .put(serviceJourney.getId(), serviceJourney);
  }

  public void addJourneyPattern(JourneyPattern journeyPattern) {
    this.jaxbValidationContext.getNetexEntitiesIndex()
      .getJourneyPatternIndex()
      .put(journeyPattern.getId(), journeyPattern);
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

  private JAXBValidationContext recreateJaxbValidationContext(
    HashMap<String, String> flexibleStopPlaceRefByStopPointRef
  ) {
    return new JAXBValidationContext(
      this.jaxbValidationContext.getValidationReportId(),
      this.jaxbValidationContext.getNetexEntitiesIndex(),
      new TestCommonDataRepository(
        Map.ofEntries(),
        flexibleStopPlaceRefByStopPointRef
      ),
      v -> testStopPlaceRepository,
      "ENT",
      "_common.xml",
      Map.of()
    );
  }

  public void addFlexibleStopPlaceToCommonDataRepository(
    String scheduledStopPointId,
    FlexibleStopPlace flexibleStopPlace
  ) {
    HashMap<String, String> currentFlexibleStopPlaceRefByStopPointRef =
      this.testCommonDataRepository.getFlexibleStopPlaceRefByStopPointRef();
    currentFlexibleStopPlaceRefByStopPointRef.put(
      scheduledStopPointId,
      flexibleStopPlace.getId()
    );
    this.jaxbValidationContext =
      recreateJaxbValidationContext(currentFlexibleStopPlaceRefByStopPointRef);
  }

  private NetexTestEnvironment recreateNetexTestEnvironment() {
    NetexTestEnvironment netexTestEnvironment = new NetexTestEnvironment();
    netexTestEnvironment.testCommonDataRepository
      .getFlexibleStopPlaceRefByStopPointRef()
      .putAll(
        this.testCommonDataRepository.getFlexibleStopPlaceRefByStopPointRef()
      );
    netexTestEnvironment.jaxbValidationContext =
      recreateJaxbValidationContext(
        this.testCommonDataRepository.getFlexibleStopPlaceRefByStopPointRef()
      );
    netexTestEnvironment.testStopPlaceRepository = this.testStopPlaceRepository;
    return netexTestEnvironment;
  }

  public NetexTestEnvironmentBuilder toBuilder() {
    NetexTestEnvironment netexTestEnvironment = recreateNetexTestEnvironment();
    return new NetexTestEnvironmentBuilder(netexTestEnvironment);
  }

  public JAXBValidationContext getJaxbValidationContext() {
    return jaxbValidationContext;
  }
}
