package no.entur.antu.netex.test.builder;

import static org.entur.netex.validation.test.jaxb.support.JAXBUtils.createJaxbElement;

import java.time.LocalTime;
import org.rutebanken.netex.model.StopPointInJourneyPatternRefStructure;
import org.rutebanken.netex.model.TimetabledPassingTime;

public class TimetabledPassingTimeBuilder
  extends EntityBuilder<TimetabledPassingTime> {

  private final StopPointInJourneyPatternBuilder pointInJourneyPattern;
  private LocalTime departureTime;
  private LocalTime arrivalTime;
  private LocalTime earliestDepartureTime;
  private LocalTime latestArrivalTime;

  public TimetabledPassingTimeBuilder(
    int id,
    StopPointInJourneyPatternBuilder pointInJourneyPattern
  ) {
    super("TimetabledPassingTime", id);
    this.pointInJourneyPattern = pointInJourneyPattern;
  }

  public TimetabledPassingTimeBuilder withDepartureTime(
    LocalTime departureTime
  ) {
    this.departureTime = departureTime;
    return this;
  }

  public TimetabledPassingTimeBuilder withArrivalTime(LocalTime arrivalTime) {
    this.arrivalTime = arrivalTime;
    return this;
  }

  public TimetabledPassingTimeBuilder withEarliestDepartureTime(
    LocalTime earliestDepartureTime
  ) {
    this.earliestDepartureTime = earliestDepartureTime;
    return this;
  }

  public TimetabledPassingTimeBuilder withLatestArrivalTime(
    LocalTime latestArrivalTime
  ) {
    this.latestArrivalTime = latestArrivalTime;
    return this;
  }

  @Override
  public TimetabledPassingTime build() {
    return new TimetabledPassingTime()
      .withId(ref())
      .withDepartureTime(departureTime)
      .withArrivalTime(arrivalTime)
      .withEarliestDepartureTime(earliestDepartureTime)
      .withLatestArrivalTime(latestArrivalTime)
      .withPointInJourneyPatternRef(
        createJaxbElement(
          new StopPointInJourneyPatternRefStructure()
            .withRef(pointInJourneyPattern.ref())
        )
      );
  }
}
