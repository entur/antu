package no.entur.antu.netex.test.builder;

import java.time.Duration;
import org.rutebanken.netex.model.ScheduledStopPointRefStructure;
import org.rutebanken.netex.model.ServiceJourneyInterchange;
import org.rutebanken.netex.model.VehicleJourneyRefStructure;

public class ServiceJourneyInterchangeBuilder
  extends EntityBuilder<ServiceJourneyInterchange> {

  private boolean guaranteed = true;
  private Duration maximumWaitTime;
  private ScheduledStopPointRefStructure fromPointRef;
  private ScheduledStopPointRefStructure toPointRef;
  private VehicleJourneyRefStructure fromJourneyRef;
  private VehicleJourneyRefStructure toJourneyRef;

  public ServiceJourneyInterchangeBuilder(int id) {
    super("ServiceJourneyInterchange", id);
  }

  public ServiceJourneyInterchangeBuilder withGuaranteed(boolean guaranteed) {
    this.guaranteed = guaranteed;
    return this;
  }

  public ServiceJourneyInterchangeBuilder withMaximumWaitTime(
    Duration maximumWaitTime
  ) {
    this.maximumWaitTime = maximumWaitTime;
    return this;
  }

  public ServiceJourneyInterchangeBuilder withFromPointRef(
    ScheduledStopPointRefStructure fromPointRef
  ) {
    this.fromPointRef = fromPointRef;
    return this;
  }

  public ServiceJourneyInterchangeBuilder withToPointRef(
    ScheduledStopPointRefStructure toPointRef
  ) {
    this.toPointRef = toPointRef;
    return this;
  }

  public ServiceJourneyInterchangeBuilder withFromJourneyRef(
    VehicleJourneyRefStructure fromJourneyRef
  ) {
    this.fromJourneyRef = fromJourneyRef;
    return this;
  }

  public ServiceJourneyInterchangeBuilder withToJourneyRef(
    VehicleJourneyRefStructure toJourneyRef
  ) {
    this.toJourneyRef = toJourneyRef;
    return this;
  }

  @Override
  public ServiceJourneyInterchange build() {
    ServiceJourneyInterchange serviceJourneyInterchange =
      new ServiceJourneyInterchange()
        .withId(ref())
        .withGuaranteed(guaranteed)
        .withMaximumWaitTime(maximumWaitTime);

    if (fromPointRef != null) {
      serviceJourneyInterchange.withFromPointRef(fromPointRef);
    }

    if (toPointRef != null) {
      serviceJourneyInterchange.withToPointRef(toPointRef);
    }

    if (fromJourneyRef != null) {
      serviceJourneyInterchange.withFromJourneyRef(fromJourneyRef);
    }

    if (toJourneyRef != null) {
      serviceJourneyInterchange.withToJourneyRef(toJourneyRef);
    }

    return serviceJourneyInterchange;
  }
}
