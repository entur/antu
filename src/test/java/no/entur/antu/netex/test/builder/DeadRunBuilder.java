package no.entur.antu.netex.test.builder;

import static org.entur.netex.validation.test.jaxb.support.JAXBUtils.createJaxbElement;

import java.util.ArrayList;
import java.util.List;
import org.rutebanken.netex.model.DeadRun;
import org.rutebanken.netex.model.JourneyPatternRefStructure;
import org.rutebanken.netex.model.LineRefStructure;
import org.rutebanken.netex.model.Line_VersionStructure;
import org.rutebanken.netex.model.TimetabledPassingTimes_RelStructure;

public class DeadRunBuilder extends EntityBuilder<DeadRun> {

  private final GenericLineBuilder<? extends Line_VersionStructure> lineRef;
  private final JourneyPatternBuilder journeyPattern;
  private final List<TimetabledPassingTimeBuilder> timetabledPassingTimes =
    new ArrayList<>();

  public DeadRunBuilder(
    int id,
    GenericLineBuilder<? extends Line_VersionStructure> lineRef,
    JourneyPatternBuilder journeyPattern
  ) {
    super("DeadRun", id);
    this.lineRef = lineRef;
    this.journeyPattern = journeyPattern;
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

  @Override
  public DeadRun build() {
    DeadRun deadRun = new DeadRun()
      .withId(ref())
      .withLineRef(
        createJaxbElement(new LineRefStructure().withRef(lineRef.ref()))
      )
      .withDayTypes(NetexRefs.everyDayRefs())
      .withJourneyPatternRef(
        createJaxbElement(
          new JourneyPatternRefStructure().withRef(journeyPattern.ref())
        )
      );

    deadRun.withPassingTimes(
      new TimetabledPassingTimes_RelStructure()
        .withTimetabledPassingTime(
          timetabledPassingTimes
            .stream()
            .map(TimetabledPassingTimeBuilder::build)
            .toList()
        )
    );

    return deadRun;
  }
}
