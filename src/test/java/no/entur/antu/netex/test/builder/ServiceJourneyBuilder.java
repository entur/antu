package no.entur.antu.netex.test.builder;

import static org.entur.netex.validation.test.jaxb.support.JAXBUtils.createJaxbElement;

import java.util.ArrayList;
import java.util.List;
import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;
import org.rutebanken.netex.model.JourneyPatternRefStructure;
import org.rutebanken.netex.model.LineRefStructure;
import org.rutebanken.netex.model.Line_VersionStructure;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.TimetabledPassingTimes_RelStructure;
import org.rutebanken.netex.model.TransportSubmodeStructure;
import org.rutebanken.netex.model.VehicleJourneyRefStructure;
import org.rutebanken.netex.model.VersionOfObjectRefStructure;

public class ServiceJourneyBuilder
  extends EntityBuilder<ServiceJourney>
  implements Referenceable<VersionOfObjectRefStructure> {

  private final GenericLineBuilder<? extends Line_VersionStructure> line;
  private final JourneyPatternBuilder journeyPattern;
  private final List<TimetabledPassingTimeBuilder> timetabledPassingTimes =
    new ArrayList<>();
  private AllVehicleModesOfTransportEnumeration transportMode;
  private TransportSubmodeStructure transportSubmode;

  public ServiceJourneyBuilder(
    int id,
    GenericLineBuilder<? extends Line_VersionStructure> line,
    JourneyPatternBuilder journeyPattern
  ) {
    super("ServiceJourney", id);
    this.line = line;
    this.journeyPattern = journeyPattern;
  }

  @Override
  public VehicleJourneyRefStructure refObject() {
    return NetexRefs.serviceJourneyRef(id);
  }

  /**
   * Adds a passing time at the given stop point in the journey pattern.
   *
   * @param id the id of the timetabled passing time
   * @param stopPointInJourneyPattern the stop point this passing time is at
   * @return TimetabledPassingTimeBuilder
   */
  public TimetabledPassingTimeBuilder addTimetabledPassingTime(
    int id,
    StopPointInJourneyPatternBuilder stopPointInJourneyPattern
  ) {
    TimetabledPassingTimeBuilder timetabledPassingTime =
      new TimetabledPassingTimeBuilder(id, stopPointInJourneyPattern);
    timetabledPassingTimes.add(timetabledPassingTime);
    return timetabledPassingTime;
  }

  public ServiceJourneyBuilder withTransportMode(
    AllVehicleModesOfTransportEnumeration transportMode
  ) {
    this.transportMode = transportMode;
    return this;
  }

  public ServiceJourneyBuilder withTransportSubmode(
    TransportSubmodeStructure transportSubmode
  ) {
    this.transportSubmode = transportSubmode;
    return this;
  }

  @Override
  public ServiceJourney build() {
    ServiceJourney serviceJourney = new ServiceJourney()
      .withId(ref())
      .withLineRef(
        createJaxbElement(new LineRefStructure().withRef(line.ref()))
      )
      .withDayTypes(NetexRefs.everyDayRefs())
      .withJourneyPatternRef(
        createJaxbElement(
          new JourneyPatternRefStructure().withRef(journeyPattern.ref())
        )
      );

    serviceJourney.withPassingTimes(
      new TimetabledPassingTimes_RelStructure()
        .withTimetabledPassingTime(
          timetabledPassingTimes
            .stream()
            .map(TimetabledPassingTimeBuilder::build)
            .toList()
        )
    );

    if (transportMode != null) {
      serviceJourney.withTransportMode(transportMode);
    }

    if (transportSubmode != null) {
      serviceJourney.withTransportSubmode(transportSubmode);
    }

    return serviceJourney;
  }
}
