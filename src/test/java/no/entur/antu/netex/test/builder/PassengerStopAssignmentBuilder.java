package no.entur.antu.netex.test.builder;

import static org.entur.netex.validation.test.jaxb.support.JAXBUtils.createJaxbElement;

import org.rutebanken.netex.model.PassengerStopAssignment;
import org.rutebanken.netex.model.QuayRefStructure;
import org.rutebanken.netex.model.ScheduledStopPointRefStructure;
import org.rutebanken.netex.model.StopPlaceRefStructure;

public class PassengerStopAssignmentBuilder
  extends EntityBuilder<PassengerStopAssignment> {

  private ScheduledStopPointRefStructure scheduledStopPointRef;
  private StopPlaceRefStructure stopPlaceRef;
  private QuayRefStructure quayRef;

  public PassengerStopAssignmentBuilder(int id) {
    super("PassengerStopAssignment", id);
  }

  public PassengerStopAssignmentBuilder withScheduledStopPointRef(
    ScheduledStopPointRefStructure scheduledStopPointRef
  ) {
    this.scheduledStopPointRef = scheduledStopPointRef;
    return this;
  }

  public PassengerStopAssignmentBuilder withStopPlaceRef(
    StopPlaceRefStructure stopPlaceRef
  ) {
    this.stopPlaceRef = stopPlaceRef;
    return this;
  }

  public PassengerStopAssignmentBuilder withQuayRef(QuayRefStructure quayRef) {
    this.quayRef = quayRef;
    return this;
  }

  @Override
  public PassengerStopAssignment build() {
    return new PassengerStopAssignment()
      .withId(ref())
      .withScheduledStopPointRef(createJaxbElement(scheduledStopPointRef))
      .withQuayRef(createJaxbElement(quayRef))
      .withStopPlaceRef(createJaxbElement(stopPlaceRef));
  }
}
