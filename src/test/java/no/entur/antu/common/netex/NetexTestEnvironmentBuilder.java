package no.entur.antu.common.netex;

import org.rutebanken.netex.model.FlexibleStopPlace;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.ServiceJourney;

public class NetexTestEnvironmentBuilder {

  private NetexTestEnvironment netexTestEnvironment;

  public NetexTestEnvironmentBuilder(
    NetexTestEnvironment netexTestEnvironment
  ) {
    this.netexTestEnvironment = netexTestEnvironment;
  }

  public NetexTestEnvironmentBuilder addServiceJourney(
    ServiceJourney serviceJourney
  ) {
    this.netexTestEnvironment.addServiceJourney(serviceJourney);
    return this;
  }

  public NetexTestEnvironmentBuilder addJourneyPattern(
    JourneyPattern journeyPattern
  ) {
    this.netexTestEnvironment.addJourneyPattern(journeyPattern);
    return this;
  }

  public NetexTestEnvironmentBuilder addFlexibleStopPlaceToNetexEntitiesIndex(
    String scheduledStopPointId,
    FlexibleStopPlace flexibleStopPlace
  ) {
    this.netexTestEnvironment.addFlexibleStopPlaceToNetexEntitiesIndex(
        scheduledStopPointId,
        flexibleStopPlace
      );
    return this;
  }

  public NetexTestEnvironmentBuilder addFlexibleStopPlaceToCommonDataRepository(
    String scheduledStopPointId,
    FlexibleStopPlace flexibleStopPlace
  ) {
    this.netexTestEnvironment.addFlexibleStopPlaceToCommonDataRepository(
        scheduledStopPointId,
        flexibleStopPlace
      );
    return this;
  }

  public NetexTestEnvironment build() {
    return this.netexTestEnvironment;
  }
}
