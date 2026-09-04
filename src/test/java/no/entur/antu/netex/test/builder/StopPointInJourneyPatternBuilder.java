package no.entur.antu.netex.test.builder;

import static org.entur.netex.validation.test.jaxb.support.JAXBUtils.createJaxbElement;

import java.math.BigInteger;
import org.rutebanken.netex.model.DestinationDisplayRefStructure;
import org.rutebanken.netex.model.ScheduledStopPointRefStructure;
import org.rutebanken.netex.model.StopPointInJourneyPattern;

public class StopPointInJourneyPatternBuilder
  extends EntityBuilder<StopPointInJourneyPattern> {

  private int order = 1;
  private ScheduledStopPointRefStructure scheduledStopPointRef;
  private DestinationDisplayRefStructure destinationDisplayRef;
  private boolean forAlighting = false;
  private boolean forBoarding = false;

  public StopPointInJourneyPatternBuilder(int id) {
    super("StopPointInJourneyPattern", id);
  }

  public StopPointInJourneyPatternBuilder withOrder(int order) {
    this.order = order;
    return this;
  }

  public StopPointInJourneyPatternBuilder withScheduledStopPointRef(
    ScheduledStopPointRefStructure scheduledStopPointRef
  ) {
    this.scheduledStopPointRef = scheduledStopPointRef;
    return this;
  }

  public StopPointInJourneyPatternBuilder withDestinationDisplayId(
    DestinationDisplayRefStructure destinationDisplayRef
  ) {
    this.destinationDisplayRef = destinationDisplayRef;
    return this;
  }

  public StopPointInJourneyPatternBuilder withForAlighting(
    boolean forAlighting
  ) {
    this.forAlighting = forAlighting;
    return this;
  }

  public StopPointInJourneyPatternBuilder withForBoarding(boolean forBoarding) {
    this.forBoarding = forBoarding;
    return this;
  }

  @Override
  public StopPointInJourneyPattern build() {
    StopPointInJourneyPattern stopPointInJourneyPattern =
      new StopPointInJourneyPattern()
        .withId(ref())
        .withOrder(BigInteger.valueOf(order));

    if (scheduledStopPointRef != null) {
      stopPointInJourneyPattern.withScheduledStopPointRef(
        createJaxbElement(scheduledStopPointRef)
      );
    }

    if (destinationDisplayRef != null) {
      stopPointInJourneyPattern.setDestinationDisplayRef(
        createJaxbElement(destinationDisplayRef).getValue()
      );
    }

    stopPointInJourneyPattern.withForAlighting(forAlighting);
    stopPointInJourneyPattern.withForBoarding(forBoarding);

    return stopPointInJourneyPattern;
  }
}
