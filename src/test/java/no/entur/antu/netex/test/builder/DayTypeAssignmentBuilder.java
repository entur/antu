package no.entur.antu.netex.test.builder;

import static org.entur.netex.validation.test.jaxb.support.JAXBUtils.createJaxbElement;

import java.time.LocalDate;
import java.util.Optional;
import org.rutebanken.netex.model.DayTypeAssignment;
import org.rutebanken.netex.model.OperatingDayRefStructure;
import org.rutebanken.netex.model.OperatingPeriodRefStructure;

/**
 * A day type assignment is assigned to exactly one of a date, an operating day or an operating
 * period, so each with* method clears the other two.
 */
public class DayTypeAssignmentBuilder extends EntityBuilder<DayTypeAssignment> {

  private LocalDate date;
  private OperatingDayBuilder operatingDayRef;
  private String operatingPeriodRef;

  public DayTypeAssignmentBuilder(int id) {
    super("DayTypeAssignment", id);
  }

  public DayTypeAssignmentBuilder withDate(LocalDate date) {
    this.date = date;
    this.operatingDayRef = null;
    this.operatingPeriodRef = null;
    return this;
  }

  public DayTypeAssignmentBuilder withOperatingDayRef(
    OperatingDayBuilder operatingDayRef
  ) {
    this.operatingDayRef = operatingDayRef;
    this.date = null;
    this.operatingPeriodRef = null;
    return this;
  }

  public DayTypeAssignmentBuilder withOperatingPeriodRef(
    String operatingPeriodRef
  ) {
    this.operatingPeriodRef = operatingPeriodRef;
    this.date = null;
    this.operatingDayRef = null;
    return this;
  }

  @Override
  public DayTypeAssignment build() {
    DayTypeAssignment dayTypeAssignment = new DayTypeAssignment().withId(ref());

    Optional
      .ofNullable(date)
      .ifPresent(assignedDate ->
        dayTypeAssignment.withDate(assignedDate.atStartOfDay())
      );

    Optional
      .ofNullable(operatingDayRef)
      .ifPresent(operatingDay ->
        dayTypeAssignment.withOperatingDayRef(
          new OperatingDayRefStructure().withRef(operatingDay.ref())
        )
      );

    Optional
      .ofNullable(operatingPeriodRef)
      .ifPresent(operatingPeriod ->
        dayTypeAssignment.withOperatingPeriodRef(
          createJaxbElement(
            new OperatingPeriodRefStructure().withRef(operatingPeriod)
          )
        )
      );

    return dayTypeAssignment;
  }
}
